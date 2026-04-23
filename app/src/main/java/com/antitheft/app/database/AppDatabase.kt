package com.antitheft.app.database

import android.content.Context
import androidx.room.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CommandLog::class, OfflineDataEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandLogDao(): CommandLogDao
    abstract fun offlineDataDao(): OfflineDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "system_data.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val command: String,
    val timestamp: Long,
    val status: String,
    val result: String? = null
)

@Dao
interface CommandLogDao {
    @Insert
    suspend fun insert(log: CommandLog)
    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<CommandLog>
    @Query("DELETE FROM command_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

@Entity(tableName = "offline_data")
data class OfflineDataEntity(
    @PrimaryKey val id: String,
    val type: String,
    val path: String,
    val size: Long,
    val timestamp: Long,
    val priority: Int,
    val uploaded: Boolean,
    val metadata: String? = null
)

@Dao
interface OfflineDataDao {
    @Insert
    suspend fun insert(data: OfflineDataEntity)
    @Update
    suspend fun update(data: OfflineDataEntity)
    @Delete
    suspend fun delete(data: OfflineDataEntity)
    @Query("SELECT * FROM offline_data WHERE uploaded = 0 ORDER BY priority DESC")
    suspend fun getPendingUploads(): List<OfflineDataEntity>
    @Query("SELECT * FROM offline_data WHERE uploaded = 1 AND timestamp < :cutoffTime")
    suspend fun getOldUploadedData(cutoffTime: Long): List<OfflineDataEntity>
    @Query("SELECT SUM(size) FROM offline_data")
    suspend fun getTotalSize(): Long
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }
    @TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }
}
