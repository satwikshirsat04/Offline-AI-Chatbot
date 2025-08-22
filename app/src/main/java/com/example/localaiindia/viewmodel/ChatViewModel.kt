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
        initializeModel()
        createNewChat() // Start with a new chat
    }

    private fun initializeModel() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = llamaService.initialize(getApplication())
            _isModelReady.value = success
            _isLoading.value = false
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

        // Add welcome message for new chat
        addWelcomeMessage()
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
                title = generateChatTitle(session.messages)
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

    // In ChatViewModel.kt, update the generateChatTitle function
    private fun generateChatTitle(messages: List<ChatMessage>): String {
        // Find the first user message to generate title
        val firstUserMessage = messages.find { it.isFromUser }?.text
        return if (!firstUserMessage.isNullOrBlank()) {
            // Take first few words to create a title
            val words = firstUserMessage.split(" ")
            val truncated = if (words.size > 5) {
                words.take(5).joinToString(" ") + "..."
            } else {
                firstUserMessage
            }
            truncated
        } else {
            "New Chat"
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            text = "🇮🇳 Welcome to Satwik AI! I'm your offline AI assistant. I work completely on your device to keep your conversations private and secure. How can I help you today?",
            isFromUser = false
        )
        _messages.value = listOf(welcomeMessage)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(text = text, isFromUser = true)
        _messages.value = _messages.value + userMessage

        // Add typing indicator
        val typingMessage = ChatMessage(text = "", isFromUser = false, isTyping = true)
        _messages.value = _messages.value + typingMessage

        // Generate AI response
        viewModelScope.launch {
            try {
                val response = llamaService.chat(text)

                // Remove typing indicator and add actual response
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = response,
                    isFromUser = false
                )

                // Auto-save session after each message
                saveCurrentSession()

            } catch (e: Exception) {
                // Remove typing indicator and add error message
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "I apologize, but I encountered an error. Please try again.",
                    isFromUser = false
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
        addWelcomeMessage()
    }

    fun clearAllChats() {
        _chatSessions.value = emptyList()
        createNewChat()
    }

    // Get formatted chat sessions for display
    fun getFormattedChatSessions(): List<ChatSessionDisplay> {
        return _chatSessions.value.map { session ->
            ChatSessionDisplay(
                id = session.id,
                title = session.title,
                lastMessage = session.messages.lastOrNull()?.text ?: "No messages",
                formattedTime = formatTime(session.lastUpdated),
                isActive = session.id == _currentSession.value?.id
            )
        }.sortedByDescending { it.session.lastUpdated }
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
        // Save current session before clearing
        saveCurrentSession()
        llamaService.destroy()
    }
}

// Data class for displaying chat sessions in the sidebar
data class ChatSessionDisplay(
    val id: String,
    val title: String,
    val lastMessage: String,
    val formattedTime: String,
    val isActive: Boolean
) {
    // Keep reference to actual session for sorting
    val session: ChatSession
        get() = ChatSession(id, title, emptyList(), Date(), Date())
}