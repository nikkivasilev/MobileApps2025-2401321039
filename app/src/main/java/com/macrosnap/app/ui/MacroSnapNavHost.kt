package com.macrosnap.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.macrosnap.app.ui.progress.ProgressScreen
import com.macrosnap.app.ui.recipes.RecipeEditorScreen
import com.macrosnap.app.ui.recipes.RecipesScreen
import com.macrosnap.app.ui.review.ReviewScreen
import com.macrosnap.app.ui.settings.SettingsScreen
import com.macrosnap.app.ui.supplements.SupplementEditorScreen
import com.macrosnap.app.ui.theme.AppIcons
import com.macrosnap.app.ui.today.TodayScreen
import com.macrosnap.app.ui.workouts.ExercisesScreen
import com.macrosnap.app.ui.workouts.RoutineEditorScreen
import com.macrosnap.app.ui.workouts.WorkoutScreen
import com.macrosnap.app.ui.workouts.WorkoutsScreen
import kotlin.reflect.KClass

private enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    TODAY("Today", Icons.Filled.Home, TodayRoute, TodayRoute::class),
    WORKOUTS("Gym", AppIcons.Dumbbell, WorkoutsRoute, WorkoutsRoute::class),
    RECIPES("Recipes", Icons.AutoMirrored.Filled.List, RecipesRoute, RecipesRoute::class),
    PROGRESS("Progress", Icons.Filled.DateRange, ProgressRoute, ProgressRoute::class),
    SETTINGS("Settings", Icons.Filled.Settings, SettingsRoute, SettingsRoute::class),
}

@Composable
fun MacroSnapNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { destination?.hasRoute(it.routeClass) == true }

    fun navigateToTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // Each screen owns its top bar and status-bar inset; this scaffold only places the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(item.routeClass) == true,
                            onClick = { navigateToTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable<TodayRoute> {
                TodayScreen(onOpenReview = { navController.navigate(it) })
            }
            composable<WorkoutsRoute> {
                WorkoutsScreen(
                    onOpenWorkout = { navController.navigate(it) },
                    onOpenRoutine = { navController.navigate(it) },
                    onOpenExercises = { navController.navigate(ExercisesRoute) },
                )
            }
            composable<ExercisesRoute> {
                ExercisesScreen(onClose = { navController.popBackStack() })
            }
            composable<WorkoutRoute> {
                WorkoutScreen(onClose = { navController.popBackStack() })
            }
            composable<RoutineEditorRoute> {
                RoutineEditorScreen(onClose = { navController.popBackStack() })
            }
            composable<RecipesRoute> {
                RecipesScreen(
                    onOpenEditor = { navController.navigate(it) },
                    onOpenReview = { navController.navigate(it) },
                )
            }
            composable<RecipeEditorRoute> {
                RecipeEditorScreen(onClose = { navController.popBackStack() })
            }
            composable<ProgressRoute> {
                ProgressScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen(onOpenSupplementEditor = { navController.navigate(it) })
            }
            composable<SupplementEditorRoute> {
                SupplementEditorScreen(onClose = { navController.popBackStack() })
            }
            composable<ReviewRoute> {
                ReviewScreen(
                    onClose = { navController.popBackStack() },
                    onOpenSettings = {
                        navController.popBackStack()
                        navigateToTopLevel(SettingsRoute)
                    },
                )
            }
        }
    }
}
