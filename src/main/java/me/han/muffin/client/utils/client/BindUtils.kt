package me.han.muffin.client.utils.client

import me.han.muffin.client.core.Globals
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.abs

object BindUtils {
    private var mouseKeyStates = BooleanArray(0)

    fun getFormattedKeyBind(bind: Int) = if (bind == 0 || bind > 0) Keyboard.getKeyName(bind) else "MOUSE${abs(bind)}"

    fun getConvertedKeyBind(bind: String): Int {
        var modifiedBind = bind
        var finalValue = Keyboard.getKeyIndex(bind)

        if (modifiedBind.startsWith("MOUSE")) {
            modifiedBind = modifiedBind.replace("MOUSE", "")
            finalValue = -modifiedBind.toInt()
        }

        return finalValue
    }


    fun checkIsClicked(value: Int): Boolean {
        if (Globals.mc.currentScreen != null) return false
        if (value == 0 || value == Keyboard.KEY_NONE) return false
        if (value > 0) return Keyboard.isKeyDown(value)
        return Mouse.isButtonDown(abs(value))
    }

    fun checkIsClickedToggle(value: Int): Boolean {
        if (Globals.mc.currentScreen != null) return false
        if (value == 0 || value == Keyboard.KEY_NONE) return false
        if (value > 0) return Keyboard.getEventKeyState() && Keyboard.getEventKey() == value
        return Mouse.getEventButtonState() && Mouse.getEventButton() == abs(value)
    }

    fun wasButtonPressed(button: Int, newState: Boolean): Boolean {
        // Check if the state array needs to be expanded
        if (mouseKeyStates.size <= button) {
            // Create a new array with the required size
            val expandedArray = BooleanArray(button + 1)
            // Copy the elements from the old array to the new one
            System.arraycopy(mouseKeyStates, 0, expandedArray, 0, mouseKeyStates.size)
            // Set the state array to the expanded one
            mouseKeyStates = expandedArray
        }

        // true if the mouse is pressed in the new state and wasn't in the old state
        val wasPressed = newState && !mouseKeyStates[button]

        // Set the new state
        mouseKeyStates[button] = newState
        return wasPressed
    }

}