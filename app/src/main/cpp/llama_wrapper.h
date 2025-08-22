#ifndef LLAMA_WRAPPER_H
#define LLAMA_WRAPPER_H

#include <string>
#include <vector>
#include <memory>

// Forward declarations for llama.cpp structures
struct llama_model;
struct llama_context;
struct llama_context_params;
struct llama_model_params;

class LlamaWrapper {
public:
    LlamaWrapper();
    ~LlamaWrapper();

    bool initialize(const std::string& modelPath);
    std::string generateResponse(const std::string& prompt);
    void cleanup();
    bool isInitialized() const { return m_initialized; }

private:
    bool m_initialized;
    std::string m_modelPath;

    // llama.cpp structures
    llama_model* m_model;
    llama_context* m_context;

    // Model parameters
    int m_n_ctx;
    int m_n_threads;

    // Helper methods
    std::vector<int> tokenize(const std::string& text, bool add_bos = true);
    std::string detokenize(const std::vector<int>& tokens);
    std::string generateText(const std::vector<int>& prompt_tokens, int max_tokens = 256);
};

#endif // LLAMA_WRAPPER_H
