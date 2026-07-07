package com.smithware.printoutscannerpro

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsStore by preferencesDataStore("printout_scanner_settings")
val DueSoonWindowKey = intPreferencesKey("due_soon_window")

enum class DocumentType { TRAINING_REPORT, SCHEDULE, CLEANING_CHECKLIST, INVENTORY_SHEET, SIGN_OFF_LOG, GENERIC_TABLE }
enum class TrainingStatus { NOT_STARTED, IN_PROGRESS, SENT_TO_TRAINING, COMPLETED, COULD_NOT_COMPLETE, OVERDUE, NEEDS_FOLLOW_UP }
enum class AvailabilityStatus { WORKING, AVAILABLE_NOW, BUSY, OFF, UNKNOWN }

@Entity
data class DocumentImport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentType: DocumentType,
    val title: String,
    val imageUri: String?,
    val rawOcrText: String,
    val importedAt: Long,
    val confirmedAt: Long?,
    val notes: String = ""
)

@Entity
data class ExtractedRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val importId: Long,
    val rowIndex: Int,
    val rawText: String,
    val confidence: Float,
    val createdAt: Long
)

@Entity
data class Associate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val department: String? = null,
    val role: String? = null,
    val active: Boolean = true,
    val usualShiftNote: String? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity
data class TrainingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val associateId: Long,
    val importId: Long?,
    val trainingName: String,
    val dueDate: Long?,
    val status: TrainingStatus,
    val priorityScore: Int,
    val priorityReason: String,
    val completedAt: Long?,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity
data class WorkAvailability(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val associateId: Long,
    val date: Long,
    val isWorking: Boolean,
    val shiftStart: String?,
    val shiftEnd: String?,
    val availabilityStatus: AvailabilityStatus,
    val notes: String = ""
)

@Entity
data class TrainingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainingItemId: Long,
    val eventType: String,
    val note: String,
    val createdAt: Long
)

data class TrainingCardData(
    val id: Long,
    val associateId: Long,
    val associateName: String,
    val trainingName: String,
    val dueDate: Long?,
    val status: TrainingStatus,
    val priorityScore: Int,
    val priorityReason: String,
    val notes: String,
    val isWorking: Boolean,
    val shiftEnd: String?
)

data class ImportSummary(
    val id: Long,
    val title: String,
    val documentType: DocumentType,
    val importedAt: Long,
    val rowCount: Int,
    val activeCount: Int
)

@Dao
interface PrintoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAssociate(value: Associate): Long
    @Insert suspend fun insertImport(value: DocumentImport): Long
    @Insert suspend fun insertRow(value: ExtractedRow): Long
    @Insert suspend fun insertTraining(value: TrainingItem): Long
    @Insert suspend fun insertEvent(value: TrainingEvent): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAvailability(value: WorkAvailability): Long

    @Query("SELECT * FROM Associate WHERE name = :name LIMIT 1")
    suspend fun findAssociateByName(name: String): Associate?

    @Query("SELECT COUNT(*) FROM Associate")
    suspend fun associateCount(): Int

    @Query("SELECT COUNT(*) FROM TrainingItem")
    suspend fun trainingCount(): Int

    @Query("SELECT * FROM Associate ORDER BY active DESC, name")
    fun associates(): Flow<List<Associate>>

    @Query(
        """
        SELECT t.id, a.id AS associateId, a.name AS associateName, t.trainingName, t.dueDate, t.status,
               t.priorityScore, t.priorityReason, t.notes,
               COALESCE(w.isWorking, 0) AS isWorking, w.shiftEnd AS shiftEnd
        FROM TrainingItem t
        JOIN Associate a ON a.id = t.associateId
        LEFT JOIN WorkAvailability w ON w.associateId = a.id AND w.date = :today
        ORDER BY t.priorityScore DESC, t.dueDate IS NULL, t.dueDate ASC
        """
    )
    fun trainingCards(today: Long): Flow<List<TrainingCardData>>

    @Query("SELECT COUNT(*) FROM TrainingItem WHERE associateId = :associateId AND completedAt IS NULL")
    suspend fun openTrainingCount(associateId: Long): Int

    @Query("UPDATE TrainingItem SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTrainingStatus(id: Long, status: TrainingStatus, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE TrainingItem SET dueDate = :dueDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: Long?, updatedAt: Long)

    @Query("DELETE FROM TrainingItem WHERE id = :id")
    suspend fun deleteTraining(id: Long)

    @Query("DELETE FROM WorkAvailability WHERE associateId = :associateId AND date = :date")
    suspend fun deleteAvailability(associateId: Long, date: Long)

    @Query("DELETE FROM TrainingEvent")
    suspend fun clearEvents()

    @Query("DELETE FROM WorkAvailability")
    suspend fun clearAvailability()

    @Query("DELETE FROM TrainingItem")
    suspend fun clearTraining()

    @Query("DELETE FROM Associate")
    suspend fun clearAssociates()

    @Query("DELETE FROM ExtractedRow")
    suspend fun clearRows()

    @Query("DELETE FROM DocumentImport")
    suspend fun clearImports()

    @Query(
        """
        SELECT d.id, d.title, d.documentType, d.importedAt,
               COUNT(r.id) AS rowCount,
               SUM(CASE WHEN t.completedAt IS NULL THEN 1 ELSE 0 END) AS activeCount
        FROM DocumentImport d
        LEFT JOIN ExtractedRow r ON r.importId = d.id
        LEFT JOIN TrainingItem t ON t.importId = d.id
        GROUP BY d.id
        ORDER BY d.importedAt DESC
        """
    )
    fun importSummaries(): Flow<List<ImportSummary>>
}

@Database(
    entities = [DocumentImport::class, ExtractedRow::class, Associate::class, TrainingItem::class, WorkAvailability::class, TrainingEvent::class],
    version = 1,
    exportSchema = false
)
abstract class PrintoutDatabase : RoomDatabase() {
    abstract fun dao(): PrintoutDao

    companion object {
        @Volatile private var instance: PrintoutDatabase? = null

        fun get(context: Context): PrintoutDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrintoutDatabase::class.java,
                    "printout_scanner.db"
                ).build().also { instance = it }
            }
    }
}

val Context.dueSoonWindowFlow: Flow<Int>
    get() = settingsStore.data.map { it[DueSoonWindowKey] ?: 3 }
