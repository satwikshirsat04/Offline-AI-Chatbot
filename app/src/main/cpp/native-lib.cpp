#include <jni.h>
#include <string>
#include <android/log.h>
#include "llama_wrapper.h"

#define LOG_TAG "LocalAIIndia"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static LlamaWrapper* g_llama_wrapper = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_localaiindia_LlamaService_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from Local AI India C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_localaiindia_LlamaService_initializeModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath) {
    
    const char* path = env->GetStringUTFChars(modelPath, 0);
    LOGI("Initializing model at path: %s", path);
    
    try {
        if (g_llama_wrapper == nullptr) {
            g_llama_wrapper = new LlamaWrapper();
        }
        
        bool result = g_llama_wrapper->initialize(std::string(path));
        env->ReleaseStringUTFChars(modelPath, path);
        
        if (result) {
            LOGI("Model initialized successfully");
        } else {
            LOGE("Failed to initialize model");
        }
        
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception during model initialization: %s", e.what());
        env->ReleaseStringUTFChars(modelPath, path);
        return false;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_localaiindia_LlamaService_generateResponse(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt) {
    
    if (g_llama_wrapper == nullptr) {
        LOGE("Model not initialized");
        return env->NewStringUTF("Error: Model not initialized");
    }
    
    const char* promptStr = env->GetStringUTFChars(prompt, 0);
    LOGI("Generating response for prompt: %s", promptStr);
    
    try {
        std::string response = g_llama_wrapper->generateResponse(std::string(promptStr));
        env->ReleaseStringUTFChars(prompt, promptStr);
        
        LOGI("Generated response: %s", response.c_str());
        return env->NewStringUTF(response.c_str());
    } catch (const std::exception& e) {
        LOGE("Exception during response generation: %s", e.what());
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("Error: Failed to generate response");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_localaiindia_LlamaService_cleanup(
        JNIEnv* env,
        jobject /* this */) {
    
    LOGI("Cleaning up model");
    if (g_llama_wrapper != nullptr) {
        delete g_llama_wrapper;
        g_llama_wrapper = nullptr;
    }
}
