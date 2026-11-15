package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.ExpenseCategory
import com.example.ui.FinTrackerViewModel
import com.example.ui.screens.CategoryBreakdownScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MonthlyGoalOnboardingSheet
import com.example.ui.screens.ProfileAndParserLabScreen
import com.example.ui.screens.StreakModalSheet
import com.example.ui.screens.SubtleAddCaptureSheet
import com.example.ui.theme.FinTrackerTheme
import kotlinx.coroutines.delay

object FinTrackerRoutes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CATEGORY_DETAIL = "category_detail/{categoryName}"
    const val PROFILE_HUB = "profile_hub"

    fun categoryDetail(category: ExpenseCategory): String =
        "category_detail/${category.name}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedText = extractSharedText(intent)
        setContent {
            FinTrackerTheme {
                FinTrackerRoot(initialSharedText = sharedText)
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }
}

@Composable
fun FinTrackerRoot(
    initialSharedText: String? = null,
    viewModel: FinTrackerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showStreakSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            viewModel.handleIncomingSharedText(initialSharedText)
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3200)
            viewModel.clearStatusBanner()
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true
        viewModel.onSmsPermissionResult(granted)
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importCsvFromUri(context, it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let { viewModel.exportCsv(context, it) }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        uri?.let { viewModel.exportPdf(context, it) }
    }

    NavHost(
        navController = navController,
        startDestination = FinTrackerRoutes.HOME,
    ) {
        composable(
            route = FinTrackerRoutes.HOME,
            enterTransition = {
                fadeIn(tween(240)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                )
            },
            exitTransition = {
                fadeOut(tween(200)) + scaleOut(targetScale = 1.04f)
            },
        ) {
            HomeScreen(
                uiState = uiState,
                onCircleClick = {
                    navController.navigate(FinTrackerRoutes.CATEGORIES) {
                        launchSingleTop = true
                    }
                },
                onSubtleAddClick = { showAddSheet = true },
                onSetGoalClick = { viewModel.reopenMonthlyGoalPrompt() },
                onStreakClick = { showStreakSheet = true },
                onProfileClick = {
                    viewModel.refreshPermissionsState()
                    navController.navigate(FinTrackerRoutes.PROFILE_HUB) {
                        launchSingleTop = true
                    }
                },
                onCategoryClick = { category ->
                    navController.navigate(FinTrackerRoutes.categoryDetail(category)) {
                        launchSingleTop = true
                    }
                },
                onSimulateBankSms = { count ->
                    viewModel.simulateIncomingBankSmsBatch(count)
                },
            )
        }

        composable(
            route = FinTrackerRoutes.CATEGORIES,
            enterTransition = {
                fadeIn(tween(260)) + scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            },
            exitTransition = {
                fadeOut(tween(180))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(240),
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                fadeOut(tween(200)) + scaleOut(targetScale = 0.92f)
            },
        ) {
            CategoryBreakdownScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    navController.navigate(FinTrackerRoutes.categoryDetail(category)) {
                        launchSingleTop = true
                    }
                },
                onOpenGoalEditor = { viewModel.reopenMonthlyGoalPrompt() },
            )
        }

        composable(
            route = FinTrackerRoutes.CATEGORY_DETAIL,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220),
                ) + fadeOut(tween(180))
            },
        ) { backStackEntry ->
            val catArg = backStackEntry.arguments?.getString("categoryName").orEmpty()
            val category = ExpenseCategory.fromName(catArg)
            val summary = uiState.categorySummaries.first { it.category == category }
            val catTransactions = uiState.transactions.filter { it.categoryName == category.name }

            CategoryDetailScreen(
                category = category,
                summary = summary,
                transactions = catTransactions,
                onBack = { navController.popBackStack() },
                onRecategorize = { tx, newCat ->
                    viewModel.recategorizeTransaction(tx, newCat)
                },
                onDeleteTransaction = { id ->
                    viewModel.deleteTransaction(id)
                },
                onUpdateCategoryLimit = { newLimit ->
                    viewModel.updateCategoryBudget(category, newLimit)
                },
            )
        }

        composable(
            route = FinTrackerRoutes.PROFILE_HUB,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(220),
                ) + fadeOut(tween(180))
            },
        ) {
            ProfileAndParserLabScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onRequestSmsPermission = {
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                        ),
                    )
                },
                onScanSmsInboxNow = { viewModel.scanSmsInbox(silentIfEmpty = false) },
                onSimulateSmsBatch = { count -> viewModel.simulateIncomingBankSmsBatch(count) },
                onOpenNotificationListenerSettings = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                        )
                    }
                },
                onParserLabInputChange = { viewModel.updateParserLabInput(it) },
                onSaveParserLabToLedger = { viewModel.saveParserLabTraceToLedger() },
                onImportCsv = {
                    importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
                },
                onExportCsv = {
                    exportCsvLauncher.launch("FinTracker_${uiState.monthKey}.csv")
                },
                onExportPdf = {
                    exportPdfLauncher.launch("FinTracker_${uiState.monthKey}.pdf")
                },
                onEditMonthlyGoal = { viewModel.reopenMonthlyGoalPrompt() },
                onClearAllData = { viewModel.clearAllLocalData() },
            )
        }
    }

    if (uiState.showMonthlyGoalPrompt) {
        MonthlyGoalOnboardingSheet(
            monthTitle = uiState.monthTitle,
            suggestedGoalInr = if (uiState.hasMonthlyGoal) uiState.monthlyGoalInr else uiState.suggestedGoalInr,
            hasSmsPermission = uiState.hasSmsPermission,
            onSaveGoal = { goal -> viewModel.saveMonthlyGoal(goal) },
            onRequestSmsPermission = {
                smsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                    ),
                )
            },
            onOpenParserLab = {
                viewModel.dismissMonthlyGoalPrompt()
                navController.navigate(FinTrackerRoutes.PROFILE_HUB) {
                    launchSingleTop = true
                }
            },
            onDismiss = { viewModel.dismissMonthlyGoalPrompt() },
        )
    }

    if (showStreakSheet) {
        StreakModalSheet(
            uiState = uiState,
            onDismiss = { showStreakSheet = false },
        )
    }

    if (showAddSheet) {
        SubtleAddCaptureSheet(
            hasSmsPermission = uiState.hasSmsPermission,
            onPreviewMl = { merchant, note -> viewModel.previewMlCategory(merchant, note) },
            onQuickLog = { amt, merchant, overrideCat, note ->
                viewModel.addQuickExpense(amt, merchant, overrideCat, note)
            },
            onParseAndSaveMessage = { text -> viewModel.handleIncomingSharedText(text) },
            onScanSmsNow = {
                if (uiState.hasSmsPermission) {
                    viewModel.scanSmsInbox(silentIfEmpty = false)
                } else {
                    smsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                        ),
                    )
                }
            },
            onImportCsv = {
                importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
            },
            onOpenParserLab = {
                navController.navigate(FinTrackerRoutes.PROFILE_HUB) {
                    launchSingleTop = true
                }
            },
            onDismiss = { showAddSheet = false },
        )
    }
}
