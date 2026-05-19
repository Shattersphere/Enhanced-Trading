package weaponsprocurement.ui

import org.lwjgl.input.Keyboard

/**
 * Edge-triggered keyboard latch used by campaign scripts. This avoids repeated open/close
 * events while Starsector keeps a key down across multiple paused frames.
 */
class WimGuiHotkeyLatch {
    private var activeKeyCode = 0
    private var wasDown = false

    fun consumePress(keyCode: Int): Boolean {
        if (keyCode <= 0) {
            activeKeyCode = keyCode
            wasDown = false
            return false
        }
        if (keyCode != activeKeyCode) {
            activeKeyCode = keyCode
            wasDown = false
        }
        val down = Keyboard.isKeyDown(keyCode)
        if (!down) {
            wasDown = false
            return false
        }
        if (wasDown) return false
        wasDown = true
        return true
    }
}
