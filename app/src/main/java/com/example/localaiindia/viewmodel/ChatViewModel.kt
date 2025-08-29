package com.example.localaiindia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localaiindia.LlamaService
import com.example.localaiindia.benchmark.BenchmarkService
import com.example.localaiindia.model.ChatMessage
import com.example.localaiindia.model.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val llamaService = LlamaService()
    private val benchmarkService = BenchmarkService(application)

    // Existing state flows
    private val _currentModel = MutableStateFlow<String?>(null)
    val currentModel: StateFlow<String?> = _currentModel.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    // New benchmarking state flows
    private val _responseTimeHistory = MutableStateFlow<List<ResponseTimeEntry>>(emptyList())
    val responseTimeHistory: StateFlow<List<ResponseTimeEntry>> = _responseTimeHistory.asStateFlow()

    private val _showBenchmarkMenu = MutableStateFlow(false)
    val showBenchmarkMenu: StateFlow<Boolean> = _showBenchmarkMenu.asStateFlow()

    private val _benchmarkProgress = MutableStateFlow<BenchmarkService.BenchmarkProgress?>(null)
    val benchmarkProgress: StateFlow<BenchmarkService.BenchmarkProgress?> = _benchmarkProgress.asStateFlow()

    // Session performance tracking
    private val _sessionStats = MutableStateFlow<SessionStats?>(null)
    val sessionStats: StateFlow<SessionStats?> = _sessionStats.asStateFlow()

    data class ResponseTimeEntry(
        val promptIndex: Int,
        val prompt: String,
        val responseTime: Long,
        val timestamp: Long,
        val modelId: String,
        val tokenCount: Int = 0,
        val success: Boolean = true
    )

    data class SessionStats(
        val totalPrompts: Int,
        val averageResponseTime: Double,
        val p95ResponseTime: Double,
        val p99ResponseTime: Double,
        val minResponseTime: Long,
        val maxResponseTime: Long,
        val successRate: Double,
        val totalTokens: Int,
        val tokensPerSecond: Double
    )

    init {
        // Observe benchmark progress
        viewModelScope.launch {
            benchmarkService.benchmarkProgress.collect { progress ->
                _benchmarkProgress.value = progress
            }
        }
    }

    fun initializeModel(modelId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isModelReady.value = false
                _currentModel.value = modelId

                val success = llamaService.initializeModel(getApplication(), modelId)
                _isModelReady.value = success

                if (success) {
                    // Clear response time history when switching models
                    _responseTimeHistory.value = emptyList()
                    _sessionStats.value = null
                    createNewChat()
                } else {
                    _currentModel.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error initializing model", e)
                _isModelReady.value = false
                _currentModel.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewChat() {
        // Save current session if it exists and has messages
        _currentSession.value?.let { session ->
            if (session.messages.isNotEmpty()) {
                saveCurrentSession()
            }
        }

        // Create new session
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            messages = emptyList(),
            createdAt = Date(),
            lastUpdated = Date()
        )

        _currentSession.value = newSession
        _messages.value = emptyList()

        // Reset session stats
        _sessionStats.value = null

        if (_isModelReady.value) {
            addWelcomeMessage()
        }
    }

    fun switchToChat(sessionId: String) {
        saveCurrentSession()

        val selectedSession = _chatSessions.value.find { it.id == sessionId }
        selectedSession?.let { session ->
            _currentSession.value = session
            _messages.value = session.messages
            
            // Calculate session stats for this chat
            calculateSessionStats()
        }
    }

    private fun saveCurrentSession() {
        _currentSession.value?.let { session ->
            val updatedSession = session.copy(
                messages = _messages.value,
                lastUpdated = Date(),
                title = generateChatTitle(_messages.value)
            )

            val updatedSessions = _chatSessions.value.toMutableList()
            val existingIndex = updatedSessions.indexOfFirst { it.id == session.id }

            if (existingIndex >= 0) {
                updatedSessions[existingIndex] = updatedSession
            } else {
                updatedSessions.add(0, updatedSession)
            }

            _chatSessions.value = updatedSessions
            _currentSession.value = updatedSession
        }
    }

    private fun generateChatTitle(messages: List<ChatMessage>): String {
        val firstUserMessage = messages.find { it.isFromUser && !it.isTyping }?.text
        return if (!firstUserMessage.isNullOrBlank()) {
            val words = firstUserMessage.split(" ")
            val truncated = if (words.size > 5) {
                words.take(5).joinToString(" ") + "..."
            } else {
                firstUserMessage
            }
            if (truncated.length > 50) {
                truncated.take(47) + "..."
            } else {
                truncated
            }
        } else {
            "New Chat"
        }
    }

    private fun addWelcomeMessage() {
        if (_currentModel.value != null && _isModelReady.value) {
            val modelName = getModelDisplayName(_currentModel.value ?: "")
            val welcomeMessage = ChatMessage(
                text = "Welcome to Local AI! I'm your offline AI assistant powered by $modelName. I work completely on your device to keep your conversations private and secure. How can I help you today?",
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = listOf(welcomeMessage)
        }
    }

    private fun getModelDisplayName(modelId: String): String {
        return when {
            modelId.contains("lfm2") -> "LFM2 1.2B"
            modelId.contains("phi4") -> "Phi-4 Mini Instruct"
            modelId.contains("qwen")  -> "Qwen 1.5 1.8B"
            modelId.contains("deepseek") -> "DeepSeek-R1 Distill 1.5B"
            else -> "AI Model"
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || !_isModelReady.value) return

        val userMessage = ChatMessage(
            text = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage

        val typingMessage = ChatMessage(
            text = "",
            isFromUser = false,
            isTyping = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + typingMessage

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                val response = llamaService.chat(text)
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                // Record response time
                recordResponseTime(text, responseTime, response)

                // Remove typing indicator and add actual response
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = response,
                    isFromUser = false,
                    timestamp = endTime
                )

                saveCurrentSession()
                calculateSessionStats()

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error sending message", e)
                val responseTime = System.currentTimeMillis() - startTime
                
                // Record failed response time
                recordResponseTime(text, responseTime, "", success = false)

                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "I apologize, but I encountered an error. Please try again.",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun recordResponseTime(prompt: String, responseTime: Long, response: String, success: Boolean = true) {
        val currentModel = _currentModel.value ?: return
        val currentHistory = _responseTimeHistory.value.toMutableList()
        
        val entry = ResponseTimeEntry(
            promptIndex = currentHistory.size,
            prompt = prompt.take(100), // Truncate for storage
            responseTime = responseTime,
            timestamp = System.currentTimeMillis(),
            modelId = currentModel,
            tokenCount = estimateTokenCount(response),
            success = success
        )
        
        currentHistory.add(entry)
        _responseTimeHistory.value = currentHistory
    }

    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4.0).toInt() // Rough approximation
    }

    private fun calculateSessionStats() {
        val history = _responseTimeHistory.value.filter { it.success }
        if (history.isEmpty()) {
            _sessionStats.value = null
            return
        }

        val responseTimes = history.map { it.responseTime }
        val sortedTimes = responseTimes.sorted()

        _sessionStats.value = SessionStats(
            totalPrompts = _responseTimeHistory.value.size,
            averageResponseTime = responseTimes.average(),
            p95ResponseTime = percentile(sortedTimes, 0.95),
            p99ResponseTime = percentile(sortedTimes, 0.99),
            minResponseTime = sortedTimes.minOrNull() ?: 0L,
            maxResponseTime = sortedTimes.maxOrNull() ?: 0L,
            successRate = (history.size.toDouble() / _responseTimeHistory.value.size) * 100,
            totalTokens = history.sumOf { it.tokenCount },
            tokensPerSecond = calculateTokensPerSecond(history)
        )
    }

    private fun percentile(sortedList: List<Long>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        val index = (percentile * (sortedList.size - 1)).toInt()
        return sortedList[index.coerceIn(0, sortedList.size - 1)].toDouble()
    }

    private fun calculateTokensPerSecond(history: List<ResponseTimeEntry>): Double {
        val totalTokens = history.sumOf { it.tokenCount }
        val totalTimeSeconds = history.sumOf { it.responseTime } / 1000.0
        return if (totalTimeSeconds > 0) totalTokens / totalTimeSeconds else 0.0
    }

    // Benchmark functions
    fun startBenchmark(promptCount: Int = 100) {
        val modelId = _currentModel.value ?: return
        val modelName = getModelDisplayName(modelId)
        
        viewModelScope.launch {
            benchmarkService.startBenchmark(modelId, modelName, llamaService, promptCount)
        }
    }

    fun cancelBenchmark() {
        benchmarkService.cancelBenchmark()
    }

    fun toggleBenchmarkMenu() {
        _showBenchmarkMenu.value = !_showBenchmarkMenu.value
    }

    fun closeBenchmarkMenu() {
        _showBenchmarkMenu.value = false
    }

    // Export functions for analysis
    fun exportResponseTimes(): String {
        val history = _responseTimeHistory.value
        if (history.isEmpty()) return "No data available"

        val csv = StringBuilder()
        csv.appendLine("Index,Prompt,ResponseTime(ms),Timestamp,ModelId,TokenCount,Success")
        
        history.forEach { entry ->
            csv.appendLine("${entry.promptIndex},\"${entry.prompt.replace("\"", "\"\"")}\",${entry.responseTime},${entry.timestamp},${entry.modelId},${entry.tokenCount},${entry.success}")
        }
        
        return csv.toString()
    }

    fun getPerformanceReport(): String {
        val stats = _sessionStats.value ?: return "No performance data available"
        
        return buildString {
            appendLine("Performance Report")
            appendLine("=================")
            appendLine("Model: ${getModelDisplayName(_currentModel.value ?: "Unknown")}")
            appendLine("Total Prompts: ${stats.totalPrompts}")
            appendLine("Success Rate: ${"%.1f".format(stats.successRate)}%")
            appendLine("Average Response Time: ${"%.1f".format(stats.averageResponseTime)}ms")
            appendLine("P95 Response Time: ${"%.1f".format(stats.p95ResponseTime)}ms")
            appendLine("P99 Response Time: ${"%.1f".format(stats.p99ResponseTime)}ms")
            appendLine("Min Response Time: ${stats.minResponseTime}ms")
            appendLine("Max Response Time: ${stats.maxResponseTime}ms")
            appendLine("Tokens per Second: ${"%.1f".format(stats.tokensPerSecond)}")
            appendLine("Total Tokens Generated: ${stats.totalTokens}")
        }
    }

    fun clearResponseTimeHistory() {
        _responseTimeHistory.value = emptyList()
        _sessionStats.value = null
    }

    fun deleteChat(sessionId: String) {
        val updatedSessions = _chatSessions.value.filter { it.id != sessionId }
        _chatSessions.value = updatedSessions

        if (_currentSession.value?.id == sessionId) {
            createNewChat()
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _responseTimeHistory.value = emptyList()
        _sessionStats.value = null
        
        if (_isModelReady.value) {
            addWelcomeMessage()
        }
    }

    fun clearAllChats() {
        _chatSessions.value = emptyList()
        createNewChat()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            saveCurrentSession()
            llamaService.destroy()
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error in onCleared", e)
        }
    }
}