package com.seigz.webjingles.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.seigz.webjingles.ui.screens.HomeScreen
import com.seigz.webjingles.ui.screens.SettingsScreen
import com.seigz.webjingles.viewmodel.DownloadViewModel
import com.seigz.webjingles.viewmodel.PlayerViewModel
import com.seigz.webjingles.viewmodel.SearchViewModel
import com.seigz.webjingles.viewmodel.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    downloadViewModel: DownloadViewModel,
    settingsViewModel: SettingsViewModel,
    searchFocusRequester: FocusRequester,
    onChooseFolder: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                searchViewModel = searchViewModel,
                playerViewModel = playerViewModel,
                downloadViewModel = downloadViewModel,
                settingsViewModel = settingsViewModel,
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                searchFocusRequester = searchFocusRequester
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onChooseFolder = onChooseFolder
            )
        }
    }
}
