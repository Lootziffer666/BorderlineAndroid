package de.lootz.borderline.feature.overlay

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

/**
 * Detects whether the soft keyboard (IME) is currently visible by
 * monitoring global layout changes on a root view.
 *
 * @param rootView   any view currently attached to the window (e.g. a handle view)
 * @param onChanged  called with `true` when the keyboard becomes visible, `false` when hidden.
 */
class ImeStateDetector(
    private val rootView: View,
    private val onChanged: (visible: Boolean) -> Unit
) : ViewTreeObserver.OnGlobalLayoutListener {

    private var wasVisible = false

    fun register() {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun unregister() {
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = rootView.rootView.height
        val keyboardHeight = screenHeight - rect.bottom
        val visible = keyboardHeight > screenHeight * KEYBOARD_RATIO
        if (visible != wasVisible) {
            wasVisible = visible
            onChanged(visible)
        }
    }

    companion object {
        /** Fraction of screen height used as threshold to consider the keyboard open. */
        private const val KEYBOARD_RATIO = 0.15
    }
}
