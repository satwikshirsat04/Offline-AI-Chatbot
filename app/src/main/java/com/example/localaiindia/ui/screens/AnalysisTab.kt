package com.example.localaiindia.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localaiindia.ui.theme.*
import com.example.localaiindia.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
// Import for ExportOptionsCard from ModelComparisonTab.kt
import com.example.localaiindia.ui.screens.ExportOptionsCard

@Composable
fun AnalysisTab(
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    sessionStats: ChatViewModel.SessionStats?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Analysis Card
        item {
            PerformanceAnalysisCard(
                isDarkTheme = isDarkTheme,
                sessionStats = sessionStats,
                responseTimeHistory = responseTimeHistory
            )
        }

        // Response Time Distribution
        item {
            ResponseTimeDistributionCard(
                isDarkTheme = isDarkTheme,
                responseTimeHistory = responseTimeHistory
            )
        }

        // Export Options
        item {
            ExportOptionsCard(
                isDarkTheme = isDarkTheme,
                onExportCsv = {
                    scope.launch {
                        exportResponseTimesToCsv(context, chatViewModel)
                    }
                },
                onExportReport = {
                    scope.launch {
                        exportPerformanceReport(context, chatViewModel)
                    }
                },
                onClearData = {
                    chatViewModel.clearResponseTimeHistory()
                },
                hasData = responseTimeHistory.isNotEmpty()
            )
        }

        // Recommendations Card
        if (sessionStats != null) {
            item {
                PerformanceRecommendationsCard(
                    isDarkTheme = isDarkTheme,
                    sessionStats = sessionStats
                )
            }
        }
    }
}

@Composable
fun PerformanceAnalysisCard(
    isDarkTheme: Boolean,
    sessionStats: ChatViewModel.SessionStats?,
    responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analysis",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sessionStats != null && responseTimeHistory.isNotEmpty()) {
                // Performance insights
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnalysisInsight(
                        title = "Response Consistency",
                        value = calculateConsistencyScore(responseTimeHistory),
                        description = "How consistent your response times are",
                        isDarkTheme = isDarkTheme
                    )

                    AnalysisInsight(
                        title = "Performance Trend",
                        value = calculateTrendScore(responseTimeHistory),
                        description = "Whether performance is improving over time",
                        isDarkTheme = isDarkTheme
                    )

                    AnalysisInsight(
                        title = "Reliability Score",
                        value = "${(sessionStats.successRate).toInt()}%",
                        description = "Success rate of responses",
                        isDarkTheme = isDarkTheme
                    )

                    AnalysisInsight(
                        title = "Efficiency Rating",
                        value = calculateEfficiencyRating(sessionStats),
                        description = "Overall model efficiency",
                        isDarkTheme = isDarkTheme
                    )
                }
            } else {
                Text(
                    text = "Start chatting to generate performance analysis insights",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(
                        alpha = 0.7f
                    ),
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
fun AnalysisInsight(
    title: String,
    value: String,
    description: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(alpha = 0.7f)
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) PrimaryDark else Primary
        )
    }
}

@Composable
fun ResponseTimeDistributionCard(
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
                    contentDescription = "Distribution",
                    tint = if (isDarkTheme) SecondaryDark else Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Response Time Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (responseTimeHistory.isNotEmpty()) {
                val distribution = calculateResponseTimeDistribution(responseTimeHistory)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distribution.forEach { (range, percentage) ->
                        DistributionBar(
                            range = range,
                            percentage = percentage,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            } else {
                Text(
                    text = "No response time data available",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.7f) else OnSurface.copy(
                        alpha = 0.7f
                    ),
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
fun DistributionBar(
    range: String,
    percentage: Float,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = range,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) PrimaryDark else Primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (isDarkTheme) PrimaryDark else Primary,
            trackColor = if (isDarkTheme) PrimaryDark.copy(alpha = 0.2f) else Primary.copy(alpha = 0.2f),
        )
    }
}

@Composable
fun PerformanceRecommendationsCard(
    isDarkTheme: Boolean,
    sessionStats: ChatViewModel.SessionStats,
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
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Recommendations",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Recommendations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            } // This Row was not closed properly

                Spacer(modifier = Modifier.height(16.dp))

                val recommendations = generateRecommendations(sessionStats)

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recommendations.forEach { recommendation ->
                        RecommendationItem(
                            recommendation = recommendation,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }

@Composable
fun RecommendationItem(
    recommendation: PerformanceRecommendation,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = recommendation.icon,
            contentDescription = "Recommendation",
            tint = recommendation.color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.8f) else OnSurface.copy(
                    alpha = 0.8f
                )
            )
        }
    }
}

// Helper functions and data classes
data class PerformanceRecommendation(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// Analysis helper functions
fun calculateConsistencyScore(responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>): String {
    if (responseTimeHistory.size < 2) return "N/A"

    val times = responseTimeHistory.map { it.responseTime }
    val mean = times.average()
    val variance = times.map { (it - mean) * (it - mean) }.average()
    val standardDeviation = kotlin.math.sqrt(variance)
    val coefficientOfVariation = standardDeviation / mean

    return when {
        coefficientOfVariation < 0.2 -> "Excellent"
        coefficientOfVariation < 0.4 -> "Good"
        coefficientOfVariation < 0.6 -> "Fair"
        else -> "Needs Improvement"
    }
}

fun calculateTrendScore(responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>): String {
    if (responseTimeHistory.size < 5) return "N/A"

    val recentAverage = responseTimeHistory.takeLast(5).map { it.responseTime }.average()
    val overallAverage = responseTimeHistory.map { it.responseTime }.average()

    return when {
        recentAverage < overallAverage * 0.9 -> "Improving"
        recentAverage < overallAverage * 1.1 -> "Stable"
        else -> "Declining"
    }
}

fun calculateEfficiencyRating(sessionStats: ChatViewModel.SessionStats): String {
    val score = when {
        sessionStats.averageResponseTime < 1000 && sessionStats.tokensPerSecond > 10 -> 5
        sessionStats.averageResponseTime < 2000 && sessionStats.tokensPerSecond > 5 -> 4
        sessionStats.averageResponseTime < 3000 && sessionStats.tokensPerSecond > 2 -> 3
        sessionStats.averageResponseTime < 5000 -> 2
        else -> 1
    }

    return when (score) {
        5 -> "Excellent"
        4 -> "Very Good"
        3 -> "Good"
        2 -> "Fair"
        else -> "Poor"
    }
}

fun calculateResponseTimeDistribution(responseTimeHistory: List<ChatViewModel.ResponseTimeEntry>): List<Pair<String, Float>> {
    val times = responseTimeHistory.map { it.responseTime }
    val total = times.size.toFloat()

    return listOf(
        "< 1s" to (times.count { it < 1000 } / total * 100),
        "1-2s" to (times.count { it >= 1000 && it < 2000 } / total * 100),
        "2-3s" to (times.count { it >= 2000 && it < 3000 } / total * 100),
        "3-5s" to (times.count { it >= 3000 && it < 5000 } / total * 100),
        "> 5s" to (times.count { it >= 5000 } / total * 100)
    )
}

fun generateRecommendations(sessionStats: ChatViewModel.SessionStats): List<PerformanceRecommendation> {
    val recommendations = mutableListOf<PerformanceRecommendation>()

    // Response time recommendations
    if (sessionStats.averageResponseTime > 3000) {
        recommendations.add(
            PerformanceRecommendation(
                title = "Optimize Response Time",
                description = "Your average response time is above 3 seconds. Consider using a smaller model or reducing context length for faster responses.",
                icon = Icons.Default.Speed,
                color = Color(0xFFFF9800) // Orange
            )
        )
    }

    // P99 latency recommendations
    if (sessionStats.p99ResponseTime > sessionStats.averageResponseTime * 2) {
        recommendations.add(
            PerformanceRecommendation(
                title = "Address Latency Spikes",
                description = "Your P99 latency is significantly higher than average. Some responses take much longer - consider monitoring for resource constraints.",
                icon = Icons.Default.TrendingUp,
                color = Color.Red
            )
        )
    }

    // Success rate recommendations
    if (sessionStats.successRate < 95) {
        recommendations.add(
            PerformanceRecommendation(
                title = "Improve Reliability",
                description = "Some requests are failing. Check for memory issues or consider reducing batch sizes for better stability.",
                icon = Icons.Default.Warning,
                color = Color.Red
            )
        )
    }

    // Token throughput recommendations
    if (sessionStats.tokensPerSecond < 5) {
        recommendations.add(
            PerformanceRecommendation(
                title = "Increase Throughput",
                description = "Token generation is slow. Consider optimizing model parameters or checking device resources.",
                icon = Icons.Default.FlashOn,
                color = Color(0xFFFF9800) // Orange
            )
        )
    }

    // Positive recommendations
    if (sessionStats.averageResponseTime < 2000 && sessionStats.successRate > 95) {
        recommendations.add(
            PerformanceRecommendation(
                title = "Excellent Performance",
                description = "Your model is performing well with fast response times and high reliability. Great job!",
                icon = Icons.Default.CheckCircle,
                color = Color.Green
            )
        )
    }

    return recommendations
}

// Export functions
private suspend fun exportResponseTimesToCsv(context: Context, chatViewModel: ChatViewModel) {
    try {
        val csvContent = chatViewModel.exportResponseTimes()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "AI Model Response Times - ${System.currentTimeMillis()}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV Data"))
    } catch (e: Exception) {
        android.util.Log.e("ExportCsv", "Error exporting CSV", e)
    }
}

private suspend fun exportPerformanceReport(context: Context, chatViewModel: ChatViewModel) {
    try {
        val report = chatViewModel.getPerformanceReport()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "AI Model Performance Report - ${System.currentTimeMillis()}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Export Performance Report"))
    } catch (e: Exception) {
        android.util.Log.e("ExportReport", "Error exporting report", e)
    }
}

// TODO: Remove duplicate Composable functions and helper functions if they exist in other files.
// TODO: Ensure all necessary imports are present, especially for Color, Icons, etc.
// TODO: Verify that all data classes (like PerformanceRecommendation) are correctly defined and used.
// TODO: Check that ChatViewModel.SessionStats properties (averageResponseTime, p99ResponseTime, etc.) are correctly accessed.
// TODO: Make sure CoroutineScope is an appropriate receiver for export functions, or adjust as needed.
