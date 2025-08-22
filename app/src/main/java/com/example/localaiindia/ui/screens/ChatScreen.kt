package com.example.localaiindia.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localaiindia.model.ChatSession
import com.example.localaiindia.ui.components.*
import com.example.localaiindia.ui.theme.*
import com.example.localaiindia.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val isModelReady by chatViewModel.isModelReady.collectAsState()

    var currentMessage by remember { mutableStateOf("") }
    var isSidebarOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Get chat sessions from ViewModel
    val chatSessions by chatViewModel.chatSessions.collectAsState()
    val currentSession by chatViewModel.currentSession.collectAsState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) BackgroundDark else Background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    when {
                        dragAmount > 10f -> isSidebarOpen = true  // Swipe right to open
                        dragAmount < -10f -> isSidebarOpen = false // Swipe left to close
                    }
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            if (isSidebarOpen) {
                SidebarContent(
                    isDarkTheme = isDarkTheme,
                    chatSessions = chatSessions,
                    currentSessionId = currentSession?.id,
                    onNewChat = {
                        chatViewModel.createNewChat()
                        isSidebarOpen = false
                    },
                    onChatSelect = { sessionId ->
                        chatViewModel.switchToChat(sessionId)
                        isSidebarOpen = false
                    },
                    onDeleteChat = { sessionId ->
                        chatViewModel.deleteChat(sessionId)
                    },
                    modifier = Modifier.width(280.dp)
                )
            }

            // Main chat area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            text = currentSession?.title ?: "New Chat",
                            color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = if (isDarkTheme) OnSurfaceDark else OnSurface
                            )
                        }
                    },
                    actions = {
                        // Theme toggle button with animation
                        IconButton(
                            onClick = { scope.launch { themePreferences.toggleTheme() } },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    color = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = if (isDarkTheme) PrimaryDark else Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )

                // Chat Messages Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Welcome message when no messages
                        if (messages.isEmpty() && !isLoading) {
                            item {
                                WelcomeMessage(isDarkTheme = isDarkTheme)
                            }
                        }

                        items(messages) { message ->
                            ChatBubble(
                                message = message,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        // Loading indicator with enhanced UI
                        if (isLoading && !isModelReady) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) SurfaceDark.copy(alpha = 0.8f) else Surface.copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            strokeWidth = 4.dp,
                                            color = if (isDarkTheme) PrimaryDark else Primary
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "Initializing AI Model",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) OnSurfaceDark else OnSurface
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Setting up your offline AI assistant...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp),
                                            color = if (isDarkTheme) PrimaryDark else Primary,
                                            trackColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Message Input Area
                MessageInput(
                    message = currentMessage,
                    onMessageChange = { currentMessage = it },
                    onSendMessage = {
                        if (currentMessage.isNotBlank()) {
                            chatViewModel.sendMessage(currentMessage)
                            currentMessage = ""
                        }
                    },
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                )
            }
        }
    }
}

@Composable
private fun SidebarContent(
    isDarkTheme: Boolean,
    chatSessions: List<ChatSession>,
    currentSessionId: String?,
    onNewChat: () -> Unit,
    onChatSelect: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isDarkTheme) SurfaceDark else Surface)
            .padding(16.dp)
    ) {
        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) PrimaryDark else Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Chat",
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Divider(
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.2f) else OnSurface.copy(alpha = 0.2f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chat History
        Text(
            text = "Recent Chats",
            style = MaterialTheme.typography.titleMedium,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatSessions) { session ->
                ChatHistoryItem(
                    session = session,
                    isActive = session.id == currentSessionId,
                    isDarkTheme = isDarkTheme,
                    onClick = { onChatSelect(session.id) },
                    onDelete = { onDeleteChat(session.id) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer with developers credit
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Made with ❤️ in India",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) PrimaryDark else Primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "by Satwik & Vrushabh",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryItem(
    session: ChatSession,
    isActive: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                if (isDarkTheme) PrimaryDark.copy(alpha = 0.2f) else Primary.copy(alpha = 0.1f)
            } else {
                Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                modifier = Modifier.size(16.dp),
                tint = if (isActive) {
                    if (isDarkTheme) PrimaryDark else Primary
                } else {
                    if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive) {
                        if (isDarkTheme) PrimaryDark else Primary
                    } else {
                        if (isDarkTheme) OnSurfaceDark else OnSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )

                // Show last message preview
                session.messages.lastOrNull()?.let { lastMessage ->
                    if (!lastMessage.isTyping && lastMessage.text.isNotBlank()) {
                        Text(
                            text = lastMessage.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Delete button (only show on non-active sessions to avoid accidental deletion)
            if (!isActive) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Chat",
                        modifier = Modifier.size(14.dp),
                        tint = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.5f) else OnSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeMessage(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val keywords = listOf("Privacy", "Offline", "No Internet", "Secure", "Fast", "Local", "Made in India")
    var currentKeywordIndex by remember { mutableStateOf(0) }
    var displayText by remember { mutableStateOf("") }

    // Typing animation effect
    LaunchedEffect(Unit) {
        while (true) {
            val currentKeyword = keywords[currentKeywordIndex]

            // Typing effect - add characters
            for (i in 0..currentKeyword.length) {
                displayText = currentKeyword.take(i)
                delay(100)
            }

            delay(2000) // Hold the complete word

            // Erasing effect - remove characters
            for (i in currentKeyword.length downTo 0) {
                displayText = currentKeyword.take(i)
                delay(50)
            }

            delay(300) // Brief pause before next word
            currentKeywordIndex = (currentKeywordIndex + 1) % keywords.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Title
        Text(
            text = "Welcome to Offline AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface
        )

        Text(
            text = "Assistant",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) PrimaryDark else Primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Animated typing keywords
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkTheme) SurfaceDark else Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) PrimaryDark else Primary,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Start a conversation with your personal AI assistant that works completely offline. No internet connection required!",
            fontSize = 16.sp,
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.8f) else OnSurface.copy(alpha = 0.8f),
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature highlights
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureHighlight(
                title = "Personal AI assistant",
                isDarkTheme = isDarkTheme
            )

            FeatureHighlight(
                title = "No internet required",
                isDarkTheme = isDarkTheme
            )

            FeatureHighlight(
                title = "Private & Secure",
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun FeatureHighlight(
    title: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDarkTheme) PrimaryDark else Primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface
        )
    }
}