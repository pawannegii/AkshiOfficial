import re

with open("app/src/main/java/com/example/viewmodel/AkshiViewModel.kt", "r") as f:
    content = f.read()

# Add to AkshiUiState
content = content.replace("val savedReportId: Long? = null,", "val savedReportId: Long? = null,\n    val isCurrentReportFavorite: Boolean = false,")

# Update viewSavedReport
view_saved_replacement = """    fun viewSavedReport(report: InspectionReportEntity) {
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
                isCurrentReportFavorite = report.isFavorite
            ) 
        }
    }"""
content = re.sub(r'    fun viewSavedReport[\s\S]*?savedReportId = report\.id\s*\)\s*\}\s*\}', view_saved_replacement, content)

# Update resetScan
content = content.replace("savedReportId = null", "savedReportId = null,\n                isCurrentReportFavorite = false")

# Add toggleFavoriteCurrentReport
toggle_replacement = """    fun toggleFavorite(report: InspectionReportEntity) {
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
    }"""
content = re.sub(r'    fun toggleFavorite[\s\S]*?dao\.insertReport\(updated\)\s*\}\s*\}', toggle_replacement, content)

with open("app/src/main/java/com/example/viewmodel/AkshiViewModel.kt", "w") as f:
    f.write(content)
