package com.borderline.feature.overlay

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

interface ImeStateListener {
    fun onImeShown(keyboardHeight: Int)
    fun onImeHidden()
}

class ImeStateDetector(
    private val rootView: View,
    private val listener: ImeStateListener
) {

    private var lastImeHeight = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    init {
        setupImeDetection()
    }

    private fun setupImeDetection() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.context.resources.displayMetrics.heightPixels
            val visibleHeight = rect.bottom - rect.top
            val imeHeight = screenHeight - visibleHeight

            if (imeHeight > 100 && lastImeHeight <= 100) {
                lastImeHeight = imeHeight
                listener.onImeShown(imeHeight)
            } else if (imeHeight <= 100 && lastImeHeight > 100) {
                lastImeHeight = 0
                listener.onImeHidden()
            } else {
                lastImeHeight = if (imeHeight > 100) imeHeight else 0
            }
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    fun getCurrentImeHeight(): Int = lastImeHeight

    fun dispose() {
        val listener = globalLayoutListener ?: return
        if (rootView.viewTreeObserver.isAlive) {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        globalLayoutListener = null
    }
}
