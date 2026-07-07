package com.smithware.printoutscannerpro

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class EditableTrainingRow(
    val id: Long = System.nanoTime(),
    val associate: String,
    val training: String,
    val dueDateMillis: Long?,
    val dueDateText: String,
    val status: TrainingStatus,
    val rawText: String
)

data class ScanReviewState(
    val imageUri: String? = null,
    val rawText: String = "",
    val isProcessing: Boolean = false,
    val message: String = "Ready to scan.",
    val rows: List<EditableTrainingRow> = emptyList()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val dao = PrintoutDatabase.get(context).dao()
    private val parser = TrainingReportParser()
    private val todayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val dueSoonWindow = context.dueSoonWindowFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val associates = dao.associates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trainingCards = dao.trainingCards(todayMillis)
        .combine(dueSoonWindow) { cards, window ->
            cards.map { card ->
                val result = TrainingPriorityEngine.score(
                    dueDate = card.dueDate,
                    status = card.status,
                    isWorkingToday = card.isWorking,
                    shiftEnd = card.shiftEnd,
                    openTrainingItemsForAssociate = cards.count { it.associateId == card.associateId && it.status != TrainingStatus.COMPLETED },
                    dueSoonWindow = window
                )
                card.copy(priorityScore = result.score, priorityReason = result.reason)
            }.sortedByDescending { it.priorityScore }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val imports = dao.importSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reviewState = MutableStateFlow(ScanReviewState())
    val reviewState: StateFlow<ScanReviewState> = _reviewState.asStateFlow()

    init {
        viewModelScope.launch { seedIfNeeded() }
    }

    fun setDueSoonWindow(days: Int) {
        viewModelScope.launch { context.settingsStore.edit { it[DueSoonWindowKey] = days } }
    }

    fun processImage(uri: Uri) {
        _reviewState.value = ScanReviewState(imageUri = uri.toString(), isProcessing = true, message = "Reading printout...")
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    val image = InputImage.fromFilePath(context, uri)
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await().text
                }
            }
            val raw = result.getOrDefault("")
            buildReviewRows(uri.toString(), raw.ifBlank { sampleRawText() }, result.exceptionOrNull() != null)
        }
    }

    fun useManualEntry() {
        buildReviewRows(null, sampleRawText(), failed = false)
    }

    private fun buildReviewRows(uri: String?, rawText: String, failed: Boolean) {
        val parsed = parser.parse(rawText)
        _reviewState.value = ScanReviewState(
            imageUri = uri,
            rawText = rawText,
            isProcessing = false,
            message = if (failed || rawText.isBlank()) "We couldn't confidently read this printout. Review or enter rows manually." else "Text found. Review extracted rows.",
            rows = parsed.rows.map {
                EditableTrainingRow(
                    associate = it.associate,
                    training = it.training,
                    dueDateMillis = it.dueDateMillis,
                    dueDateText = it.dueDateText,
                    status = it.status,
                    rawText = it.rawText
                )
            }
        )
    }

    fun updateRow(row: EditableTrainingRow) {
        _reviewState.value = _reviewState.value.copy(rows = _reviewState.value.rows.map { if (it.id == row.id) row else it })
    }

    fun deleteRow(rowId: Long) {
        _reviewState.value = _reviewState.value.copy(rows = _reviewState.value.rows.filterNot { it.id == rowId })
    }

    fun addBlankRow() {
        _reviewState.value = _reviewState.value.copy(
            rows = _reviewState.value.rows + EditableTrainingRow(
                associate = "",
                training = "",
                dueDateMillis = null,
                dueDateText = "",
                status = TrainingStatus.NOT_STARTED,
                rawText = "Manual row"
            )
        )
    }

    fun confirmExtraction(onDone: () -> Unit) {
        val state = _reviewState.value
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val importId = dao.insertImport(
                DocumentImport(
                    documentType = DocumentType.TRAINING_REPORT,
                    title = "Training Report",
                    imageUri = state.imageUri,
                    rawOcrText = state.rawText,
                    importedAt = now,
                    confirmedAt = now
                )
            )
            state.rows.filter { it.associate.isNotBlank() && it.training.isNotBlank() }.forEachIndexed { index, row ->
                dao.insertRow(ExtractedRow(importId = importId, rowIndex = index, rawText = row.rawText, confidence = 0.72f, createdAt = now))
                val associate = dao.findAssociateByName(row.associate.trim())
                val associateId = associate?.id ?: dao.insertAssociate(
                    Associate(name = row.associate.trim(), createdAt = now, updatedAt = now)
                )
                val openCount = dao.openTrainingCount(associateId) + 1
                val priority = TrainingPriorityEngine.score(row.dueDateMillis, row.status, false, null, openCount, dueSoonWindow.value, now)
                val itemId = dao.insertTraining(
                    TrainingItem(
                        associateId = associateId,
                        importId = importId,
                        trainingName = row.training.trim(),
                        dueDate = row.dueDateMillis,
                        status = if (row.status == TrainingStatus.OVERDUE) TrainingStatus.OVERDUE else row.status,
                        priorityScore = priority.score,
                        priorityReason = priority.reason,
                        completedAt = if (row.status == TrainingStatus.COMPLETED) now else null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                dao.insertEvent(TrainingEvent(trainingItemId = itemId, eventType = "Imported", note = "Confirmed from OCR review", createdAt = now))
            }
            _reviewState.value = ScanReviewState(message = "Saved tracker.")
            onDone()
        }
    }

    fun setWorkingToday(associate: Associate, working: Boolean) {
        viewModelScope.launch {
            if (!working) {
                dao.deleteAvailability(associate.id, todayMillis)
            } else {
                dao.insertAvailability(
                    WorkAvailability(
                        associateId = associate.id,
                        date = todayMillis,
                        isWorking = true,
                        shiftStart = null,
                        shiftEnd = "4:00 PM",
                        availabilityStatus = AvailabilityStatus.WORKING
                    )
                )
            }
        }
    }

    fun updateTrainingStatus(id: Long, status: TrainingStatus) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.updateTrainingStatus(id, status, if (status == TrainingStatus.COMPLETED) now else null, now)
            dao.insertEvent(TrainingEvent(trainingItemId = id, eventType = status.name, note = "Status changed", createdAt = now))
        }
    }

    fun updateTrainingDueDate(id: Long, dueDate: Long?) {
        viewModelScope.launch { dao.updateDueDate(id, dueDate, System.currentTimeMillis()) }
    }

    fun deleteTraining(id: Long) {
        viewModelScope.launch { dao.deleteTraining(id) }
    }

    fun clearLocalData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.clearEvents()
            dao.clearAvailability()
            dao.clearTraining()
            dao.clearRows()
            dao.clearImports()
            dao.clearAssociates()
            onDone()
        }
    }

    private suspend fun seedIfNeeded() {
        if (dao.trainingCount() > 0 || dao.associateCount() > 0) return
        val now = System.currentTimeMillis()
        val people = listOf("Jordan M.", "Sarah L.", "Mike R.", "Amanda P.", "Chris T.")
        val ids = people.associateWith { dao.insertAssociate(Associate(name = it, createdAt = now, updatedAt = now)) }
        val importId = dao.insertImport(
            DocumentImport(
                documentType = DocumentType.TRAINING_REPORT,
                title = "Sample Training Report",
                imageUri = null,
                rawOcrText = sampleRawText(),
                importedAt = now,
                confirmedAt = now,
                notes = "Seed sample"
            )
        )
        val rows = parser.parse(sampleRawText()).rows + listOf(
            ParsedTrainingRow("Amanda P.", "Workplace Safety", datePlus(5), "Jul 22", TrainingStatus.IN_PROGRESS, "Amanda P. Workplace Safety Jul 22 In Progress"),
            ParsedTrainingRow("Chris T.", "Equipment Training", null, "", TrainingStatus.NOT_STARTED, "Chris T. Equipment Training No due date")
        )
        rows.forEachIndexed { index, row ->
            dao.insertRow(ExtractedRow(importId = importId, rowIndex = index, rawText = row.rawText, confidence = row.confidence, createdAt = now))
            val associateId = ids[row.associate] ?: dao.insertAssociate(Associate(name = row.associate, createdAt = now, updatedAt = now))
            val priority = TrainingPriorityEngine.score(row.dueDateMillis, row.status, row.associate in listOf("Jordan M.", "Sarah L.", "Mike R."), "4:00 PM", 1, 3, now)
            dao.insertTraining(
                TrainingItem(
                    associateId = associateId,
                    importId = importId,
                    trainingName = row.training,
                    dueDate = row.dueDateMillis,
                    status = row.status,
                    priorityScore = priority.score,
                    priorityReason = priority.reason,
                    completedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (row.associate in listOf("Jordan M.", "Sarah L.", "Mike R.")) {
                dao.insertAvailability(WorkAvailability(associateId = associateId, date = todayMillis, isWorking = true, shiftStart = null, shiftEnd = "4:00 PM", availabilityStatus = AvailabilityStatus.WORKING))
            }
        }
    }

    private fun datePlus(days: Long): Long =
        LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun sampleRawText(): String = """
        TRAINING REPORT - WEEK OF JUL 7 - JUL 13
        Associate | Training | Due Date | Status
        Jordan M. | Food Safety | Jul 18 | Incomplete
        Sarah L. | HazCom | Jul 20 | Incomplete
        Mike R. | Cleaning | Jul 16 | Overdue
    """.trimIndent()
}
