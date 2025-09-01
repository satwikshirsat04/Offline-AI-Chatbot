package com.example.localaiindia.benchmark

import android.content.Context
import android.util.Log
import com.example.localaiindia.LlamaService
import com.example.localaiindia.benchmark.database.BenchmarkDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BenchmarkService(private val context: Context) {
    private val database = BenchmarkDatabase.getDatabase(context)
    private val benchmarkDao = database.benchmarkDao()
    private var currentJob: Job? = null

    companion object {
        private const val TAG = "BenchmarkService"

        // Predefined benchmark prompts with varying complexity
        val BENCHMARK_PROMPTS = listOf(
            "What is artificial intelligence?",
            "Explain quantum computing in simple terms.",
            "Write a short story about a robot learning emotions.",
            "If a pen costs ₹15, how many pens can you buy with ₹120?",
            "A is taller than B, B is taller than C; who is the shortest among A, B, and C?"
        )
    }

    private val _benchmarkProgress = MutableStateFlow<BenchmarkProgress?>(null)
    val benchmarkProgress: StateFlow<BenchmarkProgress?> = _benchmarkProgress.asStateFlow()

    data class BenchmarkProgress(
        val runId: String,
        val modelId: String,
        val currentPrompt: Int,
        val totalPrompts: Int,
        val currentLatency: Double,
        val averageLatency: Double,
        val estimatedTimeRemaining: Long,
        val isCompleted: Boolean = false,
        val error: String? = null
    )



    // inside class BenchmarkService

// Add this helper function
fun loadPromptsFromAssets(fileName: String): List<String> {
    return try {
        context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.map { line ->
                var l = line.trim()
                // remove trailing commas / surrounding quotes / stray plus signs
                if (l.startsWith("\"")) l = l.removePrefix("\"")
                while (l.endsWith(",") || l.endsWith("+") || l.endsWith("\"")) {
                    l = l.dropLast(1)
                }
                l.trim()
            }.filter { it.isNotBlank() }.toList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load prompts from assets/$fileName: ${e.message}")
        emptyList()
    }
}

// Replace the original startBenchmark with this updated version
suspend fun startBenchmark(
    modelId: String,
    modelName: String,
    llamaService: LlamaService,
    promptCount: Int = 100,
    promptFileName: String? = null
): String {
    // Cancel any existing benchmark
    currentJob?.cancel()

    // Load prompts (prefer asset file if provided)
    val prompts: List<String> = if (!promptFileName.isNullOrBlank()) {
        val filePrompts = loadPromptsFromAssets(promptFileName)
        if (filePrompts.isNotEmpty()) {
            if (promptCount <= filePrompts.size) filePrompts.take(promptCount)
            else List(promptCount) { filePrompts[it % filePrompts.size] }
        } else {
            // fallback to builtin
            if (promptCount <= BENCHMARK_PROMPTS.size) BENCHMARK_PROMPTS.take(promptCount)
            else List(promptCount) { BENCHMARK_PROMPTS[it % BENCHMARK_PROMPTS.size] }
        }
    } else {
        if (promptCount <= BENCHMARK_PROMPTS.size) BENCHMARK_PROMPTS.take(promptCount)
        else List(promptCount) { BENCHMARK_PROMPTS[it % BENCHMARK_PROMPTS.size] }
    }

    val benchmarkRun = BenchmarkRun(
        modelId = modelId,
        modelName = modelName,
        startTime = System.currentTimeMillis(),
        totalPrompts = prompts.size,
        status = BenchmarkStatus.RUNNING
    )

    benchmarkDao.insertBenchmarkRun(benchmarkRun)

    currentJob = CoroutineScope(Dispatchers.IO).launch {
        try {
            runBenchmark(benchmarkRun, prompts, llamaService)
        } catch (e: CancellationException) {
            Log.d(TAG, "Benchmark cancelled")
            benchmarkDao.updateBenchmarkRun(
                benchmarkRun.copy(
                    status = BenchmarkStatus.CANCELLED,
                    endTime = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Benchmark failed", e)
            benchmarkDao.updateBenchmarkRun(
                benchmarkRun.copy(
                    status = BenchmarkStatus.FAILED,
                    endTime = System.currentTimeMillis()
                )
            )
            _benchmarkProgress.value = _benchmarkProgress.value?.copy(
                error = e.message,
                isCompleted = true
            )
        }
    }

    return benchmarkRun.id
}


    private suspend fun runBenchmark(
        benchmarkRun: BenchmarkRun,
        prompts: List<String>,
        llamaService: LlamaService
    ) {
        val results = mutableListOf<BenchmarkResult>()
        val latencies = mutableListOf<Long>()

        for ((index, prompt) in prompts.withIndex()) {
            currentCoroutineContext().ensureActive() // Check for cancellation

            val startTime = System.currentTimeMillis()

            try {
                Log.d(TAG, "Running benchmark prompt ${index + 1}/${prompts.size}: ${prompt.take(50)}...")

                val response = llamaService.chat(prompt)
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                latencies.add(responseTime)

                val result = BenchmarkResult(
                    benchmarkRunId = benchmarkRun.id,
                    promptIndex = index,
                    prompt = prompt,
                    response = response,
                    startTime = startTime,
                    endTime = endTime,
                    responseTimeMs = responseTime,
                    tokenCount = estimateTokenCount(response),
                    promptTokens = estimateTokenCount(prompt),
                    responseTokens = estimateTokenCount(response),
                    contextLength = estimateTokenCount(prompt) + estimateTokenCount(response),
                    success = true
                )

                benchmarkDao.insertBenchmarkResult(result)
                results.add(result)

                // Update progress
                val currentAverage = latencies.average()
                val estimatedRemaining = calculateEstimatedTime(
                    latencies,
                    prompts.size - (index + 1)
                )

                _benchmarkProgress.value = BenchmarkProgress(
                    runId = benchmarkRun.id,
                    modelId = benchmarkRun.modelId,
                    currentPrompt = index + 1,
                    totalPrompts = prompts.size,
                    currentLatency = responseTime.toDouble(),
                    averageLatency = currentAverage,
                    estimatedTimeRemaining = estimatedRemaining
                )

                // Update benchmark run progress
                benchmarkDao.updateBenchmarkRun(
                    benchmarkRun.copy(
                        completedPrompts = index + 1,
                        averageLatency = currentAverage
                    )
                )

                Log.d(TAG, "Completed prompt ${index + 1}/${prompts.size} in ${responseTime}ms")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing prompt ${index + 1}", e)

                val result = BenchmarkResult(
                    benchmarkRunId = benchmarkRun.id,
                    promptIndex = index,
                    prompt = prompt,
                    response = "",
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    responseTimeMs = System.currentTimeMillis() - startTime,
                    success = false,
                    errorMessage = e.message
                )

                benchmarkDao.insertBenchmarkResult(result)
                results.add(result)
            }

            // Small delay to prevent overwhelming the model
            delay(500)
        }

        // Calculate final statistics
        val stats = calculateBenchmarkStats(results)
        val endTime = System.currentTimeMillis()

        // Update final benchmark run
        val finalBenchmarkRun = benchmarkRun.copy(
            status = BenchmarkStatus.COMPLETED,
            endTime = endTime,
            completedPrompts = results.size,
            averageLatency = stats.averageLatency,
            p99Latency = stats.p99Latency,
            minLatency = stats.minLatency,
            maxLatency = stats.maxLatency,
            tokensPerSecond = stats.tokensPerSecond
        )

        benchmarkDao.updateBenchmarkRun(finalBenchmarkRun)

        // Update final progress
        _benchmarkProgress.value = BenchmarkProgress(
            runId = benchmarkRun.id,
            modelId = benchmarkRun.modelId,
            currentPrompt = prompts.size,
            totalPrompts = prompts.size,
            currentLatency = stats.averageLatency,
            averageLatency = stats.averageLatency,
            estimatedTimeRemaining = 0,
            isCompleted = true
        )

        Log.i(TAG, "Benchmark completed for ${benchmarkRun.modelName}: " +
                "Avg: ${stats.averageLatency}ms, P99: ${stats.p99Latency}ms, " +
                "Success rate: ${stats.successRate}%")
    }

    private fun calculateBenchmarkStats(results: List<BenchmarkResult>): BenchmarkStats {
        val successfulResults = results.filter { it.success }
        val latencies = successfulResults.map { it.responseTimeMs.toDouble() }.sorted()

        if (latencies.isEmpty()) {
            return BenchmarkStats(
                totalPrompts = results.size,
                completedPrompts = 0,
                averageLatency = 0.0,
                p50Latency = 0.0,
                p90Latency = 0.0,
                p95Latency = 0.0,
                p99Latency = 0.0,
                minLatency = 0.0,
                maxLatency = 0.0,
                tokensPerSecond = 0.0,
                successRate = 0.0
            )
        }

        val totalTokens = successfulResults.sumOf { it.responseTokens }
        val totalTimeSeconds = latencies.sum() / 1000.0

        return BenchmarkStats(
            totalPrompts = results.size,
            completedPrompts = successfulResults.size,
            averageLatency = latencies.average(),
            p50Latency = percentile(latencies, 0.5),
            p90Latency = percentile(latencies, 0.9),
            p95Latency = percentile(latencies, 0.95),
            p99Latency = percentile(latencies, 0.99),
            minLatency = latencies.minOrNull() ?: 0.0,
            maxLatency = latencies.maxOrNull() ?: 0.0,
            tokensPerSecond = if (totalTimeSeconds > 0) totalTokens / totalTimeSeconds else 0.0,
            successRate = (successfulResults.size.toDouble() / results.size) * 100
        )
    }

    private fun percentile(sortedList: List<Double>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        val index = (percentile * (sortedList.size - 1)).toInt()
        return sortedList[index.coerceIn(0, sortedList.size - 1)]
    }

    private fun estimateTokenCount(text: String): Int {
        // Rough approximation: 1 token ≈ 4 characters for English
        return (text.length / 4.0).toInt()
    }

    private fun calculateEstimatedTime(latencies: List<Long>, remainingPrompts: Int): Long {
        if (latencies.isEmpty() || remainingPrompts <= 0) return 0

        val recentLatencies = latencies.takeLast(minOf(10, latencies.size))
        val averageLatency = recentLatencies.average()

        // Add 500ms buffer per prompt for processing
        return ((averageLatency + 500) * remainingPrompts).toLong()
    }

    fun cancelBenchmark() {
        currentJob?.cancel()
        _benchmarkProgress.value = null
    }

    suspend fun getBenchmarkRuns(): Flow<List<BenchmarkRun>> {
        return benchmarkDao.getAllBenchmarkRuns()
    }

    suspend fun getBenchmarkResults(runId: String): List<BenchmarkResult> {
        return benchmarkDao.getResultsForRun(runId)
    }

    suspend fun getModelComparisons(): List<ModelComparison> {
        val rawData = benchmarkDao.getModelComparisons()
        return rawData.map { raw ->
            // Use the actual data from the query instead of recalculating
            ModelComparison(
                modelId = raw.modelId,
                modelName = raw.modelName,
                stats = BenchmarkStats(
                    totalPrompts = 0, // Would need separate query for this
                    completedPrompts = raw.runCount,
                    averageLatency = raw.avgLatency,
                    p50Latency = 0.0, // Would need separate calculation
                    p90Latency = 0.0, // Would need separate calculation
                    p95Latency = 0.0, // Would need separate calculation
                    p99Latency = raw.avgP99Latency,
                    minLatency = 0.0, // Would need separate calculation
                    maxLatency = 0.0, // Would need separate calculation
                    tokensPerSecond = raw.avgTokensPerSecond,
                    successRate = 100.0 // Approximation since only completed runs
                ),
                runCount = raw.runCount,
                lastRunTime = raw.lastRunTime
            )
        }
    }

    suspend fun deleteBenchmarkRun(runId: String) {
        benchmarkDao.deleteBenchmarkRun(runId)
    }

    suspend fun deleteAllBenchmarkRuns() {
        benchmarkDao.deleteAllBenchmarkRuns()
    }
}