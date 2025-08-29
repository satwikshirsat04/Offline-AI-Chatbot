package com.example.localaiindia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localaiindia.model.ChatSession
import com.example.localaiindia.ui.components.ChatBubble
import com.example.localaiindia.ui.components.MessageInput
import com.example.localaiindia.ui.theme.Background
import com.example.localaiindia.ui.theme.BackgroundDark
import com.example.localaiindia.ui.theme.OnSurface
import com.example.localaiindia.ui.theme.OnSurfaceDark
import com.example.localaiindia.ui.theme.Primary
import com.example.localaiindia.ui.theme.PrimaryDark
import com.example.localaiindia.ui.theme.Secondary
import com.example.localaiindia.ui.theme.SecondaryDark
import com.example.localaiindia.ui.theme.Surface
import com.example.localaiindia.ui.theme.SurfaceDark
import com.example.localaiindia.ui.theme.ThemePreferences
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
    val currentModel by chatViewModel.currentModel.collectAsState()

    var currentMessage by remember { mutableStateOf("") }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var showModelSelection by remember { mutableStateOf(currentModel == null) }
    val listState = rememberLazyListState()

    // Get chat sessions from ViewModel
    val chatSessions by chatViewModel.chatSessions.collectAsState()
    val currentSession by chatViewModel.currentSession.collectAsState()

    // Show model selection if no model is selected
    if (showModelSelection) {
        ModelSelectionScreen(
            onModelSelected = { selectedModel ->
                chatViewModel.initializeModel(selectedModel.id)
                showModelSelection = false
            },
            isDarkTheme = isDarkTheme
        )
        return
    }

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
                    currentModel = currentModel,
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
                    onChangeModel = {
                        showModelSelection = true
                        isSidebarOpen = false
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
                        Column {
                            Text(
                                text = currentSession?.title ?: "New Chat",
                                color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            currentModel?.let { model ->
                                Text(
                                    text = "Using ${getModelDisplayName(model)}",
                                    color = if (isDarkTheme) PrimaryDark else Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
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
                        // Benchmark access button
                        IconButton(
                            onClick = {
                                chatViewModel.toggleBenchmarkMenu()
                            },
                            modifier = Modifier
                                .background(
                                    color = if (isDarkTheme) SecondaryDark.copy(alpha = 0.1f) else Secondary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Performance Benchmark",
                                tint = if (isDarkTheme) SecondaryDark else Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Model selection button
                        IconButton(
                            onClick = { showModelSelection = true },
                            modifier = Modifier
                                .background(
                                    color = if (isDarkTheme) SecondaryDark.copy(alpha = 0.1f) else Secondary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Change Model",
                                tint = if (isDarkTheme) SecondaryDark else Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Theme toggle button
                        IconButton(
                            onClick = { scope.launch { themePreferences.toggleTheme() } },
                            modifier = Modifier
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
                                modifier = Modifier.size(20.dp)
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
                                WelcomeMessage(
                                    isDarkTheme = isDarkTheme,
                                    currentModel = currentModel
                                )
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
                                ModelInitializationCard(
                                    isDarkTheme = isDarkTheme,
                                    currentModel = currentModel
                                )
                            }
                        }
                    }
                }

                // Benchmark Menu Overlay
                val showBenchmarkMenu by chatViewModel.showBenchmarkMenu.collectAsState()
                if (showBenchmarkMenu) {
                    BenchmarkMenuOverlay(
                        isDarkTheme = isDarkTheme,
                        chatViewModel = chatViewModel,
                        onDismiss = { chatViewModel.closeBenchmarkMenu() },
                        onNavigateToBenchmark = { /* Handle navigation to full benchmark screen */ }
                    )
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
                    isEnabled = isModelReady && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                )
            }
        }
    }
}

@Composable
fun BenchmarkMenuOverlay(
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onNavigateToBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionStats by chatViewModel.sessionStats.collectAsState()
    val responseTimeHistory by chatViewModel.responseTimeHistory.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) { }, // Prevent dismiss when clicking card
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) SurfaceDark else Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Performance Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) OnSurfaceDark else OnSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) OnSurfaceDark else OnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Stats - Fix the smart cast issue
                sessionStats?.let { stats ->
                    QuickStatsGrid(
                        sessionStats = stats,
                        isDarkTheme = isDarkTheme
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onNavigateToBenchmark()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) PrimaryDark else Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Full Dashboard",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Full Benchmark Dashboard",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            chatViewModel.startBenchmark(50)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) SecondaryDark else Secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Quick Benchmark",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Quick Benchmark (50 prompts)")
                    }

                    if (responseTimeHistory.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                // Export current session data
                                val report = chatViewModel.getPerformanceReport()
                                // Handle sharing report
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkTheme) OnSurfaceDark else OnSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Report",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Session Report")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsGrid(
    sessionStats: ChatViewModel.SessionStats,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Session Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) PrimaryDark else Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(
                    value = sessionStats.totalPrompts.toString(),
                    label = "Prompts",
                    isDarkTheme = isDarkTheme
                )
                QuickStatItem(
                    value = "${sessionStats.averageResponseTime.toInt()}ms",
                    label = "Avg Time",
                    isDarkTheme = isDarkTheme
                )
                QuickStatItem(
                    value = "${sessionStats.p99ResponseTime.toInt()}ms",
                    label = "P99 Time",
                    isDarkTheme = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(
                    value = "${"%.1f".format(sessionStats.tokensPerSecond)}",
                    label = "Tokens/s",
                    isDarkTheme = isDarkTheme
                )
                QuickStatItem(
                    value = "${sessionStats.successRate.toInt()}%",
                    label = "Success Rate",
                    isDarkTheme = isDarkTheme
                )
                QuickStatItem(
                    value = sessionStats.totalTokens.toString(),
                    label = "Total Tokens",
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun QuickStatItem(
    value: String,
    label: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModelSelectionScreen(
    onModelSelected: (SelectedModel) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val availableModels = remember {
        listOf(
            SelectedModel(
                id = "lfm2",
                name = "LFM2 1.2B",
                description = "Optimized for conversations and general tasks",
                icon = Icons.Default.Psychology,
                size = "~800MB"
            ),
            SelectedModel(
                id = "phi4",
                name = "Phi-4 Mini Instruct",
                description = "Microsoft's efficient instruction-following model",
                icon = Icons.Default.SmartToy,
                size = "~2.3GB"
            ),
            SelectedModel(
                id = "qwen",
                name = "Qwen 1.5 1.8B",
                description = "Alibaba's multilingual and reasoning model",
                icon = Icons.Default.Memory,
                size = "~1.1GB"
            ),
            SelectedModel(
                id = "deepseek",
                name = "DeepSeek-R1 Distill 1.5B",
                description = "Advanced reasoning model with step-by-step thinking",
                icon = Icons.Default.AutoAwesome,
                size = "~950MB"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) BackgroundDark else Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header
            Text(
                text = "Choose Your AI Model",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select the AI model that best fits your needs",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Model Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(availableModels) { model ->
                    ModelCard(
                        model = model,
                        isDarkTheme = isDarkTheme,
                        onClick = { onModelSelected(model) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (isDarkTheme) PrimaryDark else Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "All models run completely offline on your device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkTheme) PrimaryDark else Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: SelectedModel,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = model.icon,
                        contentDescription = model.name,
                        tint = if (isDarkTheme) PrimaryDark else Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) OnSurfaceDark else OnSurface
                    )

                    Text(
                        text = model.size,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) SecondaryDark else Secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Select",
                    tint = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.8f) else OnSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureChip(
                    text = "Offline",
                    isDarkTheme = isDarkTheme
                )
                FeatureChip(
                    text = "Fast",
                    isDarkTheme = isDarkTheme
                )
                FeatureChip(
                    text = "Private",
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun FeatureChip(
    text: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkTheme) PrimaryDark else Primary,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data class for selected model
data class SelectedModel(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val size: String
)

@Composable
private fun SidebarContent(
    isDarkTheme: Boolean,
    chatSessions: List<ChatSession>,
    currentSessionId: String?,
    currentModel: String?,
    onNewChat: () -> Unit,
    onChatSelect: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onChangeModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isDarkTheme) SurfaceDark else Surface)
            .padding(16.dp)
    ) {
        // Current Model Info
        currentModel?.let { model ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getModelIcon(model),
                            contentDescription = "Current Model",
                            tint = if (isDarkTheme) PrimaryDark else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Model",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getModelDisplayName(model),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) PrimaryDark else Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        // Change Model Button
        Button(
            onClick = onChangeModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) SecondaryDark else Secondary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Change Model",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Change Model",
                fontWeight = FontWeight.Medium,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) PrimaryDark else Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Chat",
                fontWeight = FontWeight.Medium,
                color = Color.White,
                fontSize = 14.sp
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
private fun ModelInitializationCard(
    isDarkTheme: Boolean,
    currentModel: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentModel?.let { model ->
                    Icon(
                        imageVector = getModelIcon(model),
                        contentDescription = "Model Icon",
                        tint = if (isDarkTheme) PrimaryDark else Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = if (isDarkTheme) PrimaryDark else Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Initializing ${currentModel?.let { getModelDisplayName(it) } ?: "AI Model"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Loading model and preparing for conversation...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) PrimaryDark else Primary,
                trackColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun WelcomeMessage(
    isDarkTheme: Boolean,
    currentModel: String?,
    modifier: Modifier = Modifier
) {
    val keywords = listOf("Privacy", "Offline", "No Internet", "Secure", "Fast", "Local", "Made in India")
    var currentKeywordIndex by remember { mutableStateOf(0) }
    var displayText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val currentKeyword = keywords[currentKeywordIndex]

            for (i in 0..currentKeyword.length) {
                displayText = currentKeyword.take(i)
                delay(100)
            }

            delay(2000)

            for (i in currentKeyword.length downTo 0) {
                displayText = currentKeyword.take(i)
                delay(50)
            }

            delay(300)
            currentKeywordIndex = (currentKeywordIndex + 1) % keywords.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        currentModel?.let { model ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getModelIcon(model),
                        contentDescription = "Model",
                        tint = if (isDarkTheme) PrimaryDark else Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Powered by ${getModelDisplayName(model)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) PrimaryDark else Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

// Helper functions for model information
private fun getModelDisplayName(modelId: String): String {
    return when {
        modelId.contains("lfm2") -> "LFM2 1.2B"
        modelId.contains("phi4") -> "Phi-4 Mini Instruct"
        modelId.contains("qwen") -> "Qwen 1.5 1.8B"
        modelId.contains("deepseek") -> "DeepSeek-R1 Distill 1.5B"
        else -> "AI Model"
    }
}

private fun getModelIcon(modelId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        modelId.contains("lfm2") -> Icons.Default.Psychology
        modelId.contains("phi4") -> Icons.Default.SmartToy
        modelId.contains("qwen") -> Icons.Default.Memory
        modelId.contains("deepseek") -> Icons.Default.AutoAwesome
        else -> Icons.Default.Psychology
    }
}