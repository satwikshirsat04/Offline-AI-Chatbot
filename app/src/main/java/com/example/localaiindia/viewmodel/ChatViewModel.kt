package com.example.localaiindia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localaiindia.LlamaService
import com.example.localaiindia.model.ChatMessage
import com.example.localaiindia.model.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val llamaService = LlamaService()

    // Current selected model
    private val _currentModel = MutableStateFlow<String?>(null)
    val currentModel: StateFlow<String?> = _currentModel.asStateFlow()

    // Current chat session
    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    // All chat sessions
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // Current messages (for the active chat)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    init {
        // Don't auto-initialize any model - wait for user selection
    }

    fun initializeModel(modelId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isModelReady.value = false
                _currentModel.value = modelId

                // Initialize the model
                val success = llamaService.initializeModel(getApplication(), modelId)
                _isModelReady.value = success

                if (success) {
                    // Create new chat session when model is successfully initialized
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

        // Add welcome message for new chat only if model is ready
        if (_isModelReady.value) {
            addWelcomeMessage()
        }
    }

    fun switchToChat(sessionId: String) {
        // Save current session first
        saveCurrentSession()

        // Find and switch to the selected session
        val selectedSession = _chatSessions.value.find { it.id == sessionId }
        selectedSession?.let { session ->
            _currentSession.value = session
            _messages.value = session.messages
        }
    }

    private fun saveCurrentSession() {
        _currentSession.value?.let { session ->
            val updatedSession = session.copy(
                messages = _messages.value,
                lastUpdated = Date(),
                title = generateChatTitle(_messages.value)
            )

            // Update or add session to the list
            val updatedSessions = _chatSessions.value.toMutableList()
            val existingIndex = updatedSessions.indexOfFirst { it.id == session.id }

            if (existingIndex >= 0) {
                updatedSessions[existingIndex] = updatedSession
            } else {
                updatedSessions.add(0, updatedSession) // Add to top
            }

            _chatSessions.value = updatedSessions
            _currentSession.value = updatedSession
        }
    }

    private fun generateChatTitle(messages: List<ChatMessage>): String {
        // Find the first user message to generate title
        val firstUserMessage = messages.find { it.isFromUser && !it.isTyping }?.text
        return if (!firstUserMessage.isNullOrBlank()) {
            // Take first few words to create a title
            val words = firstUserMessage.split(" ")
            val truncated = if (words.size > 5) {
                words.take(5).joinToString(" ") + "..."
            } else {
                firstUserMessage
            }
            // Limit title length
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
                text = "✨ Welcome to Local AI! I'm your offline AI assistant powered by $modelName. I work completely on your device to keep your conversations private and secure. How can I help you today? 🤔",
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

        // Add user message
        val userMessage = ChatMessage(
            text = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage

        // Add typing indicator
        val typingMessage = ChatMessage(
            text = "",
            isFromUser = false,
            isTyping = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + typingMessage

        // Generate AI response
        viewModelScope.launch {
            try {
                val response = llamaService.chat(text)

                // Remove typing indicator and add actual response
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = response,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )

                // Auto-save session after each message
                saveCurrentSession()

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error sending message", e)
                // Remove typing indicator and add error message
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "I apologize, but I encountered an error. Please try again.",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    fun deleteChat(sessionId: String) {
        val updatedSessions = _chatSessions.value.filter { it.id != sessionId }
        _chatSessions.value = updatedSessions

        // If we deleted the current session, create a new one
        if (_currentSession.value?.id == sessionId) {
            createNewChat()
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        if (_isModelReady.value) {
            addWelcomeMessage()
        }
    }

    fun clearAllChats() {
        _chatSessions.value = emptyList()
        createNewChat()
    }

    private fun formatTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            // Save current session before clearing
            saveCurrentSession()
            llamaService.destroy()
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error in onCleared", e)
        }
    }
}