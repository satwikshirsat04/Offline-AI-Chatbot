package com.example.localaiindia.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localaiindia.benchmark.BenchmarkService
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
import com.example.localaiindia.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max


// Data class for performance insights

data class PerformanceInsight(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BenchmarkScreen(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val benchmarkService = remember { BenchmarkService(context) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Live Metrics", "Benchmark Tests", "Model Comparison", "Analysis")
    val pagerState = rememberPagerState { tabs.size }

    // State for benchmark data - using correct types from benchmark package
    var benchmarkRuns by remember { mutableStateOf(emptyList<com.example.localaiindia.benchmark.BenchmarkRun>()) }
    var modelComparisons by remember { mutableStateOf(emptyList<com.example.localaiindia.benchmark.ModelComparison>()) }
    val benchmarkProgress by benchmarkService.benchmarkProgress.collectAsState()
    val sessionStats by chatViewModel.sessionStats.collectAsState()
    val responseTimeHistory by chatViewModel.responseTimeHistory.collectAsState()

    // Load benchmark data
    LaunchedEffect(Unit) {
        benchmarkService.getBenchmarkRuns().collect { runs ->
            benchmarkRuns = runs
        }
    }

    LaunchedEffect(Unit) {
        modelComparisons = benchmarkService.getModelComparisons()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) BackgroundDark else Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Performance Benchmark",
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkTheme) OnSurfaceDark else OnSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
            contentColor = if (isDarkTheme) Color.White else Color.Black,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    height = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Tab Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) {
            when (selectedTab) {
                0 -> LiveMetricsTab(
                    isDarkTheme = isDarkTheme,
                    sessionStats = sessionStats,
                    responseTimeHistory = responseTimeHistory,
                    benchmarkProgress = benchmarkProgress
                )
                1 -> BenchmarkTestsTab(
                    isDarkTheme = isDarkTheme,
                    chatViewModel = chatViewModel,
                    benchmarkService = benchmarkService,
                    benchmarkRuns = benchmarkRuns,
                    benchmarkProgress = benchmarkProgress,
                    onStartBenchmark = {
                            promptCount ->
                        scope.launch {
                            benchmarkService.startBenchmark(
                                modelId = "current_model",
                                modelName = "Current Model",
                                llamaService = chatViewModel.llamaService,
                                promptCount = promptCount
                            )
                        }
                    },
                    onCancelBenchmark = {
                        benchmarkService.cancelBenchmark()
                    }
                )
                2 -> ModelComparisonTab(
                    isDarkTheme = isDarkTheme,
                    modelComparisons = modelComparisons
                )
                3 -> AnalysisTab(
                    isDarkTheme = isDarkTheme,
                    chatViewModel = chatViewModel,
                    responseTimeHistory = responseTimeHistory,
                    sessionStats = sessionStats
                )
            }
        }
    }
}

@Composable
fun LiveMetricsTab(
    isDarkTheme: Boolean,
    sessionStats: ChatViewModel.SessionStats?,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    benchmarkProgress: BenchmarkService.BenchmarkProgress?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Session Performance
        item {
            CurrentSessionCard(
                isDarkTheme = isDarkTheme,
                sessionStats = sessionStats,
                responseTimeHistory = responseTimeHistory
            )
        }

        // Real-time Response Time Chart
        item {
            ResponseTimeChartCard(
                isDarkTheme = isDarkTheme,
                responseTimeHistory = responseTimeHistory
            )
        }

        // Performance Metrics Grid
        sessionStats?.let { stats ->
            item {
                PerformanceMetricsGrid(
                    isDarkTheme = isDarkTheme,
                    stats = stats
                )
            }
        }

        // Current Benchmark Progress (if running)
        benchmarkProgress?.let { progress ->
            item {
                BenchmarkProgressCard(
                    isDarkTheme = isDarkTheme,
                    progress = progress
                )
            }
        }
    }
}

@Composable
fun CurrentSessionCard(
    isDarkTheme: Boolean,
    sessionStats: ChatViewModel.SessionStats?,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Performance",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Current Session Performance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sessionStats != null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        MetricCard(
                            title = "Average",
                            value = "${sessionStats.averageResponseTime.toInt()}ms",
                            subtitle = "Response Time",
                            icon = Icons.Default.AccessTime,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    item {
                        MetricCard(
                            title = "P95",
                            value = "${sessionStats.p95ResponseTime.toInt()}ms",
                            subtitle = "95th Percentile",
                            icon = Icons.Default.TrendingUp,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    item {
                        MetricCard(
                            title = "P99",
                            value = "${sessionStats.p99ResponseTime.toInt()}ms",
                            subtitle = "99th Percentile",
                            icon = Icons.Default.ShowChart,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    item {
                        MetricCard(
                            title = "Min/Max",
                            value = "${sessionStats.minResponseTime}/${sessionStats.maxResponseTime}ms",
                            subtitle = "Range",
                            icon = Icons.Default.Height,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            } else {
                Text(
                    text = "No performance data available. Start chatting to see metrics!",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(width = 140.dp, height = 100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) PrimaryDark else Primary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ResponseTimeChartCard(
    isDarkTheme: Boolean,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Chart",
                    tint = if (isDarkTheme) SecondaryDark else Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Response Time Trends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (responseTimeHistory.isNotEmpty()) {
                ResponseTimeChart(
                    isDarkTheme = isDarkTheme,
                    responseTimeHistory = responseTimeHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start chatting to see response time trends",
                        color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ResponseTimeChart(
    isDarkTheme: Boolean,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDarkTheme) PrimaryDark else Primary
    val secondaryColor = if (isDarkTheme) SecondaryDark else Secondary
    val surfaceColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.1f) else OnSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        if (responseTimeHistory.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f

        val dataPoints = responseTimeHistory.map { it.responseTime.toFloat() }
        val maxValue = dataPoints.maxOrNull() ?: 0f
        val minValue = dataPoints.minOrNull() ?: 0f
        val range = max(1f, maxValue - minValue)

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (height - 2 * padding) * i / gridLines
            drawLine(
                color = surfaceColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw data points and line
        val points = dataPoints.mapIndexed { index, value ->
            val x = padding + (width - 2 * padding) * index / (dataPoints.size - 1).coerceAtLeast(1)
            val y = padding + (height - 2 * padding) * (1 - (value - minValue) / range)
            Offset(x, y)
        }

        // Draw line connecting points
        for (i in 0 until points.size - 1) {
            drawLine(
                color = primaryColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw points
        points.forEachIndexed { index, point ->
            val isSuccess = responseTimeHistory[index].success
            drawCircle(
                color = if (isSuccess) primaryColor else Color.Red,
                radius = 6.dp.toPx(),
                center = point
            )
        }

        // Draw average line
        val average = dataPoints.average().toFloat()
        val avgY = padding + (height - 2 * padding) * (1 - (average - minValue) / range)
        drawLine(
            color = secondaryColor,
            start = Offset(padding, avgY),
            end = Offset(width - padding, avgY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }
}

@Composable
fun PerformanceMetricsGrid(
    isDarkTheme: Boolean,
    stats: ChatViewModel.SessionStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Detailed Performance Metrics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of metrics
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailedMetricItem(
                        label = "Total Prompts",
                        value = stats.totalPrompts.toString(),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedMetricItem(
                        label = "Success Rate",
                        value = "${stats.successRate.toInt()}%",
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedMetricItem(
                        label = "Total Tokens",
                        value = stats.totalTokens.toString(),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailedMetricItem(
                        label = "Min Response",
                        value = "${stats.minResponseTime}ms",
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedMetricItem(
                        label = "Max Response",
                        value = "${stats.maxResponseTime}ms",
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedMetricItem(
                        label = "Tokens/Sec",
                        value = "${"%.1f".format(stats.tokensPerSecond)}",
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedMetricItem(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) PrimaryDark else Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BenchmarkProgressCard(
    isDarkTheme: Boolean,
    progress: BenchmarkService.BenchmarkProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Benchmark Running",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Benchmark in Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.currentPrompt.toFloat() / progress.totalPrompts },
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) PrimaryDark else Primary,
                trackColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress: ${progress.currentPrompt}/${progress.totalPrompts}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
                Text(
                    text = "ETA: ${formatTime(progress.estimatedTimeRemaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${progress.currentLatency.toInt()}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
                Text(
                    text = "Average: ${progress.averageLatency.toInt()}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }
        }
    }
}

@Composable
fun BenchmarkTestsTab(
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel,
    benchmarkService: BenchmarkService,
    benchmarkRuns: List<com.example.localaiindia.benchmark.BenchmarkRun>,
    benchmarkProgress: BenchmarkService.BenchmarkProgress?,
    onStartBenchmark: (Int) -> Unit,
    onCancelBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPromptCount by remember { mutableStateOf(50) }
    val promptOptions = listOf(25, 50, 100, 200)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Benchmark Controls
        item {
            BenchmarkControlsCard(
                isDarkTheme = isDarkTheme,
                benchmarkProgress = benchmarkProgress,
                selectedPromptCount = selectedPromptCount,
                promptOptions = promptOptions,
                onPromptCountChange = { selectedPromptCount = it },
                onStartBenchmark = { onStartBenchmark(selectedPromptCount) },
                onCancelBenchmark = onCancelBenchmark
            )
        }

        // Benchmark History Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) SurfaceDark else Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Benchmark History",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) OnSurfaceDark else OnSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (benchmarkRuns.isEmpty()) {
                        Text(
                            text = "No benchmark runs yet. Start your first benchmark test above!",
                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    }
                }
            }
        }

        // Benchmark run items
        items(benchmarkRuns) { run ->
            BenchmarkRunCard(
                isDarkTheme = isDarkTheme,
                benchmarkRun = run
            )
        }
    }
}

@Composable
fun BenchmarkControlsCard(
    isDarkTheme: Boolean,
    benchmarkProgress: BenchmarkService.BenchmarkProgress?,
    selectedPromptCount: Int,
    promptOptions: List<Int>,
    onPromptCountChange: (Int) -> Unit,
    onStartBenchmark: () -> Unit,
    onCancelBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Benchmark Controls",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Automated Benchmark Testing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Run comprehensive performance tests with predefined prompts",
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Prompt count selection
            Text(
                text = "Number of test prompts:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(promptOptions) { count ->
                    FilterChip(
                        selected = selectedPromptCount == count,
                        onClick = { onPromptCountChange(count) },
                        label = {
                            Text(
                                text = "$count prompts",
                                fontWeight = if (selectedPromptCount == count) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDarkTheme) PrimaryDark else Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control buttons
            if (benchmarkProgress != null) {
                // Show progress and cancel button
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Running benchmark...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) OnSurfaceDark else OnSurface
                            )
                            Text(
                                text = "${benchmarkProgress.currentPrompt}/${benchmarkProgress.totalPrompts} prompts completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = onCancelBenchmark,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { benchmarkProgress.currentPrompt.toFloat() / benchmarkProgress.totalPrompts },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isDarkTheme) PrimaryDark else Primary
                    )
                }
            } else {
                // Show start button
                Button(
                    onClick = onStartBenchmark,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) PrimaryDark else Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Benchmark",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Benchmark ($selectedPromptCount prompts)",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Estimated time: ${estimateBenchmarkTime(selectedPromptCount)}",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BenchmarkRunCard(
    isDarkTheme: Boolean,
    benchmarkRun: com.example.localaiindia.benchmark.BenchmarkRun,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = benchmarkRun.modelName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) OnSurfaceDark else OnSurface
                    )
                    Text(
                        text = formatDateTime(benchmarkRun.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                    )
                }

                StatusChip(
                    status = benchmarkRun.status,
                    isDarkTheme = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (benchmarkRun.status == com.example.localaiindia.benchmark.BenchmarkStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricText(
                        label = "Avg",
                        value = "${benchmarkRun.averageLatency.toInt()}ms",
                        isDarkTheme = isDarkTheme
                    )
                    MetricText(
                        label = "P99",
                        value = "${benchmarkRun.p99Latency.toInt()}ms",
                        isDarkTheme = isDarkTheme
                    )
                    MetricText(
                        label = "Prompts",
                        value = "${benchmarkRun.completedPrompts}/${benchmarkRun.totalPrompts}",
                        isDarkTheme = isDarkTheme
                    )
                    MetricText(
                        label = "Tokens/s",
                        value = "${"%.1f".format(benchmarkRun.tokensPerSecond)}",
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    status: com.example.localaiindia.benchmark.BenchmarkStatus,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val (chipColor, chipText) = when (status) {
        com.example.localaiindia.benchmark.BenchmarkStatus.COMPLETED -> Pair(Color.Green, "Completed")
        com.example.localaiindia.benchmark.BenchmarkStatus.RUNNING -> Pair(if (isDarkTheme) PrimaryDark else Primary, "Running")
        com.example.localaiindia.benchmark.BenchmarkStatus.FAILED -> Pair(Color.Red, "Failed")
        com.example.localaiindia.benchmark.BenchmarkStatus.CANCELLED -> Pair(Color.Gray, "Cancelled")
        com.example.localaiindia.benchmark.BenchmarkStatus.PENDING -> Pair(Color(0xFFF57C00), "Pending")
    }

    Box(
        modifier = modifier
            .background(
                color = chipColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = chipText,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MetricText(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) PrimaryDark else Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModelComparisonTab(
    isDarkTheme: Boolean,
    modelComparisons: List<com.example.localaiindia.benchmark.ModelComparison>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) SurfaceDark else Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Model Comparison",
                            tint = if (isDarkTheme) PrimaryDark else Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Model Performance Comparison",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) OnSurfaceDark else OnSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (modelComparisons.isEmpty()) {
                        Text(
                            text = "No comparison data available. Run benchmarks on multiple models to see comparisons.",
                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    } else {
                        Text(
                            text = "Compare performance metrics across different models to find the best one for your use case.",
                            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (modelComparisons.isNotEmpty()) {
            // Model comparison cards
            items(modelComparisons) { comparison ->
                ModelComparisonCard(
                    isDarkTheme = isDarkTheme,
                    comparison = comparison
                )
            }

            // Performance summary charts
            item {
                ModelComparisonCharts(
                    isDarkTheme = isDarkTheme,
                    modelComparisons = modelComparisons
                )
            }
        }
    }
}

@Composable
fun ModelComparisonCard(
    isDarkTheme: Boolean,
    comparison: com.example.localaiindia.benchmark.ModelComparison,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comparison.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )

                Text(
                    text = "${comparison.runCount} tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Performance metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonMetricItem(
                    label = "Avg Latency",
                    value = "${comparison.stats.averageLatency.toInt()}ms",
                    isDarkTheme = isDarkTheme
                )
                ComparisonMetricItem(
                    label = "P99 Latency",
                    value = "${comparison.stats.p99Latency.toInt()}ms",
                    isDarkTheme = isDarkTheme
                )
                ComparisonMetricItem(
                    label = "Tokens/s",
                    value = "${"%.1f".format(comparison.stats.tokensPerSecond)}",
                    isDarkTheme = isDarkTheme
                )
                ComparisonMetricItem(
                    label = "Success",
                    value = "${comparison.stats.successRate.toInt()}%",
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun ComparisonMetricItem(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) PrimaryDark else Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModelComparisonCharts(
    isDarkTheme: Boolean,
    modelComparisons: List<com.example.localaiindia.benchmark.ModelComparison>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Performance Comparison Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple bar chart for latency comparison
            LatencyComparisonChart(
                isDarkTheme = isDarkTheme,
                modelComparisons = modelComparisons,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Simple bar chart for tokens per second comparison
            TokensPerSecondChart(
                isDarkTheme = isDarkTheme,
                modelComparisons = modelComparisons,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun LatencyComparisonChart(
    isDarkTheme: Boolean,
    modelComparisons: List<com.example.localaiindia.benchmark.ModelComparison>,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDarkTheme) PrimaryDark else Primary
    val surfaceColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.1f) else OnSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        if (modelComparisons.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 60f
        val barWidth = (width - 2 * padding) / (modelComparisons.size * 2)

        val maxLatency = modelComparisons.maxOfOrNull { it.stats.averageLatency.toFloat() }?.times(1.1f) ?: 0f

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (height - 2 * padding) * i / gridLines
            drawLine(
                color = surfaceColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw bars for average latency
        modelComparisons.forEachIndexed { index, comparison ->
            val x = padding + index * barWidth * 2
            val barHeight = if (maxLatency > 0) (height - 2 * padding) * (comparison.stats.averageLatency.toFloat() / maxLatency) else 0f

            drawRect(
                color = primaryColor,
                topLeft = Offset(x, height - padding - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun TokensPerSecondChart(
    isDarkTheme: Boolean,
    modelComparisons: List<com.example.localaiindia.benchmark.ModelComparison>,
    modifier: Modifier = Modifier
) {
    val secondaryColor = if (isDarkTheme) SecondaryDark else Secondary
    val surfaceColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.1f) else OnSurface.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        if (modelComparisons.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 60f
        val barWidth = (width - 2 * padding) / (modelComparisons.size * 2)

        val maxTokensPerSecond = modelComparisons.maxOfOrNull { it.stats.tokensPerSecond.toFloat() }?.times(1.1f) ?: 0f

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (height - 2 * padding) * i / gridLines
            drawLine(
                color = surfaceColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw bars for tokens per second
        modelComparisons.forEachIndexed { index, comparison ->
            val x = padding + index * barWidth * 2
            val barHeight = if (maxTokensPerSecond > 0) (height - 2 * padding) * (comparison.stats.tokensPerSecond.toFloat() / maxTokensPerSecond) else 0f

            drawRect(
                color = secondaryColor,
                topLeft = Offset(x + barWidth, height - padding - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun PerformanceInsightCard(
    isDarkTheme: Boolean,
    insight: PerformanceInsight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = insight.title,
                tint = insight.color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(
    isDarkTheme: Boolean,
    sessionStats: ChatViewModel.SessionStats?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Optimization Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            val recommendations = listOf(
                "Consider using quantization to reduce model size and improve inference speed",
                "Enable GPU acceleration if available for faster processing",
                "Implement prompt caching for frequently used queries",
                "Adjust temperature settings for more consistent response times",
                "Monitor memory usage and consider model optimization techniques"
            )

            recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Recommendation",
                        tint = if (isDarkTheme) SecondaryDark else Secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.8f) else OnSurface.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BenchmarkExportOptionsCard(
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onExportCsv: () -> Unit, // Added for consistency with AnalysisTab
    onExportJson: () -> Unit, // Added for consistency with AnalysisTab
    onExportReport: () -> Unit // Added for consistency with AnalysisTab
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Export Performance Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) PrimaryDark else Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CSV")
                }

                Button(
                    onClick = onExportJson,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) PrimaryDark else Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Export JSON",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JSON")
                }

                Button(
                    onClick = onExportReport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) PrimaryDark else Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report")
                }
            }
        }
    }
}

// Helper functions
private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${seconds}s"
    }
}

private fun formatDateTime(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
}

private fun estimateBenchmarkTime(promptCount: Int): String {
    // Assuming ~2 seconds per prompt on average
    val totalSeconds = promptCount * 2
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

// Placeholder export functions - implement these in ChatViewModel or pass from AnalysisTab if already defined
private suspend fun exportDataAsCsv(context: Context, chatViewModel: ChatViewModel) {
    // TODO: Implement CSV export logic or call existing function from ChatViewModel
    // For example: chatViewModel.exportResponseTimesToCsv(context)
    // Ensure this function is defined in ChatViewModel or a shared utility class
}

private suspend fun exportDataAsJson(context: Context, chatViewModel: ChatViewModel) {
    // TODO: Implement JSON export logic or call existing function from ChatViewModel
}

private suspend fun exportDataAsReport(context: Context, chatViewModel: ChatViewModel) {
    // TODO: Implement report export logic or call existing function from ChatViewModel
    // For example: chatViewModel.exportPerformanceReport(context)
    // Ensure this function is defined in ChatViewModel or a shared utility class
}
