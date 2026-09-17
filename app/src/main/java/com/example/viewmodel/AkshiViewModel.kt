package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AkshiDatabase
import com.example.data.InspectionReportEntity
import com.example.model.OnionResult
import com.example.service.OnionAnalysisResponse
import com.example.service.OnionAnalysisService
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.service.OnionInspectionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    LOCATION("Location (A-Z)")
}

data class AkshiUiState(
    val selectedImageUri: Uri? = null,
    val selectedSamplePreset: String? = null, // "GRADE_A", "MIXED", "HIGH_URS"
    val isAnalyzing: Boolean = false,
    val analysisStep: Int = 0, // 0: Detecting onions, 1: Surface texture & color, 2: Proportions
    val analysisStepText: String = "Scanning frame for onions...",
    val currentResult: OnionResult? = null,
    val errorMessage: String? = null,
    val savedReportId: Long? = null,
    val isCurrentReportFavorite: Boolean = false,
    val activeBatchId: String = ""
)

class AkshiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AkshiDatabase.getDatabase(application)
    private val dao = db.inspectionDao()
    private val analysisService: OnionAnalysisService = OnionInspectionService()

    private val _uiState = MutableStateFlow(AkshiUiState())
    val uiState: StateFlow<AkshiUiState> = _uiState.asStateFlow()
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val reportsList: StateFlow<List<InspectionReportEntity>> = dao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReports: StateFlow<List<InspectionReportEntity>> = combine(
        dao.getAllReports(),
        _searchQuery,
        _sortOrder
    ) { reports, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            reports
        } else {
            reports.filter {
                it.batchId.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.inspectorName.contains(query, ignoreCase = true)
            }
        }
        when (sortOrder) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.timestamp }
            SortOrder.LOCATION -> filtered.sortedBy { it.location }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteReports: StateFlow<List<InspectionReportEntity>> = combine(
        dao.getAllReports(),
        _searchQuery,
        _sortOrder
    ) { reports, query, sortOrder ->
        val favorites = reports.filter { it.isFavorite }
        val filtered = if (query.isBlank()) {
            favorites
        } else {
            favorites.filter {
                it.batchId.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.inspectorName.contains(query, ignoreCase = true)
            }
        }
        when (sortOrder) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.timestamp }
            SortOrder.LOCATION -> filtered.sortedBy { it.location }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Generate initial unique batch ID
        refreshBatchId()
    }

    fun cycleSortOrder() {
        _sortOrder.update { current ->
            when (current) {
                SortOrder.DATE_DESC -> SortOrder.DATE_ASC
                SortOrder.DATE_ASC -> SortOrder.LOCATION
                SortOrder.LOCATION -> SortOrder.DATE_DESC
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshBatchId() {
        val randomNum = Random.nextInt(1000, 9999)
        val dateStr = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(activeBatchId = "AK-$dateStr-$randomNum") }
    }

    fun selectSamplePreset(preset: String) {
        _uiState.update {
            it.copy(
                selectedSamplePreset = preset,
                selectedImageUri = null,
                errorMessage = null
            )
        }
    }

    fun selectImageUri(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                selectedSamplePreset = null,
                errorMessage = null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val persistentUri = ImageStorageHelper.savePersistentInspectionImage(
                context = getApplication(),
                sourceUri = uri,
                batchId = _uiState.value.activeBatchId
            )
            if (persistentUri != null) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        if (it.selectedImageUri == uri) {
                            it.copy(selectedImageUri = persistentUri)
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                selectedSamplePreset = null,
                errorMessage = null
            )
        }
    }

    fun resetScan() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                selectedSamplePreset = null,
                isAnalyzing = false,
                analysisStep = 0,
                analysisStepText = "Scanning frame for onions...",
                errorMessage = null,
                currentResult = null,
                savedReportId = null,
                isCurrentReportFavorite = false
            )
        }
        refreshBatchId()
    }

    fun startAnalysis(onComplete: () -> Unit) {
        val currentState = _uiState.value
        refreshBatchId()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisStep = 0,
                    analysisStepText = "Scanning image for onion bulbs...",
                    errorMessage = null,
                    currentResult = null,
                    savedReportId = null,
                isCurrentReportFavorite = false
                )
            }

            // Step 1: Real verification of onions in the image
            delay(500)
            val detectionResult = analysisService.detectAndAnalyze(
                context = getApplication(),
                imageUri = currentState.selectedImageUri,
                presetType = currentState.selectedSamplePreset
            ) { step, msg ->
                _uiState.update {
                    it.copy(
                        analysisStep = step,
                        analysisStepText = msg
                    )
                }
            }

            when (detectionResult) {
                is OnionAnalysisResponse.NoOnionsDetected -> {
                    // STOP IMMEDIATELY! Do NOT produce fake calculation!
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = detectionResult.message,
                            currentResult = null
                        )
                    }
                    return@launch
                }
                is OnionAnalysisResponse.Success -> {
                    val currentUri = currentState.selectedImageUri
                    val persistentUri = if (currentUri != null) {
                        ImageStorageHelper.savePersistentInspectionImage(
                            context = getApplication(),
                            sourceUri = currentUri,
                            batchId = currentState.activeBatchId
                        ) ?: currentUri
                    } else null

                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            currentResult = detectionResult.result,
                            selectedImageUri = persistentUri ?: currentState.selectedImageUri,
                            errorMessage = null
                        )
                    }
                    onComplete()
                }
            }
        }
    }

    fun saveCurrentReport(
        location: String = "Lasalgaon Mandi, Nashik",
        inspectorName: String = "Pawan Negi (SIH-26031)",
        onSaved: (Long) -> Unit
    ) {
        val state = _uiState.value
        val result = state.currentResult ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val persistentUriString = state.selectedImageUri?.let { uri ->
                ImageStorageHelper.savePersistentInspectionImage(
                    context = getApplication(),
                    sourceUri = uri,
                    batchId = state.activeBatchId
                )?.toString() ?: uri.toString()
            }

            val entity = InspectionReportEntity(
                id = state.savedReportId ?: 0,
                batchId = state.activeBatchId,
                location = location,
                inspectorName = inspectorName,
                totalOnions = result.totalOnionsDetected,
                gradeAPercentage = result.gradeAPercentage,
                ursPercentage = result.ursPercentage,
                damagedCount = result.defectBreakdown.damaged,
                rottenCount = result.defectBreakdown.rotten,
                sproutedCount = result.defectBreakdown.sprouted,
                undersizedCount = result.defectBreakdown.undersized,
                notes = result.notes,
                imageUri = persistentUriString,
                samplePresetType = state.selectedSamplePreset
            )
            val newId = dao.insertReport(entity)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        savedReportId = newId,
                        selectedImageUri = persistentUriString?.let { str -> Uri.parse(str) } ?: it.selectedImageUri
                    )
                }
                onSaved(newId)
            }
        }
    }

    fun deleteReport(report: InspectionReportEntity) {
        viewModelScope.launch {
            dao.deleteReport(report)
            if (_uiState.value.savedReportId == report.id) {
                _uiState.update { it.copy(savedReportId = null, isCurrentReportFavorite = false) }
            }
        }
    }

    fun deleteCurrentReport(onDeleted: () -> Unit) {
        val reportId = _uiState.value.savedReportId
        if (reportId != null) {
            viewModelScope.launch {
                val report = dao.getReportById(reportId)
                if (report != null) {
                    dao.deleteReport(report)
                }
                _uiState.update { it.copy(savedReportId = null, isCurrentReportFavorite = false) }
                onDeleted()
            }
        } else {
            onDeleted()
        }
    }

    fun viewSavedReport(report: InspectionReportEntity) {
        val onionResult = com.example.model.OnionResult(
            totalOnionsDetected = report.totalOnions,
            gradeAPercentage = report.gradeAPercentage,
            ursPercentage = report.ursPercentage,
            defectBreakdown = com.example.model.DefectBreakdown(
                damaged = report.damagedCount,
                rotten = report.rottenCount,
                sprouted = report.sproutedCount,
                undersized = report.undersizedCount
            ),
            notes = report.notes
        )
        _uiState.update { 
            it.copy(
                activeBatchId = report.batchId,
                currentResult = onionResult,
                savedReportId = report.id,
                isCurrentReportFavorite = report.isFavorite,
                selectedImageUri = report.imageUri?.let { uriStr -> Uri.parse(uriStr) },
                selectedSamplePreset = report.samplePresetType
            ) 
        }
    }

    fun toggleFavorite(report: InspectionReportEntity) {
        viewModelScope.launch {
            val updated = report.copy(isFavorite = !report.isFavorite)
            dao.insertReport(updated)
            if (_uiState.value.savedReportId == report.id) {
                _uiState.update { it.copy(isCurrentReportFavorite = updated.isFavorite) }
            }
        }
    }

    fun toggleFavoriteCurrentReport() {
        val state = _uiState.value
        val reportId = state.savedReportId
        if (reportId != null) {
            viewModelScope.launch {
                val report = dao.getReportById(reportId)
                if (report != null) {
                    val updated = report.copy(isFavorite = !report.isFavorite)
                    dao.insertReport(updated)
                    _uiState.update { it.copy(isCurrentReportFavorite = updated.isFavorite) }
                }
            }
        } else {
            saveCurrentReport { newId ->
                viewModelScope.launch {
                    val report = dao.getReportById(newId)
                    if (report != null) {
                        val updated = report.copy(isFavorite = true)
                        dao.insertReport(updated)
                        _uiState.update { it.copy(isCurrentReportFavorite = true) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        analysisService.close()
    }
}
