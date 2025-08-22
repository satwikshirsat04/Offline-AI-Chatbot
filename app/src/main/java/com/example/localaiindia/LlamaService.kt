package com.example.localaiindia

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaService {
    companion object {
        private const val TAG = "LlamaService"
        
        init {
            try {
                System.loadLibrary("localaiindia")
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    private var isInitialized = false
    
    // Native methods
    external fun stringFromJNI(): String
    external fun initializeModel(modelPath: String): Boolean
    external fun generateResponse(prompt: String): String
    external fun cleanup()
    
    suspend fun initialize(context: Context, modelFileName: String = "LFM2-1.2B-Q4_0.gguf"): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing LlamaService")
            
            val modelFile = try {
                copyAssetToInternalStorage(context, modelFileName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load from assets: ${e.message}")
                File(context.filesDir, modelFileName)
            }
            
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found at ${modelFile.absolutePath}")
                val downloadsFile = File(context.getExternalFilesDir(null), modelFileName)
                if (downloadsFile.exists()) {
                    Log.i(TAG, "Found model in external files, copying to internal storage...")
                    downloadsFile.copyTo(modelFile, overwrite = true)
                }
            }
            
            isInitialized = initializeModel(modelFile.absolutePath)
            Log.i(TAG, "Model initialization result: $isInitialized")
            
            return@withContext isInitialized
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            return@withContext false
        }
    }
    
    private fun copyAssetToInternalStorage(context: Context, fileName: String): File {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val outputFile = File(context.filesDir, fileName)
        
        if (!outputFile.exists()) {
            Log.i(TAG, "Copying model from assets to internal storage...")
            outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Log.i(TAG, "Model copied successfully to ${outputFile.absolutePath}")
        }
        
        inputStream.close()
        return outputFile
    }
    
    suspend fun chat(prompt: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isInitialized) {
                "I'm not fully initialized yet. Please make sure the model file is available."
            } else {
                generateResponse(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            "I encountered an error while processing your request. Please try again."
        }
    }
    
    fun isReady(): Boolean = isInitialized
    
    fun destroy() {
        if (isInitialized) {
            cleanup()
            isInitialized = false
        }
    }
}
