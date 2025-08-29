package com.example.localaiindia.benchmark

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

// Data models
@Entity(tableName = "benchmark_runs")
data class BenchmarkRun(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val modelId: String,
    val modelName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalPrompts: Int,
    val completedPrompts: Int = 0,
    val status: BenchmarkStatus,
    val averageLatency: Double = 0.0,
    val p99Latency: Double = 0.0,
    val minLatency: Double = 0.0,
    val maxLatency: Double = 0.0,
    val tokensPerSecond: Double = 0.0
)

@Entity(
    tableName = "benchmark_results",
    foreignKeys = [
        ForeignKey(
            entity = BenchmarkRun::class,
            parentColumns = ["id"],
            childColumns = ["benchmarkRunId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["benchmarkRunId"]) // Add index for foreign key
    ]
)
data class BenchmarkResult(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val benchmarkRunId: String,
    val promptIndex: Int,
    val prompt: String,
    val response: String,
    val startTime: Long,
    val endTime: Long,
    val responseTimeMs: Long,
    val tokenCount: Int = 0,
    val promptTokens: Int = 0,
    val responseTokens: Int = 0,
    val contextLength: Int = 0,
    val success: Boolean = true,
    val errorMessage: String? = null
)

enum class BenchmarkStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class BenchmarkStats(
    val totalPrompts: Int,
    val completedPrompts: Int,
    val averageLatency: Double,
    val p50Latency: Double,
    val p90Latency: Double,
    val p95Latency: Double,
    val p99Latency: Double,
    val minLatency: Double,
    val maxLatency: Double,
    val tokensPerSecond: Double,
    val successRate: Double
)

data class ModelComparison(
    val modelId: String,
    val modelName: String,
    val stats: BenchmarkStats,
    val runCount: Int,
    val lastRunTime: Long
)