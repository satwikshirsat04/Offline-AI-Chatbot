#ifndef LLAMA_WRAPPER_H
#define LLAMA_WRAPPER_H

#include <string>
#include <vector>

// Forward declarations
struct llama_model;
struct llama_context;
struct llama_sampler;
typedef int32_t llama_token;

class LlamaWrapper {
public:
    enum ModelType {
        MODEL_UNKNOWN = 0,
        MODEL_LFM2 = 1,
        MODEL_PHI4 = 2,
        MODEL_QWEN = 3,
        MODEL_DEEPSEEK = 4
    };

    LlamaWrapper();
    ~LlamaWrapper();

    bool initialize(const std::string& modelPath);
    std::string generateResponse(const std::string& prompt);
    void cleanup();
    bool isInitialized() const { return m_initialized; }

private:
    ModelType detectModelType(const std::string& modelPath);
    std::string getSystemPrompt();
    std::vector<std::string> getStopSequences();
    std::vector<llama_token> tokenize(const std::string& text, bool add_bos);
    std::string detokenize(const std::vector<llama_token>& tokens);
    std::string generateText(const std::vector<llama_token>& prompt_tokens, int max_tokens);

    bool m_initialized;
    llama_model* m_model;
    llama_context* m_context;
    llama_sampler* m_sampler;
    std::string m_modelPath;
    ModelType m_current_model_type;
    int m_n_ctx;
    int m_n_threads;
};

#endif // LLAMA_WRAPPER_H
