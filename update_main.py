import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

new_home_composable = """        composable("home") {
            HomeScreen(
                currentRoute = "home",
                onNavigate = { route ->
                    if (route != "home") {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onNavigateToCapture = {
                    viewModel.resetScan()
                    navController.navigate("capture")
                },
                onSelectSamplePreset = { preset ->
                    viewModel.selectSamplePreset(preset)
                    viewModel.startAnalysis {
                        navController.navigate("results") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    navController.navigate("analyzing")
                },
                onViewSavedReport = { report ->
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
                    navController.navigate("report")
                },
                reports = reports,
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) }
            )
        }
        
        composable("dashboard") {
            com.example.ui.screens.DashboardScreen(
                currentRoute = "dashboard",
                onNavigate = { route ->
                    if (route != "dashboard") {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                reports = viewModel.filteredReports.collectAsState().value,
                onViewSavedReport = { report ->
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
                    navController.navigate("report")
                }
            )
        }

        composable("favourites") {
            com.example.ui.screens.FavouritesScreen(
                currentRoute = "favourites",
                onNavigate = { route ->
                    if (route != "favourites") {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                reports = viewModel.favoriteReports.collectAsState().value,
                onViewSavedReport = { report ->
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
                    navController.navigate("report")
                }
            )
        }
        
        composable("profile") {
            com.example.ui.screens.ProfileScreen(
                currentRoute = "profile",
                onNavigate = { route ->
                    if (route != "profile") {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }"""

content = re.sub(r'composable\("home"\)\s*\{[\s\S]*?\}\s*composable\(\s*route = "capture"', new_home_composable + '\n        composable(\n            route = "capture"', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
