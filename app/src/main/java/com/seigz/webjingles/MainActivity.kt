package com.seigz.webjingles

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.seigz.webjingles.input.GamepadAction
import com.seigz.webjingles.input.GamepadHandler
import com.seigz.webjingles.player.LocalSoundManager
import com.seigz.webjingles.player.SoundManager
import com.seigz.webjingles.ui.navigation.AppNavigation
import com.seigz.webjingles.ui.navigation.Routes
import com.seigz.webjingles.ui.theme.WebJinglesTheme
import com.seigz.webjingles.ui.theme.DarkBackground
import com.seigz.webjingles.viewmodel.DownloadViewModel
import com.seigz.webjingles.viewmodel.PlayerViewModel
import com.seigz.webjingles.viewmodel.SearchViewModel
import com.seigz.webjingles.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private var searchFocusRequester: FocusRequester? = null
    private var currentRoute: String = Routes.HOME
    private var isFullscreen: Boolean = true
    private lateinit var soundManager: SoundManager

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            contentResolver.takePersistableUriPermission(
                selectedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = selectedUri.path ?: selectedUri.toString()
            val friendlyName = path.substringAfterLast(":")
                .ifBlank { path.substringAfterLast("/") }
                .ifBlank { "Selected Folder" }
            settingsViewModel.setDownloadFolder(selectedUri.toString(), friendlyName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        soundManager = SoundManager(this)

        // Observe settings changes
        lifecycleScope.launch {
            settingsViewModel.uiState.collect { state ->
                requestedOrientation = if (state.enablePortrait) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                applyFullscreenMode(state.fullscreenMode)
            }
        }

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()
            val currentDensity = LocalDensity.current
            val scaledDensity = remember(settingsState.uiScale, currentDensity) {
                Density(
                    density = currentDensity.density * settingsState.uiScale,
                    fontScale = currentDensity.fontScale * settingsState.uiScale
                )
            }

            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
                LocalSoundManager provides soundManager
            ) {
                WebJinglesTheme {
                    val focusRequester = remember { FocusRequester() }
                    searchFocusRequester = focusRequester

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkBackground
                    ) {
                        val navController = rememberNavController()

                        DisposableEffect(navController) {
                            val listener =
                                androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                                    currentRoute = destination.route ?: Routes.HOME
                                }
                            navController.addOnDestinationChangedListener(listener)
                            onDispose {
                                navController.removeOnDestinationChangedListener(listener)
                            }
                        }

                        AppNavigation(
                            navController = navController,
                            searchViewModel = searchViewModel,
                            playerViewModel = playerViewModel,
                            downloadViewModel = downloadViewModel,
                            settingsViewModel = settingsViewModel,
                            searchFocusRequester = focusRequester,
                            onChooseFolder = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    private var lastKeyEventTime = 0L
    private val KEY_REPEAT_DELAY = 150L // ms between repeated key events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyDown(keyCode, event)

        // Debounce repeat events to prevent clunky rapid-fire navigation
        if (event.repeatCount > 0) {
            val now = System.currentTimeMillis()
            if (now - lastKeyEventTime < KEY_REPEAT_DELAY) return true
            lastKeyEventTime = now
        } else {
            lastKeyEventTime = System.currentTimeMillis()
        }

        val action = GamepadHandler.handleKeyEvent(keyCode, event)
        if (action != GamepadAction.NONE) {
            handleGamepadAction(action)
            return true
        }

        // Also handle D-pad from non-gamepad sources (keyboard etc.)
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { handleGamepadAction(GamepadAction.NAVIGATE_UP); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { handleGamepadAction(GamepadAction.NAVIGATE_DOWN); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { handleGamepadAction(GamepadAction.NAVIGATE_LEFT); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { handleGamepadAction(GamepadAction.NAVIGATE_RIGHT); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onGenericMotionEvent(event)

        val action = GamepadHandler.handleMotionEvent(event)
        if (action != GamepadAction.NONE) {
            handleGamepadAction(action)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun handleGamepadAction(action: GamepadAction) {
        when (currentRoute) {
            Routes.HOME -> handleHomeAction(action)
            Routes.SETTINGS -> handleSettingsAction(action)
        }
    }

    private fun handleHomeAction(action: GamepadAction) {
        when (action) {
            GamepadAction.NAVIGATE_UP -> searchViewModel.moveSelection(-1)
            GamepadAction.NAVIGATE_DOWN -> searchViewModel.moveSelection(1)
            GamepadAction.SCROLL_UP_FAST -> searchViewModel.moveSelection(-5)
            GamepadAction.SCROLL_DOWN_FAST -> searchViewModel.moveSelection(5)

            GamepadAction.SELECT -> {
                searchViewModel.getSelectedResult()?.let {
                    playerViewModel.playPreview(it)
                }
            }
            GamepadAction.PREVIEW -> {
                searchViewModel.getSelectedResult()?.let {
                    playerViewModel.playPreview(it)
                }
            }
            GamepadAction.DOWNLOAD -> {
                searchViewModel.getSelectedResult()?.let {
                    downloadViewModel.downloadTrack(it)
                }
            }
            GamepadAction.BACK -> {
                playerViewModel.stop()
            }
            GamepadAction.OPEN_SETTINGS -> {
                // Navigate handled via NavController — use callback
            }
            GamepadAction.FOCUS_SEARCH -> {
                try {
                    searchFocusRequester?.requestFocus()
                } catch (_: Exception) { }
            }
            else -> {}
        }
    }

    private fun handleSettingsAction(action: GamepadAction) {
        when (action) {
            GamepadAction.NAVIGATE_UP -> settingsViewModel.moveSelection(-1)
            GamepadAction.NAVIGATE_DOWN -> settingsViewModel.moveSelection(1)
            GamepadAction.BACK -> {
                onBackPressedDispatcher.onBackPressed()
            }
            else -> {}
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isFullscreen) {
            applyFullscreenMode(true)
        }
    }

    private fun applyFullscreenMode(enabled: Boolean) {
        isFullscreen = enabled
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}