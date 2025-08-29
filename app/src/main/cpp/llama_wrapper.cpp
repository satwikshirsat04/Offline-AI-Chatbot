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

LlamaWrapper::LlamaWrapper() : m_initialized(false), m_model(nullptr), m_context(nullptr), m_current_model_type(MODEL_UNKNOWN) {
    LOGI("LlamaWrapper constructor called");
    llama_backend_init();
}

LlamaWrapper::~LlamaWrapper() {
    cleanup();
    llama_backend_free();
}

bool LlamaWrapper::initialize(const std::string& modelPath) {
    LOGI("Initializing LlamaWrapper with model: %s", modelPath.c_str());

    // Clean up any existing model first - IMPORTANT: Complete cleanup
    cleanup();

    m_modelPath = modelPath;
    m_current_model_type = detectModelType(modelPath);

    LOGI("Detected model type: %d", static_cast<int>(m_current_model_type));

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for mobile
    m_model = llama_model_load_from_file(modelPath.c_str(), model_params);
    if (m_model == nullptr) {
        LOGE("Error loading model");
        return false;
    }

    llama_context_params ctx_params = llama_context_default_params();

    // Adjust context parameters based on model type
    switch (m_current_model_type) {
        case MODEL_LFM2:
            ctx_params.n_ctx = 16384;
            ctx_params.n_batch = 64;
            ctx_params.n_threads = 4;
            break;
        case MODEL_PHI4:
            ctx_params.n_ctx = 16384;
            ctx_params.n_batch = 64;
            ctx_params.n_threads = 4;
            break;
        case MODEL_QWEN:
            ctx_params.n_ctx = 16384;
            ctx_params.n_batch = 64;
            ctx_params.n_threads = 4;
            break;
        case MODEL_DEEPSEEK:
            ctx_params.n_ctx = 16384;
            ctx_params.n_batch = 64;
            ctx_params.n_threads = 4;
            break;
        default:
            ctx_params.n_ctx = 16384;
            ctx_params.n_batch = 64;
            ctx_params.n_threads = 4;
            break;
    }

    m_n_ctx = ctx_params.n_ctx;
    m_n_threads = ctx_params.n_threads;

    m_context = llama_init_from_model(m_model, ctx_params);
    if (m_context == nullptr) {
        LOGE("Error creating context");
        llama_model_free(m_model);
        m_model = nullptr;
        return false;
    }

    m_initialized = true;
    LOGI("Model initialized successfully with context size: %d, Model type: %d", m_n_ctx, static_cast<int>(m_current_model_type));

    return true;
}

LlamaWrapper::ModelType LlamaWrapper::detectModelType(const std::string& modelPath) {
    std::string path_lower = modelPath;
    std::transform(path_lower.begin(), path_lower.end(), path_lower.begin(), ::tolower);

    if (path_lower.find("lfm2") != std::string::npos) {
        return MODEL_LFM2;
    } else if (path_lower.find("phi-4") != std::string::npos || path_lower.find("phi4") != std::string::npos) {
        return MODEL_PHI4;
    } else if (path_lower.find("qwen") != std::string::npos) {
        return MODEL_QWEN;
    } else if (path_lower.find("deepseek") != std::string::npos) {
        return MODEL_DEEPSEEK;
    }

    return MODEL_UNKNOWN;
}

std::string LlamaWrapper::getSystemPrompt() {
    switch (m_current_model_type) {
        case MODEL_LFM2:
            return "<|im_start|>system\nYou are Local AI India, a helpful offline AI assistant powered by LFM2. You work completely on the user's device to keep conversations private and secure. Provide helpful, accurate, and informative responses.<|im_end|>\n<|im_start|>user\n{{prompt}}<|im_end|>\n<|im_start|>assistant\n";

        case MODEL_PHI4:
            // Phi-4 chat template: <|system|>content<|end|><|user|>content<|end|><|assistant|>
            return "<|system|>You are Phi-4, a helpful offline AI assistant. You work completely on the user's device to keep conversations private and secure. Provide helpful, accurate, and informative responses.<|end|><|user|>{{prompt}}<|end|><|assistant|>";

        case MODEL_QWEN:
            // Qwen 1.5 uses ChatML format with proper spacing
            return "<|im_start|>system\nYou are Qwen, a helpful AI assistant powered by Qwen 1.5. You are running offline on the user's device to ensure privacy and security.<|im_end|>\n<|im_start|>user\n{{prompt}}<|im_end|>\n<|im_start|>assistant\n";

        case MODEL_DEEPSEEK:
            // DeepSeek-R1 uses a simple format without complex tags
            return "User: {{prompt}}\n\nAssistant: ";

        default:
            return "<|im_start|>system\nYou are Local AI India, a helpful offline AI assistant. You work completely on the user's device to keep conversations private and secure. Provide helpful, accurate, and informative responses.<|im_end|>\n<|im_start|>user\n{{prompt}}<|im_end|>\n<|im_start|>assistant\n";
    }
}

std::vector<std::string> LlamaWrapper::getStopSequences() {
    switch (m_current_model_type) {
        case MODEL_LFM2:
            return {"<|im_end|>", "<|endoftext|>", "</s>"};

        case MODEL_PHI4:
            return {"<|end|>", "<|user|>", "<|system|>", "</s>"};

        case MODEL_QWEN:
            return {"<|im_end|>", "<|endoftext|>", "</s>"};

        case MODEL_DEEPSEEK:
            return {"User:", "\nUser:", "\n\nUser:", "</s>"};

        default:
            return {"<|im_end|>", "<|endoftext|>", "</s>"};
    }
}

std::string LlamaWrapper::generateResponse(const std::string& prompt) {
    if (!m_initialized) {
        LOGE("Model not initialized when generateResponse called");
        return "Error: Model not initialized";
    }

    LOGI("Generating response for model type %d, prompt: %s", static_cast<int>(m_current_model_type), prompt.substr(0, 50).c_str());

    // IMPORTANT: Always reset context for new conversation to prevent template mixing
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = m_n_ctx;
    ctx_params.n_batch = 128;
    ctx_params.n_threads = m_n_threads;

    if (m_context) {
        llama_free(m_context);
        m_context = nullptr;
    }
    
    m_context = llama_init_from_model(m_model, ctx_params);
    if (m_context == nullptr) {
        LOGE("Failed to reset context");
        return "Error: Failed to reset context";
    }

    // Get model-specific system prompt and format it
    std::string system_prompt = getSystemPrompt();
    std::string formatted_prompt;

    size_t placeholder_pos = system_prompt.find("{{prompt}}");
    if (placeholder_pos != std::string::npos) {
        system_prompt.replace(placeholder_pos, 10, prompt);
        formatted_prompt = system_prompt;
    } else {
        formatted_prompt = prompt;
    }

    LOGI("Formatted prompt for model type %d: %s", static_cast<int>(m_current_model_type), formatted_prompt.substr(0, 100).c_str());

    // Handle BOS token properly for different models
    bool add_bos = true;
    if (m_current_model_type == MODEL_PHI4) {
        // Phi-4 doesn't need explicit BOS token
        add_bos = false;
    }

    std::vector<int> tokens_list = tokenize(formatted_prompt, add_bos);
    if (tokens_list.empty()) {
        LOGE("Could not tokenize prompt");
        return "Error: Could not tokenize prompt";
    }

    LOGI("Tokenized prompt length: %zu", tokens_list.size());

    // Adjust max tokens based on model type and context size
    int max_tokens = std::min(1024, m_n_ctx - (int)tokens_list.size() - 100);

    // Model-specific token limits
    switch (m_current_model_type) {
        case MODEL_PHI4:
            max_tokens = std::min(768, max_tokens);
            break;
        case MODEL_DEEPSEEK:
            max_tokens = std::min(800, max_tokens);
            break;
        case MODEL_QWEN:
            max_tokens = std::min(768, max_tokens);
            break;
        case MODEL_LFM2:
            max_tokens = std::min(1024, max_tokens);
            break;
        default:
            max_tokens = std::min(512, max_tokens);
            break;
    }

    std::string result = generateText(tokens_list, max_tokens);

    // Clean up the result with model-specific stop sequences
    std::vector<std::string> stop_sequences = getStopSequences();
    for (const std::string& stop_seq : stop_sequences) {
        size_t pos = result.find(stop_seq);
        if (pos != std::string::npos) {
            result = result.substr(0, pos);
            LOGI("Found stop sequence: %s", stop_seq.c_str());
            break;
        }
    }

    // Model-specific post-processing
    switch (m_current_model_type) {
        case MODEL_DEEPSEEK:
            // Remove any thinking tags that might appear
            {
                size_t think_start = result.find("<think>");
                size_t think_end = result.find("</think>");
                if (think_start != std::string::npos && think_end != std::string::npos && think_end > think_start) {
                    result.erase(think_start, think_end - think_start + 8);
                }
                // Remove any reasoning blocks
                think_start = result.find("<reasoning>");
                think_end = result.find("</reasoning>");
                if (think_start != std::string::npos && think_end != std::string::npos && think_end > think_start) {
                    result.erase(think_start, think_end - think_start + 12);
                }
            }
            break;
        case MODEL_PHI4:
            // Clean up any residual tags
            {
                size_t tag_pos = result.find("<|");
                if (tag_pos != std::string::npos) {
                    result = result.substr(0, tag_pos);
                }
            }
            break;
    }

    // General cleanup
    size_t end = result.find_last_not_of(" \t\n\r");
    if (end != std::string::npos) {
        result = result.substr(0, end + 1);
    }

    // Remove any leading whitespace
    size_t start = result.find_first_not_of(" \t\n\r");
    if (start != std::string::npos) {
        result = result.substr(start);
    }

    LOGI("Final response length: %zu", result.length());
    return result.empty() ? "I apologize, but I couldn't generate a response. Please try again." : result;
}

void LlamaWrapper::cleanup() {
    if (m_initialized) {
        LOGI("Cleaning up LlamaWrapper - Model type was: %d", static_cast<int>(m_current_model_type));
        if (m_context) {
            llama_free(m_context);
            m_context = nullptr;
        }
        if (m_model) {
            llama_model_free(m_model);
            m_model = nullptr;
        }
        m_initialized = false;
        m_current_model_type = MODEL_UNKNOWN; // Reset model type
    }
}

std::vector<int> LlamaWrapper::tokenize(const std::string& text, bool add_bos) {
    if (!m_model) return {};

    auto vocab = llama_model_get_vocab(m_model);
    int n_tokens = text.length() + (add_bos ? 1 : 0);
    std::vector<int> tokens(n_tokens);
    n_tokens = llama_tokenize(vocab, text.c_str(), text.length(), tokens.data(), tokens.size(), add_bos, false);
    if (n_tokens < 0) {
        tokens.resize(text.length() + 100);
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
    if (!m_model || tokens.empty()) return "";

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
    if (prompt_tokens.empty() || !m_context) {
        return "";
    }

    LOGI("Generating text with %zu prompt tokens, max %d new tokens", prompt_tokens.size(), max_tokens);

    std::string result = "";
    int n_len = max_tokens;
    int n_cur = 0;

    llama_batch batch = llama_batch_init(512, 0, 1);

    // Evaluate the prompt
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

    // Generation loop
    while (n_cur <= n_len + prompt_tokens.size()) {
        auto vocab = llama_model_get_vocab(m_model);
        auto n_vocab = llama_vocab_n_tokens(vocab);
        auto * logits = llama_get_logits_ith(m_context, batch.n_tokens - 1);

        std::vector<llama_token_data> candidates;
        candidates.reserve(n_vocab);
        for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
            candidates.emplace_back(llama_token_data{token_id, logits[token_id], 0.0f});
        }
        llama_token_data_array candidates_p = { candidates.data(), candidates.size(), false };

        // Create sampler chain with model-specific parameters
        auto sparams = llama_sampler_chain_default_params();
        auto sampler = llama_sampler_chain_init(sparams);

        // Model-specific sampling parameters
        switch (m_current_model_type) {
            case MODEL_LFM2:
                llama_sampler_chain_add(sampler, llama_sampler_init_top_k(50));
                llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
                break;
            case MODEL_PHI4:
                llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
                llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.95f, 1));
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
                break;
            case MODEL_QWEN:
                llama_sampler_chain_add(sampler, llama_sampler_init_top_k(60));
                llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.8f, 1));
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
                break;
            case MODEL_DEEPSEEK:
                llama_sampler_chain_add(sampler, llama_sampler_init_top_k(50));
                llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.85f, 1));
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.6f));
                break;
            default:
                llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
                llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.95f, 1));
                llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.8f));
                break;
        }

        llama_sampler_chain_add(sampler, llama_sampler_init_dist(1234));

        llama_token id = llama_sampler_sample(sampler, m_context, batch.n_tokens - 1);
        llama_sampler_accept(sampler, id);
        llama_sampler_free(sampler);

        // Check for end of stream
        if (id == llama_vocab_eos(vocab)) {
            LOGI("EOS token encountered, stopping generation");
            break;
        }

        // Append the token to the result
        char piece[256] = {0};
        int32_t n_chars = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, false);
        if (n_chars > 0) {
            std::string token_str(piece, n_chars);
            result += token_str;

            // Check for model-specific stop sequences during generation
            std::vector<std::string> stop_sequences = getStopSequences();
            for (const std::string& stop_seq : stop_sequences) {
                if (result.find(stop_seq) != std::string::npos) {
                    LOGI("Stop sequence detected during generation: %s", stop_seq.c_str());
                    llama_batch_free(batch);
                    return result.substr(0, result.find(stop_seq));
                }
            }
        }

        // Prepare for the next iteration
        batch.n_tokens = 0;
        batch.token[0] = id;
        batch.pos[0] = n_cur;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;

        if (llama_decode(m_context, batch) != 0) {
            LOGE("llama_decode() failed during generation");
            break;
        }
        n_cur++;
    }

    llama_batch_free(batch);

    LOGI("Generated %zu characters", result.length());
    return result;
}