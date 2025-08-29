package com.example.localaiindia.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint // Import Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas // Import drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localaiindia.benchmark.BenchmarkRun
import com.example.localaiindia.benchmark.ModelComparison
import com.example.localaiindia.ui.theme.*
// import kotlinx.coroutines.launch // Unused import
import android.util.Log

@Composable
fun ModelComparisonTab(
    isDarkTheme: Boolean,
    benchmarkRuns: List<BenchmarkRun>,
    modelComparisons: List<ModelComparison>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // val scope = rememberCoroutineScope() // Unused variable

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model Performance Comparison Card
        item {
            ModelPerformanceCard(
                isDarkTheme = isDarkTheme,
                modelComparisons = modelComparisons
            )
        }

        // Benchmark Results Table
        if (benchmarkRuns.isNotEmpty()) {
            item {
                BenchmarkResultsCard(
                    isDarkTheme = isDarkTheme,
                    benchmarkRuns = benchmarkRuns
                )
            }
        }

        // Performance Chart
        if (modelComparisons.isNotEmpty()) {
            item {
                PerformanceChartCard(
                    isDarkTheme = isDarkTheme,
                    modelComparisons = modelComparisons
                )
            }
        }

        // Export Options
        item {
            ExportOptionsCard(
                isDarkTheme = isDarkTheme,
                onExportCsv = {
                    exportComparisonData(context, modelComparisons, benchmarkRuns)
                },
                onExportReport = {
                    exportComparisonReport(context, modelComparisons, benchmarkRuns)
                },
                onClearData = { /* Handle clear data if needed */ },
                hasData = modelComparisons.isNotEmpty() || benchmarkRuns.isNotEmpty()
            )
        }
    }
}

@Composable
fun ModelPerformanceCard(
    isDarkTheme: Boolean,
    modelComparisons: List<ModelComparison>,
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
                    imageVector = Icons.Default.BarChart,
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

            if (modelComparisons.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    modelComparisons.forEach { comparison ->
                        ModelComparisonItem(
                            comparison = comparison,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            } else {
                Text(
                    text = "No model comparisons available. Run benchmarks to compare different models.",
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
fun ModelComparisonItem(
    comparison: ModelComparison,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) SurfaceDark.copy(alpha = 0.5f) else Surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = comparison.modelName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) OnSurfaceDark else OnSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Avg Latency",
                    value = "${comparison.stats.averageLatency.toInt()}ms",
                    isDarkTheme = isDarkTheme
                )
                MetricItem(
                    label = "P99 Latency",
                    value = "${comparison.stats.p99Latency.toInt()}ms",
                    isDarkTheme = isDarkTheme
                )
                MetricItem(
                    label = "Success Rate",
                    value = "${comparison.stats.successRate.toInt()}%",
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun MetricItem(
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
            style = MaterialTheme.typography.titleMedium,
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
fun BenchmarkResultsCard(
    isDarkTheme: Boolean,
    benchmarkRuns: List<BenchmarkRun>,
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
                    imageVector = Icons.Default.TableChart,
                    contentDescription = "Benchmark Results",
                    tint = if (isDarkTheme) SecondaryDark else Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Benchmark Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Model",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Avg Time",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Tokens/s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.2f) else OnSurface.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Table Rows
            benchmarkRuns.forEach { run ->
                BenchmarkResultRow(
                    benchmarkRun = run,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BenchmarkResultRow(
    benchmarkRun: BenchmarkRun,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getModelDisplayName(benchmarkRun.modelId),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${benchmarkRun.averageLatency.toInt()}ms",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = "%.1f".format(benchmarkRun.tokensPerSecond),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) OnSurfaceDark else OnSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun PerformanceChartCard(
    isDarkTheme: Boolean,
    modelComparisons: List<ModelComparison>,
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
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = "Performance Chart",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Visualization",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simple bar chart visualization
            PerformanceChart(
                modelComparisons = modelComparisons,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.height(200.dp)
            )
        }
    }
}

@Composable
fun PerformanceChart(
    modelComparisons: List<ModelComparison>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val chartColors = listOf(
        if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2E7D32), // Green
        if (isDarkTheme) Color(0xFF2196F3) else Color(0xFF1565C0), // Blue
        if (isDarkTheme) Color(0xFF9C27B0) else Color(0xFF7B1FA2), // Purple
        if (isDarkTheme) Color(0xFFFF5722) else Color(0xFFD84315), // Orange-Red
        if (isDarkTheme) Color(0xFFFFEB3B) else Color(0xFFF57F17)  // Yellow
    )

    Canvas(
        modifier = modifier.fillMaxWidth()
    ) {
        if (modelComparisons.isEmpty()) return@Canvas

        val chartWidth = size.width - 120f // Leave space for labels
        val chartHeight = size.height - 80f
        val barHeight = chartHeight / modelComparisons.size
        val maxLatency = modelComparisons.maxOfOrNull { it.stats.averageLatency } ?: 1000.0

        modelComparisons.forEachIndexed { index, comparison ->
            val barWidth = (comparison.stats.averageLatency / maxLatency * chartWidth).toFloat()
            val y = index * barHeight + 40f
            val color = chartColors[index % chartColors.size]

            // Draw bar
            drawRoundRect(
                color = color,
                topLeft = Offset(100f, y),
                size = Size(barWidth, barHeight * 0.6f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw model name
            drawIntoCanvas {
                val paint = Paint().apply {
                    this.color = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                    textSize = 32f
                    isAntiAlias = true
                }
                it.nativeCanvas.drawText(
                    getModelDisplayName(comparison.modelName).take(10), // Truncate for display
                    10f,
                    y + barHeight * 0.4f,
                    paint
                )

                it.nativeCanvas.drawText(
                    "${comparison.stats.averageLatency.toInt()}ms",
                    barWidth + 110f,
                    y + barHeight * 0.4f,
                    paint
                )
            }
        }
    }
}

@Composable
fun ExportOptionsCard(
    isDarkTheme: Boolean,
    onExportCsv: () -> Unit,
    onExportReport: () -> Unit,
    onClearData: () -> Unit,
    hasData: Boolean,
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
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Export",
                    tint = if (isDarkTheme) PrimaryDark else Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Export & Data Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) OnSurfaceDark else OnSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onExportCsv,
                    enabled = hasData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) PrimaryDark else Primary,
                        disabledContainerColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.2f) else OnSurface.copy(
                            alpha = 0.2f
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CSV Data", fontSize = 14.sp)
                }

                Button(
                    onClick = onExportReport,
                    enabled = hasData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) SecondaryDark else Secondary,
                        disabledContainerColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.2f) else OnSurface.copy(
                            alpha = 0.2f
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clear data button
            OutlinedButton(
                onClick = onClearData,
                enabled = hasData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Data",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Performance Data", fontSize = 14.sp)
            }

            if (!hasData) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No data available for export",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(
                        alpha = 0.6f
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Helper functions
private fun getModelDisplayName(modelId: String): String {
    return when {
        modelId.contains("lfm2") -> "LFM2 1.2B"
        modelId.contains("phi4") -> "Phi-4 Mini"
        modelId.contains("qwen") -> "Qwen 1.5 1.8B"
        modelId.contains("deepseek") -> "DeepSeek-R1"
        else -> modelId.take(15) // Truncate long model names
    }
}

private fun exportComparisonData( // Removed suspend
    context: Context,
    modelComparisons: List<ModelComparison>,
    benchmarkRuns: List<BenchmarkRun>
) {
    try {
        val csvContent = buildString {
            appendLine("Model,Average Latency (ms),P99 Latency (ms),Success Rate (%),Tokens/s")

            modelComparisons.forEach { comparison ->
                appendLine("${comparison.modelName},${comparison.stats.averageLatency.toInt()},${comparison.stats.p99Latency.toInt()},${comparison.stats.successRate.toInt()},${comparison.stats.tokensPerSecond}")
            }

            benchmarkRuns.forEach { run ->
                val successRate = if (run.totalPrompts > 0) (run.completedPrompts.toDouble() / run.totalPrompts * 100) else 0.0
                appendLine("${run.modelId},${run.averageLatency.toInt()},${run.p99Latency.toInt()},${successRate.toInt()},${run.tokensPerSecond}")
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Model Comparison Report - ${System.currentTimeMillis()}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Export Model Comparison"))
    } catch (e: Exception) {
        Log.e("ExportComparison", "Error exporting comparison data", e)
    }
}

private fun exportComparisonReport( // Removed suspend
    context: Context,
    modelComparisons: List<ModelComparison>,
    benchmarkRuns: List<BenchmarkRun>
) {
    try {
        val report = buildString {
            appendLine("Model Performance Comparison Report")
            appendLine("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("=".repeat(50))
            appendLine()

            appendLine("Model Comparisons:")
            appendLine("-".repeat(30))
            modelComparisons.forEach { comparison ->
                appendLine("${comparison.modelName}:")
                appendLine("  • Average Latency: ${comparison.stats.averageLatency.toInt()}ms")
                appendLine("  • P99 Latency: ${comparison.stats.p99Latency.toInt()}ms")
                appendLine("  • Success Rate: ${comparison.stats.successRate.toInt()}%")
                appendLine("  • Tokens/Second: ${"%.1f".format(comparison.stats.tokensPerSecond)}")
                appendLine()
            }

            appendLine("Benchmark Runs:")
            appendLine("-".repeat(30))
            benchmarkRuns.forEach { run ->
                val successRate = if (run.totalPrompts > 0) (run.completedPrompts.toDouble() / run.totalPrompts * 100) else 0.0
                appendLine("${run.modelId}:")
                appendLine("  • Average Latency: ${run.averageLatency.toInt()}ms")
                appendLine("  • P99 Latency: ${run.p99Latency.toInt()}ms")
                appendLine("  • Success Rate: ${successRate.toInt()}%")
                appendLine("  • Tokens/Second: ${"%.1f".format(run.tokensPerSecond)}")
                appendLine("  • Completed: ${run.completedPrompts}/${run.totalPrompts}")
                appendLine()
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Model Performance Report - ${System.currentTimeMillis()}"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Export Performance Report"))
    } catch (e: Exception) {
        Log.e("ExportReport", "Error exporting report", e)
    }
}
