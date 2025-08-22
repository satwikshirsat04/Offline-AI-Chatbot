#include "llama_wrapper.h"
#include "include/llama.h"
#include <android/log.h>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <vector>
#include <cstdlib>
#include <cmath>

#define LOG_TAG "LlamaWrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

LlamaWrapper::LlamaWrapper() : m_initialized(false), m_model(nullptr), m_context(nullptr) {
    LOGI("LlamaWrapper constructor called");
    llama_backend_init();
}

LlamaWrapper::~LlamaWrapper() {
    cleanup();
    llama_backend_free();
}

bool LlamaWrapper::initialize(const std::string& modelPath) {
    LOGI("Initializing LlamaWrapper with model: %s", modelPath.c_str());

    m_modelPath = modelPath;

    llama_model_params model_params = llama_model_default_params();
    m_model = llama_model_load_from_file(modelPath.c_str(), model_params);
    if (m_model == nullptr) {
        LOGE("Error loading model");
        return false;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 32768;  // Increased context size
    ctx_params.n_batch = 128; // Set batch size
    m_n_ctx = ctx_params.n_ctx;
    m_n_threads = ctx_params.n_threads;

    m_context = llama_init_from_model(m_model, ctx_params);
    if (m_context == nullptr) {
        LOGE("Error creating context");
        llama_model_free(m_model);
        return false;
    }

    m_initialized = true;
    LOGI("Model initialized successfully");

    return true;
}

std::string LlamaWrapper::generateResponse(const std::string& prompt) {
    if (!m_initialized) {
        return "Error: Model not initialized";
    }

    LOGI("Generating response for: %s", prompt.c_str());

    // CRITICAL: Reset context for new conversation by recreating it
    // This prevents decode errors on subsequent prompts
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = m_n_ctx;
    ctx_params.n_batch = 128;
    ctx_params.n_threads = m_n_threads;

    llama_free(m_context);
    m_context = llama_init_from_model(m_model, ctx_params);
    if (m_context == nullptr) {
        LOGE("Failed to reset context");
        return "Error: Failed to reset context";
    }

    const char * tmpl = "<|im_start|>system\nYou are Local AI India, a helpful offline AI assistant. You work completely on the user's device to keep conversations private and secure. Provide helpful, accurate, and informative responses.<|im_end|>\n<|im_start|>user\n{{prompt}}<|im_end|>\n<|im_start|>assistant\n";

    std::string formatted_prompt;
    std::string prompt_with_placeholder = tmpl;
    size_t placeholder_pos = prompt_with_placeholder.find("{{prompt}}");
    if (placeholder_pos != std::string::npos) {
        prompt_with_placeholder.replace(placeholder_pos, 10, prompt);
        formatted_prompt = prompt_with_placeholder;
    } else {
        formatted_prompt = prompt;
    }

    std::vector<int> tokens_list = tokenize(formatted_prompt, true);
    if (tokens_list.empty()) {
        return "Error: Could not tokenize prompt";
    }

    LOGI("Tokenized prompt length: %zu", tokens_list.size());

    // Use the existing generateText method but with improved parameters
    int max_tokens = std::min(1024, 2048 - (int)tokens_list.size() - 50);
    std::string result = generateText(tokens_list, max_tokens);

    // Clean up the result
    size_t end = result.find_last_not_of(" \t\n\r");
    if (end != std::string::npos) {
        result = result.substr(0, end + 1);
    }

    // Check for stop sequences and clean them
    if (result.find("<|im_end|>") != std::string::npos) {
        size_t pos = result.find("<|im_end|>");
        result = result.substr(0, pos);
    }

    return result.empty() ? "I apologize, but I couldn't generate a response. Please try again." : result;
}

void LlamaWrapper::cleanup() {
    if (m_initialized) {
        LOGI("Cleaning up LlamaWrapper");
        if (m_context) {
            llama_free(m_context);
            m_context = nullptr;
        }
        if (m_model) {
            llama_model_free(m_model);
            m_model = nullptr;
        }
        m_initialized = false;
    }
}

std::vector<int> LlamaWrapper::tokenize(const std::string& text, bool add_bos) {
    // Use the EXACT same signature as your original working code
    auto vocab = llama_model_get_vocab(m_model);
    int n_tokens = text.length() + (add_bos ? 1 : 0);
    std::vector<int> tokens(n_tokens);
    n_tokens = llama_tokenize(vocab, text.c_str(), text.length(), tokens.data(), tokens.size(), add_bos, false);
    if (n_tokens < 0) {
        tokens.resize(text.length() + 100); // Resize if buffer was too small
        n_tokens = llama_tokenize(vocab, text.c_str(), text.length(), tokens.data(), tokens.size(), add_bos, false);
        if (n_tokens < 0) {
            LOGE("Failed to tokenize text after resize");
            tokens.clear();
            return tokens;
        }
    }
    tokens.resize(n_tokens);
    return tokens;
}

std::string LlamaWrapper::detokenize(const std::vector<int>& tokens) {
    std::string result;
    auto vocab = llama_model_get_vocab(m_model);
    for (int token : tokens) {
        char piece[256] = {0};
        int32_t n_chars = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, false);
        if (n_chars > 0) {
            result += std::string(piece, n_chars);
        }
    }
    return result;
}

std::string LlamaWrapper::generateText(const std::vector<int>& prompt_tokens, int max_tokens) {
    if (prompt_tokens.empty()) {
        return "";
    }

    LOGI("Generating text with %zu prompt tokens, max %d new tokens", prompt_tokens.size(), max_tokens);

    std::string result = "";

    // Main prediction loop - using the EXACT same approach as your original code
    int n_len = max_tokens;
    int n_cur = 0;

    llama_batch batch = llama_batch_init(512, 0, 1);

    // Evaluate the prompt - EXACT same approach as original
    for (size_t i = 0; i < prompt_tokens.size(); i++) {
        batch.token[batch.n_tokens] = prompt_tokens[i];
        batch.pos[batch.n_tokens] = i;
        batch.n_seq_id[batch.n_tokens] = 1;
        batch.seq_id[batch.n_tokens][0] = 0;
        batch.logits[batch.n_tokens] = false;
        batch.n_tokens++;
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(m_context, batch) != 0) {
        LOGE("llama_decode() failed on prompt");
        llama_batch_free(batch);
        return "Error: Failed to decode prompt";
    }
    n_cur = batch.n_tokens;

    // Generation loop - EXACT same approach as original
    while (n_cur <= n_len + prompt_tokens.size()) {
        // Sample the next token - using EXACT same functions as original
        auto vocab = llama_model_get_vocab(m_model);
        auto n_vocab = llama_vocab_n_tokens(vocab);
        auto * logits = llama_get_logits_ith(m_context, batch.n_tokens - 1);

        std::vector<llama_token_data> candidates;
        candidates.reserve(n_vocab);
        for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
            candidates.emplace_back(llama_token_data{token_id, logits[token_id], 0.0f});
        }
        llama_token_data_array candidates_p = { candidates.data(), candidates.size(), false };

        // Create sampler chain with top-k, top-p, and temperature
        auto sparams = llama_sampler_chain_default_params();
        auto sampler = llama_sampler_chain_init(sparams);
        llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.95f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.8f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(1234)); // seed for randomness

        llama_token id = llama_sampler_sample(sampler, m_context, batch.n_tokens - 1);
        llama_sampler_accept(sampler, id);
        llama_sampler_free(sampler);

        // Check for end of stream - using EXACT same function as original
        if (id == llama_vocab_eos(vocab)) {
            LOGI("EOS token encountered, stopping generation");
            break;
        }

        // Append the token to the result - using EXACT same function as original
        char piece[256] = {0}; // Increased buffer size
        int32_t n_chars = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, false);
        if (n_chars > 0) {
            std::string token_str(piece, n_chars);
            result += token_str;

            // Check for stop sequences
            if (result.find("<|im_end|>") != std::string::npos) {
                LOGI("Stop sequence detected, ending generation");
                break;
            }
        }

        // Prepare for the next iteration - EXACT same approach as original
        batch.n_tokens = 0; // Reset batch
        batch.token[0] = id;
        batch.pos[0] = n_cur;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;

        if (llama_decode(m_context, batch) != 0) {
            LOGE("llama_decode() failed during generation");
            break; // Don't return error, just stop generation
        }
        n_cur++;
    }

    llama_batch_free(batch);

    LOGI("Generated %zu characters", result.length());
    return result;
}