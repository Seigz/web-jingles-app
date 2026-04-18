package com.seigz.webjingles.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

enum class GamepadAction {
    NAVIGATE_UP,
    NAVIGATE_DOWN,
    NAVIGATE_LEFT,
    NAVIGATE_RIGHT,
    SELECT,
    BACK,
    PREVIEW,
    DOWNLOAD,
    SCROLL_UP_FAST,
    SCROLL_DOWN_FAST,
    OPEN_SETTINGS,
    FOCUS_SEARCH,
    NONE
}

object GamepadHandler {

    private const val ANALOG_THRESHOLD = 0.5f

    fun handleKeyEvent(keyCode: Int, event: KeyEvent): GamepadAction {
        if (event.action != KeyEvent.ACTION_DOWN) return GamepadAction.NONE

        if (!isGamepad(event.device)) return GamepadAction.NONE

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.NAVIGATE_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.NAVIGATE_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.NAVIGATE_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.NAVIGATE_RIGHT

            KeyEvent.KEYCODE_BUTTON_A -> GamepadAction.SELECT
            KeyEvent.KEYCODE_BUTTON_B -> GamepadAction.BACK
            KeyEvent.KEYCODE_BUTTON_X -> GamepadAction.PREVIEW
            KeyEvent.KEYCODE_BUTTON_Y -> GamepadAction.DOWNLOAD

            KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.SCROLL_UP_FAST
            KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.SCROLL_DOWN_FAST

            KeyEvent.KEYCODE_BUTTON_START -> GamepadAction.OPEN_SETTINGS
            KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadAction.FOCUS_SEARCH

            else -> GamepadAction.NONE
        }
    }

    fun handleMotionEvent(event: MotionEvent): GamepadAction {
        if (event.action != MotionEvent.ACTION_MOVE) return GamepadAction.NONE
        if (!isGamepad(event.device)) return GamepadAction.NONE

        val axisX = event.getAxisValue(MotionEvent.AXIS_X)
        val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        val x = if (kotlin.math.abs(axisX) > kotlin.math.abs(hatX)) axisX else hatX
        val y = if (kotlin.math.abs(axisY) > kotlin.math.abs(hatY)) axisY else hatY

        return when {
            y < -ANALOG_THRESHOLD -> GamepadAction.NAVIGATE_UP
            y > ANALOG_THRESHOLD -> GamepadAction.NAVIGATE_DOWN
            x < -ANALOG_THRESHOLD -> GamepadAction.NAVIGATE_LEFT
            x > ANALOG_THRESHOLD -> GamepadAction.NAVIGATE_RIGHT
            else -> GamepadAction.NONE
        }
    }

    private fun isGamepad(device: InputDevice?): Boolean {
        if (device == null) return false
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
    }
}
