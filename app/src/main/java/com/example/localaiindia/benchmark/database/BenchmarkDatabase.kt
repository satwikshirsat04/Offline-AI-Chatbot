package com.example.localaiindia.benchmark.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.example.localaiindia.benchmark.*

// DAO Interface
@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_runs ORDER BY startTime DESC")
    fun getAllBenchmarkRuns(): Flow<List<BenchmarkRun>>

    @Query("SELECT * FROM benchmark_runs WHERE modelId = :modelId ORDER BY startTime DESC")
    fun getBenchmarkRunsForModel(modelId: String): Flow<List<BenchmarkRun>>

    @Query("SELECT * FROM benchmark_runs WHERE id = :runId")
    suspend fun getBenchmarkRun(runId: String): BenchmarkRun?

    @Query("SELECT * FROM benchmark_results WHERE benchmarkRunId = :runId ORDER BY promptIndex")
    suspend fun getResultsForRun(runId: String): List<BenchmarkResult>

    @Query("SELECT * FROM benchmark_results WHERE benchmarkRunId = :runId ORDER BY promptIndex")
    fun getResultsForRunFlow(runId: String): Flow<List<BenchmarkResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkRun(run: BenchmarkRun): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkResult(result: BenchmarkResult): Long

    @Update
    suspend fun updateBenchmarkRun(run: BenchmarkRun): Int

    @Query("DELETE FROM benchmark_runs WHERE id = :runId")
    suspend fun deleteBenchmarkRun(runId: String): Int

    @Query("DELETE FROM benchmark_runs")
    suspend fun deleteAllBenchmarkRuns(): Int

    // Analytics queries
    @Query("""
        SELECT 
            br.modelId,
            br.modelName,
            COUNT(*) as runCount,
            MAX(br.startTime) as lastRunTime,
            AVG(br.averageLatency) as avgLatency,
            AVG(br.p99Latency) as avgP99Latency,
            AVG(br.tokensPerSecond) as avgTokensPerSecond
        FROM benchmark_runs br 
        WHERE br.status = 'COMPLETED'
        GROUP BY br.modelId, br.modelName
        ORDER BY avgLatency ASC
    """)
    suspend fun getModelComparisons(): List<ModelComparisonRaw>

    @Query("""
        SELECT responseTimeMs 
        FROM benchmark_results 
        WHERE benchmarkRunId = :runId AND success = 1
        ORDER BY responseTimeMs
    """)
    suspend fun getResponseTimesForRun(runId: String): List<Long>
}

// Raw data class for Room query result
data class ModelComparisonRaw(
    val modelId: String,
    val modelName: String,
    val runCount: Int,
    val lastRunTime: Long,
    val avgLatency: Double,
    val avgP99Latency: Double,
    val avgTokensPerSecond: Double
)

// Room Database
@Database(
    entities = [BenchmarkRun::class, BenchmarkResult::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(BenchmarkConverters::class)
abstract class BenchmarkDatabase : RoomDatabase() {
    abstract fun benchmarkDao(): BenchmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BenchmarkDatabase? = null

        fun getDatabase(context: Context): BenchmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BenchmarkDatabase::class.java,
                    "benchmark_database"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Type converters for Room
class BenchmarkConverters {
    @TypeConverter
    fun fromBenchmarkStatus(status: BenchmarkStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBenchmarkStatus(status: String): BenchmarkStatus {
        return BenchmarkStatus.valueOf(status)
    }
}