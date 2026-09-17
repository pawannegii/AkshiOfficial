package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.InspectionReportEntity
import com.example.ui.screens.AnalyzingScreen
import com.example.ui.screens.CaptureScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.AkshiTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AkshiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AkshiTheme {
                AkshiApp()
            }
        }
    }
}

// Fluid physics curves for butter-smooth 60/120fps navigation motion
private val FluidDecel = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val FluidAccel = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

@Composable
fun AkshiApp(viewModel: AkshiViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val reports by viewModel.reportsList.collectAsState()

    val onViewSavedReport = remember(viewModel, navController) {
        { report: InspectionReportEntity ->
            viewModel.viewSavedReport(report)
            navController.navigate("report")
        }
    }
    val onToggleFavorite = remember(viewModel) {
        { report: InspectionReportEntity -> viewModel.toggleFavorite(report) }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(320, easing = FluidDecel),
                initialOffset = { it / 4 }
            ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(260, easing = FluidAccel),
                targetOffset = { -it / 6 }
            ) + fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(320, easing = FluidDecel),
                initialOffset = { -it / 6 }
            ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(260, easing = FluidAccel),
                targetOffset = { it / 3 }
            ) + fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
        },
        modifier = Modifier.fillMaxSize()
    ) {
                composable("home") {
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
                onUploadImage = { uri ->
                    viewModel.selectImageUri(uri)
                    viewModel.startAnalysis {
                        navController.navigate("results") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    navController.navigate("analyzing")
                },
                onNavigateToAbout = {
                    navController.navigate("about")
                },
                onViewSavedReport = onViewSavedReport,
                reports = viewModel.filteredReports.collectAsState().value,
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onFilterClick = { 
                    viewModel.cycleSortOrder()
                    Toast.makeText(context, "Sorted by: ${viewModel.sortOrder.value.label}", Toast.LENGTH_SHORT).show()
                },
                onToggleFavorite = onToggleFavorite
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
                onNavigateBack = {
                    navController.popBackStack()
                },
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onFilterClick = { 
                    viewModel.cycleSortOrder()
                    Toast.makeText(context, "Sorted by: ${viewModel.sortOrder.value.label}", Toast.LENGTH_SHORT).show()
                },
                reports = viewModel.filteredReports.collectAsState().value,
                onViewSavedReport = { report ->
                    viewModel.viewSavedReport(report)
                    navController.navigate("report")
                },
                onToggleFavorite = { report -> viewModel.toggleFavorite(report) },
                onDeleteReport = { report -> viewModel.deleteReport(report) }
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
                onNavigateBack = {
                    navController.popBackStack()
                },
                searchQuery = viewModel.searchQuery.collectAsState().value,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onFilterClick = { 
                    viewModel.cycleSortOrder()
                    Toast.makeText(context, "Sorted by: ${viewModel.sortOrder.value.label}", Toast.LENGTH_SHORT).show()
                },
                reports = viewModel.favoriteReports.collectAsState().value,
                onViewSavedReport = { report ->
                    viewModel.viewSavedReport(report)
                    navController.navigate("report")
                },
                onToggleFavorite = { report -> viewModel.toggleFavorite(report) },
                onDeleteReport = { report -> viewModel.deleteReport(report) }
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
        }
        composable(
            route = "capture",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(340, easing = FluidDecel),
                    initialOffset = { it / 3 }
                ) + fadeIn(animationSpec = tween(240))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(280, easing = FluidAccel),
                    targetOffset = { it / 3 }
                ) + fadeOut(animationSpec = tween(200))
            }
        ) {
            CaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onImageSelected = { uri ->
                    viewModel.selectImageUri(uri)
                },
                onPresetSelected = { preset ->
                    viewModel.selectSamplePreset(preset)
                },
                onClearSelection = {
                    viewModel.clearSelection()
                },
                onProceedToAnalyze = {
                    viewModel.startAnalysis {
                        navController.navigate("results") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    navController.navigate("analyzing")
                },
                selectedImageUri = uiState.selectedImageUri,
                selectedPreset = uiState.selectedSamplePreset
            )
        }

        composable(
            route = "analyzing",
            enterTransition = {
                fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 1.02f, animationSpec = tween(200, easing = FastOutLinearInEasing))
            }
        ) {
            AnalyzingScreen(
                currentStep = uiState.analysisStep,
                stepText = uiState.analysisStepText,
                errorMessage = uiState.errorMessage,
                onRetry = {
                    // Navigate back to capture screen so user can re-align camera on actual onions
                    navController.popBackStack()
                },
                onFallbackPreset = {
                    viewModel.selectSamplePreset("GRADE_A")
                    viewModel.startAnalysis {
                        navController.navigate("results") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = "results",
            enterTransition = {
                fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(340, easing = FluidDecel),
                        initialOffset = { it / 5 }
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            }
        ) {
            val result = uiState.currentResult ?: com.example.model.OnionResult.empty()
            ResultsScreen(
                batchId = uiState.activeBatchId,
                result = result,
                imageUri = uiState.selectedImageUri,
                presetType = uiState.selectedSamplePreset,
                onNavigateBack = {
                    navController.popBackStack("home", inclusive = false)
                },
                onNavigateToReport = {
                    navController.navigate("report")
                }
            )
        }

        composable(
            route = "report",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(320, easing = FluidDecel),
                    initialOffset = { it / 4 }
                ) + fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = FluidAccel),
                    targetOffset = { it / 3 }
                ) + fadeOut(animationSpec = tween(180))
            }
        ) {
            val result = uiState.currentResult ?: com.example.model.OnionResult.empty()
            ReportScreen(
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
                onToggleFavorite = { viewModel.toggleFavoriteCurrentReport() },
                onDeleteReport = {
                    viewModel.deleteCurrentReport {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(
            route = "about",
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(320, easing = FluidDecel),
                    initialOffset = { it / 4 }
                ) + fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = FluidAccel),
                    targetOffset = { it / 3 }
                ) + fadeOut(animationSpec = tween(180))
            }
        ) {
            com.example.ui.screens.AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
