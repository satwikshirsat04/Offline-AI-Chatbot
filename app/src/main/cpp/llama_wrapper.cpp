#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <stdexcept>
#include <thread>
#include "include/llama.h"
#include "llama_wrapper.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "LlamaWrapper", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LlamaWrapper", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "LlamaWrapper", __VA_ARGS__)

LlamaWrapper::LlamaWrapper()
        : m_initialized(false), m_model(nullptr), m_context(nullptr), m_sampler(nullptr),
          m_current_model_type(MODEL_UNKNOWN), m_n_ctx(16384), m_n_threads(4) {  // UPDATED: 16K context, 4 threads
    LOGI("LlamaWrapper constructor called");
}

LlamaWrapper::~LlamaWrapper() {
    LOGI("LlamaWrapper destructor called");
    cleanup();
}

bool LlamaWrapper::initialize(const std::string& modelPath) {
    LOGI("=== Starting model initialization ===");
    m_modelPath = modelPath;
    cleanup();

    try {
        llama_backend_init();
        LOGD("Llama backend initialized");

        // Conservative model parameters
        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0;
        model_params.use_mmap = true;
        model_params.use_mlock = false;
        model_params.vocab_only = false;

        // Load model
        m_model = llama_model_load_from_file(modelPath.c_str(), model_params);
        if (!m_model) {
            LOGE("Failed to load model from file: %s", modelPath.c_str());
            cleanup();
            return false;
        }

        LOGI("Model loaded successfully");
        m_current_model_type = detectModelType(modelPath);

        // UPDATED: 16K context parameters
        llama_context_params ctx_params = llama_context_default_params();

        // Adjust context parameters based on model type (as shown in your screenshot)
        switch (m_current_model_type) {
            case MODEL_LFM2:
                ctx_params.n_ctx = 16384;
                ctx_params.n_batch = 128;
                ctx_params.n_threads = 4;
                break;
            case MODEL_PHI4:
                ctx_params.n_ctx = 16384;
                ctx_params.n_batch = 128;
                ctx_params.n_threads = 4;
                break;
            case MODEL_QWEN:
                ctx_params.n_ctx = 8192;
                ctx_params.n_batch = 64;
                ctx_params.n_threads = 4;
                break;
            case MODEL_DEEPSEEK:
                ctx_params.n_ctx = 16384;
                ctx_params.n_batch = 128;
                ctx_params.n_threads = 4;
                break;
            default:
                ctx_params.n_ctx = 16384;
                ctx_params.n_batch = 128;
                ctx_params.n_threads = 4;
                break;
        }

        ctx_params.n_threads_batch = ctx_params.n_threads;
        ctx_params.no_perf = true;
        ctx_params.embeddings = false;

        // Create context
        m_context = llama_init_from_model(m_model, ctx_params);
        if (!m_context) {
            LOGE("Failed to create llama context");
            cleanup();
            return false;
        }

        LOGI("Context created successfully with 16K context");

        // Initialize sampler chain
        auto sparams = llama_sampler_chain_default_params();
        m_sampler = llama_sampler_chain_init(sparams);
        llama_sampler_chain_add(m_sampler, llama_sampler_init_greedy());
        LOGI("Sampler initialized successfully");

        // Verify model
        const struct llama_vocab* vocab = llama_model_get_vocab(m_model);
        if (!vocab) {
            LOGE("Model vocabulary check failed");
            cleanup();
            return false;
        }

        int32_t vocab_size = llama_vocab_n_tokens(vocab);
        if (vocab_size <= 0) {
            LOGE("Invalid vocabulary size: %d", vocab_size);
            cleanup();
            return false;
        }

        LOGI("Vocabulary size: %d tokens", vocab_size);
        m_initialized = true;
        LOGI("=== Model initialization completed successfully ===");
        return true;

    } catch (const std::exception& e) {
        LOGE("Exception during initialization: %s", e.what());
        cleanup();
        return false;
    }
}

std::string LlamaWrapper::generateResponse(const std::string& prompt) {
    if (!m_initialized || !m_model || !m_context || !m_sampler) {
        LOGE("Model not properly initialized");
        return "Error: Model not initialized";
    }

    try {
        LOGI("Generating response for prompt: %.50s...", prompt.c_str());

        // Allow longer prompts with 16K context
        std::string limited_prompt = prompt;
        const size_t MAX_PROMPT_LENGTH = 1000;  // INCREASED for 16K context
        if (limited_prompt.length() > MAX_PROMPT_LENGTH) {
            limited_prompt = limited_prompt.substr(0, MAX_PROMPT_LENGTH);
            LOGD("Prompt truncated to %zu characters", MAX_PROMPT_LENGTH);
        }

        std::vector<llama_token> prompt_tokens = tokenize(limited_prompt, true);
        if (prompt_tokens.empty()) {
            LOGE("Failed to tokenize prompt");
            return "Error: Failed to process prompt";
        }

        if (prompt_tokens.size() > 512) {  // INCREASED from 64
            prompt_tokens.resize(512);
            LOGD("Token count limited to 512 tokens");
        }

        LOGD("Tokenized prompt into %zu tokens", prompt_tokens.size());
        std::string response = generateText(prompt_tokens, 256);  // INCREASED from 100
        LOGI("Generated response length: %zu characters", response.length());
        return response;

    } catch (const std::exception& e) {
        LOGE("Exception during response generation: %s", e.what());
        return "Error: Exception during response generation";
    }
}

void LlamaWrapper::cleanup() {
    LOGI("Starting resource cleanup...");
    try {
        if (m_sampler) {
            llama_sampler_free(m_sampler);
            m_sampler = nullptr;
            LOGD("Sampler freed successfully");
        }

        if (m_context) {
            llama_memory_t mem = llama_get_memory(m_context);
            if (mem) {
                llama_memory_clear(mem, true);
            }
            llama_free(m_context);
            m_context = nullptr;
            LOGD("Context freed successfully");
        }

        if (m_model) {
            llama_model_free(m_model);
            m_model = nullptr;
            LOGD("Model freed successfully");
        }

        m_initialized = false;
        LOGI("Resource cleanup completed successfully");

    } catch (const std::exception& e) {
        LOGE("Exception during cleanup: %s", e.what());
        m_context = nullptr;
        m_model = nullptr;
        m_sampler = nullptr;
        m_initialized = false;
    }
}

// Rest of your existing methods remain the same...
LlamaWrapper::ModelType LlamaWrapper::detectModelType(const std::string& modelPath) {
    if (modelPath.find("LFM2") != std::string::npos || modelPath.find("lfm2") != std::string::npos) {
        return MODEL_LFM2;
    } else if (modelPath.find("Phi-4") != std::string::npos || modelPath.find("phi4") != std::string::npos) {
        return MODEL_PHI4;
    } else if (modelPath.find("qwen") != std::string::npos || modelPath.find("Qwen") != std::string::npos) {
        return MODEL_QWEN;
    } else if (modelPath.find("DeepSeek") != std::string::npos || modelPath.find("deepseek") != std::string::npos) {
        return MODEL_DEEPSEEK;
    }
    return MODEL_UNKNOWN;
}

std::string LlamaWrapper::getSystemPrompt() {
    switch (m_current_model_type) {
        case MODEL_PHI4:
            return "<|system|>\nYou are a helpful AI assistant.<|end|>\n<|user|>\n";
        case MODEL_QWEN:
            return "<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n<|im_start|>user\n";
        default:
            return "";
    }
}

std::vector<std::string> LlamaWrapper::getStopSequences() {
    switch (m_current_model_type) {
        case MODEL_PHI4:
            return {"<|end|>", "<|user|>", "<|assistant|>"};
        case MODEL_QWEN:
            return {"<|im_end|>", "<|im_start|>"};
        default:
            return {};
    }
}

std::vector<llama_token> LlamaWrapper::tokenize(const std::string& text, bool add_bos) {
    if (!m_model) return {};
    const struct llama_vocab* vocab = llama_model_get_vocab(m_model);
    if (!vocab) return {};

    std::vector<llama_token> tokens(text.length() + (add_bos ? 1 : 0));
    int n_tokens = llama_tokenize(
            vocab,
            text.c_str(),
            text.length(),
            tokens.data(),
            tokens.size(),
            add_bos,
            false
    );

    if (n_tokens < 0) {
        LOGE("Failed to tokenize text");
        return {};
    }

    tokens.resize(n_tokens);
    return tokens;
}

std::string LlamaWrapper::detokenize(const std::vector<llama_token>& tokens) {
    if (!m_model || tokens.empty()) return "";
    const struct llama_vocab* vocab = llama_model_get_vocab(m_model);
    if (!vocab) return "";

    std::string result;
    result.reserve(tokens.size() * 4);

    for (llama_token token : tokens) {
        char token_str[256] = {0};
        int token_len = llama_token_to_piece(
                vocab, token, token_str, sizeof(token_str), 0, false
        );
        if (token_len > 0 && token_len < sizeof(token_str)) {
            result.append(token_str, token_len);
        }
    }
    return result;
}

std::string LlamaWrapper::generateText(const std::vector<llama_token>& prompt_tokens, int max_tokens) {
    if (!m_context || prompt_tokens.empty()) return "";

    // Clear memory
    llama_memory_t mem = llama_get_memory(m_context);
    if (mem) {
        llama_memory_clear(mem, true);
    }

    // Process prompt
    llama_batch batch = llama_batch_get_one(
            const_cast<llama_token*>(prompt_tokens.data()),
            prompt_tokens.size()
    );

    if (llama_decode(m_context, batch)) {
        LOGE("Failed to decode prompt batch");
        return "Error: Failed to process prompt";
    }

    std::vector<llama_token> response_tokens;
    const struct llama_vocab* vocab = llama_model_get_vocab(m_model);

    // Generate tokens
    for (int i = 0; i < max_tokens; ++i) {
        llama_token next_token = llama_sampler_sample(m_sampler, m_context, -1);

        // Check for end of sequence
        if (next_token == llama_vocab_eos(vocab)) {
            break;
        }

        response_tokens.push_back(next_token);
        llama_sampler_accept(m_sampler, next_token);

        // Process the new token
        llama_batch single_batch = llama_batch_get_one(&next_token, 1);
        if (llama_decode(m_context, single_batch)) {
            LOGE("Failed to decode token at position %d", i);
            break;
        }
    }

    return detokenize(response_tokens);
}
