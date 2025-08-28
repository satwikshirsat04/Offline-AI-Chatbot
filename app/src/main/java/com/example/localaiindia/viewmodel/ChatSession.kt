package com.example.localaiindia.model

import java.util.Date

data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val createdAt: Date,
    val lastUpdated: Date
)