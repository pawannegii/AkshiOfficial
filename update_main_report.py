import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

replacement = """            ReportScreen(
                batchId = uiState.activeBatchId.ifEmpty { "AK-26031-DEMO" },
                result = result,
                location = "Lasalgaon Mandi, Nashik",
                inspectorName = "Pawan Negi (SIH-26031)",
                onSaveReport = {
                    viewModel.saveCurrentReport(
                        location = "Lasalgaon Mandi, Nashik",
                        inspectorName = "Pawan Negi (SIH-26031)",
                        onSaved = { }
                    )
                },
                onScanAgain = {
                    viewModel.resetScan()
                    navController.popBackStack("home", inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                isSaved = uiState.savedReportId != null,
                isFavorite = uiState.isCurrentReportFavorite,
                onToggleFavorite = { viewModel.toggleFavoriteCurrentReport() }
            )"""

content = re.sub(r'            ReportScreen\([\s\S]*?isSaved = uiState\.savedReportId != null\s*\)', replacement, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
