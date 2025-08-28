#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>
#include "llama_wrapper.h"

#define LOG_TAG "JNIWrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global instance of LlamaWrapper
static std::unique_ptr<LlamaWrapper> g_llamaWrapper;

// Helper function to convert jstring to std::string
std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";

    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (chars == nullptr) return "";

    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_localaiindia_LlamaService_nativeInitialize(JNIEnv* env, jobject thiz, jstring modelPath) {
    try {
        LOGI("JNI nativeInitialize called");

        std::string model_path = jstring_to_string(env, modelPath);
        LOGI("Model path: %s", model_path.c_str());

        // Create new wrapper instance
        g_llamaWrapper = std::make_unique<LlamaWrapper>();

        bool success = g_llamaWrapper->initialize(model_path);
        LOGI("Initialization result: %s", success ? "SUCCESS" : "FAILED");

        return success ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception in nativeInitialize: %s", e.what());
        return JNI_FALSE;
    } catch (...) {
        LOGE("Unknown exception in nativeInitialize");
        return JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_localaiindia_LlamaService_nativeGenerateResponse(JNIEnv* env, jobject thiz, jstring prompt) {
    try {
        if (!g_llamaWrapper) {
            LOGE("LlamaWrapper not initialized");
            return env->NewStringUTF("Error: Model not initialized");
        }

        std::string input_prompt = jstring_to_string(env, prompt);
        LOGI("Generating response for prompt length: %zu", input_prompt.length());

        std::string response = g_llamaWrapper->generateResponse(input_prompt);
        LOGI("Generated response length: %zu", response.length());

        return env->NewStringUTF(response.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception in nativeGenerateResponse: %s", e.what());
        return env->NewStringUTF("Error generating response");
    } catch (...) {
        LOGE("Unknown exception in nativeGenerateResponse");
        return env->NewStringUTF("Error generating response");
    }
}

JNIEXPORT void JNICALL
Java_com_example_localaiindia_LlamaService_nativeCleanup(JNIEnv* env, jobject thiz) {
    try {
        LOGI("JNI nativeCleanup called");
        if (g_llamaWrapper) {
            g_llamaWrapper->cleanup();
            g_llamaWrapper.reset();
        }
        LOGI("Cleanup completed");
    } catch (const std::exception& e) {
        LOGE("Exception in nativeCleanup: %s", e.what());
    } catch (...) {
        LOGE("Unknown exception in nativeCleanup");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_localaiindia_LlamaService_nativeIsInitialized(JNIEnv* env, jobject thiz) {
    try {
        bool initialized = g_llamaWrapper && g_llamaWrapper->isInitialized();
        return initialized ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

} // extern "C"