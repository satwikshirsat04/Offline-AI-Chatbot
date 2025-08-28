package com.example.localaiindia

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LlamaService {
    companion object {
        private const val TAG = "LlamaService"

        // Load the native library - FIXED: Match CMakeLists.txt project name
        init {
            try {
                System.loadLibrary("localaiindia")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }

        // Available model configurations
        val AVAILABLE_MODELS = mapOf(
            "lfm2" to ModelConfig(
                fileName = "LFM2-1.2B-Q4_0.gguf",
                displayName = "LFM2 1.2B",
                description = "Balanced model for general conversations",
                sizeInMB = 663
            ),
            "phi4" to ModelConfig(
                fileName = "Phi-4-mini-instruct-Q4_K_M.gguf",
                displayName = "Phi-4 Mini Instruct",
                description = "Microsoft's efficient instruction-following model",
                sizeInMB = 2300
            ),
            "qwen" to ModelConfig(
                fileName = "qwen1_5-1_8b-chat-q4_0.gguf",
                displayName = "Qwen 1.5 1.8B",
                description = "Alibaba's reasoning and multilingual model",
                sizeInMB = 1040
            ),
            "deepseek" to ModelConfig(
                fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
                displayName = "DeepSeek-R1 Distill 1.5B",
                description = "Advanced reasoning model with step-by-step thinking",
                sizeInMB = 1040
            )
        )
    }

    data class ModelConfig(
        val fileName: String,
        val displayName: String,
        val description: String,
        val sizeInMB: Int
    )

    private var currentModelId: String? = null
    private var isModelLoaded = false

    // Native method declarations
    private external fun nativeInitialize(modelPath: String): Boolean
    private external fun nativeGenerateResponse(prompt: String): String
    private external fun nativeCleanup()
    private external fun nativeIsInitialized(): Boolean

    /**
     * Initialize a specific model by its ID
     */
    suspend fun initializeModel(context: Context, modelId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing model: $modelId (current: $currentModelId)")

            // Check if model exists in our configuration
            val modelConfig = AVAILABLE_MODELS[modelId]
            if (modelConfig == null) {
                Log.e(TAG, "Unknown model ID: $modelId")
                return@withContext false
            }

            // IMPORTANT: Always clean up existing model first, even if same model
            // This ensures proper reset of templates and context
            if (isModelLoaded || currentModelId != null) {
                try {
                    Log.d(TAG, "Cleaning up existing model: $currentModelId")
                    nativeCleanup()
                } catch (e: Exception) {
                    Log.w(TAG, "Error during cleanup", e)
                }
                isModelLoaded = false
                currentModelId = null
                
                // Add small delay to ensure complete cleanup
                Thread.sleep(100)
            }

            // Get or copy model file to internal storage
            val modelFile = getModelFile(context, modelConfig.fileName)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: ${modelFile.absolutePath}")
                return@withContext false
            }

            Log.i(TAG, "Initializing model file: ${modelFile.absolutePath}")

            // Initialize the model
            val success = try {
                nativeInitialize(modelFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Native initialization failed", e)
                false
            }

            if (success) {
                currentModelId = modelId
                isModelLoaded = true
                Log.i(TAG, "Successfully initialized model: $modelId")
                
                // Verify initialization
                val isReady = try {
                    nativeIsInitialized()
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking initialization status", e)
                    false
                }
                
                if (!isReady) {
                    Log.e(TAG, "Model initialization verification failed")
                    currentModelId = null
                    isModelLoaded = false
                    return@withContext false
                }
            } else {
                Log.e(TAG, "Failed to initialize model: $modelId")
                currentModelId = null
                isModelLoaded = false
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model: $modelId", e)
            currentModelId = null
            isModelLoaded = false
            false
        }
    }

    /**
     * Generate chat response
     */
    suspend fun chat(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            if (!isModelLoaded || currentModelId == null) {
                return@withContext "Error: Model not initialized. Please select a model first."
            }

            val isNativeReady = try {
                nativeIsInitialized()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking native initialization", e)
                false
            }

            if (!isNativeReady) {
                Log.e(TAG, "Native model not ready, attempting to reinitialize")
                return@withContext "Error: Model not ready. Please try switching models or restart the app."
            }

            Log.d(TAG, "Generating response with model: $currentModelId, prompt: ${prompt.take(100)}...")
            val response = try {
                nativeGenerateResponse(prompt)
            } catch (e: Exception) {
                Log.e(TAG, "Error in native response generation", e)
                "I apologize, but I encountered an error while generating a response. Please try again or switch models."
            }

            Log.d(TAG, "Generated response length: ${response.length}")
            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            return@withContext "I apologize, but I encountered an error while generating a response. Please try again."
        }
    }

    /**
     * Get current model information
     */
    fun getCurrentModelInfo(): ModelConfig? {
        return currentModelId?.let { AVAILABLE_MODELS[it] }
    }

    /**
     * Check if a model is currently loaded
     */
    fun isModelReady(): Boolean {
        return try {
            isModelLoaded && currentModelId != null && nativeIsInitialized()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if model is ready", e)
            false
        }
    }

    /**
     * Get the current model ID
     */
    fun getCurrentModelId(): String? {
        return currentModelId
    }

    /**
     * Clean up and destroy the service
     */
    fun destroy() {
        try {
            if (isModelLoaded) {
                Log.d(TAG, "Destroying LlamaService with model: $currentModelId")
                nativeCleanup()
                isModelLoaded = false
                currentModelId = null
                Log.d(TAG, "LlamaService destroyed successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying LlamaService", e)
        }
    }

    /**
     * Get model file from assets or internal storage
     */
    private fun getModelFile(context: Context, fileName: String): File {
        val internalFile = File(context.filesDir, fileName)

        // If model already exists in internal storage, use it
        if (internalFile.exists()) {
            Log.d(TAG, "Using existing model file: ${internalFile.absolutePath}")
            return internalFile
        }

        // Try to copy from assets
        try {
            context.assets.open(fileName).use { inputStream ->
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.i(TAG, "Copied model from assets to: ${internalFile.absolutePath}")
        } catch (e: IOException) {
            Log.w(TAG, "Could not copy model from assets: $fileName", e)
            // Model might be in external storage or downloaded separately
        }

        return internalFile
    }

    /**
     * Check if a specific model file exists
     */
    fun isModelFileAvailable(context: Context, modelId: String): Boolean {
        val modelConfig = AVAILABLE_MODELS[modelId] ?: return false
        val modelFile = getModelFile(context, modelConfig.fileName)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Switch to a different model
     */
    suspend fun switchModel(context: Context, newModelId: String): Boolean {
        Log.d(TAG, "Switching from $currentModelId to $newModelId")
        return if (newModelId != currentModelId) {
            initializeModel(context, newModelId)
        } else {
            Log.d(TAG, "Already using model: $newModelId")
            true // Already using this model
        }
    }

    /**
     * Force reinitialize current model (useful for troubleshooting)
     */
    suspend fun reinitializeCurrentModel(context: Context): Boolean {
        val modelId = currentModelId ?: return false
        Log.d(TAG, "Force reinitializing model: $modelId")
        return initializeModel(context, modelId)
    }
}