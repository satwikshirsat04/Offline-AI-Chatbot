#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>
#include "llama_wrapper.h"

#define LOG_TAG "LocalAIIndia"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<LlamaWrapper> g_llama_wrapper;

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

    const char* path = nullptr;

    try {
        path = env->GetStringUTFChars(modelPath, 0);
        if (!path) {
            LOGE("Failed to get model path string");
            return JNI_FALSE;
        }

        LOGI("Initializing model at path: %s", path);

        g_llama_wrapper.reset();
        g_llama_wrapper = std::make_unique<LlamaWrapper>();

        bool result = g_llama_wrapper->initialize(std::string(path));

        env->ReleaseStringUTFChars(modelPath, path);
        path = nullptr;

        if (result) {
            LOGI("Model initialized successfully");
        } else {
            LOGE("Failed to initialize model");
            g_llama_wrapper.reset();
        }

        return result ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception during model initialization: %s", e.what());
        if (path) {
            env->ReleaseStringUTFChars(modelPath, path);
        }
        g_llama_wrapper.reset();
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_localaiindia_LlamaService_generateResponse(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt) {

    if (!g_llama_wrapper) {
        LOGE("Model not initialized");
        return env->NewStringUTF("Error: Model not initialized");
    }

    const char* promptStr = nullptr;

    try {
        promptStr = env->GetStringUTFChars(prompt, 0);
        if (!promptStr) {
            LOGE("Failed to get prompt string");
            return env->NewStringUTF("Error: Invalid prompt");
        }

        LOGI("Generating response for prompt: %.30s...", promptStr);

        std::string response = g_llama_wrapper->generateResponse(std::string(promptStr));

        env->ReleaseStringUTFChars(prompt, promptStr);
        promptStr = nullptr;

        LOGI("Generated response: %.50s...", response.c_str());
        return env->NewStringUTF(response.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception during response generation: %s", e.what());
        if (promptStr) {
            env->ReleaseStringUTFChars(prompt, promptStr);
        }
        return env->NewStringUTF("Error: Failed to generate response");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_localaiindia_LlamaService_cleanup(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Cleaning up model");
    try {
        g_llama_wrapper.reset();
        LOGI("Model cleanup completed");
    } catch (const std::exception& e) {
        LOGE("Exception during cleanup: %s", e.what());
    }
}
