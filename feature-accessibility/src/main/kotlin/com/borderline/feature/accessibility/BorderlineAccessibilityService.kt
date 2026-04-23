package com.borderline.feature.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.borderline.feature.overlay.BorderlineOverlayController

class BorderlineAccessibilityService : AccessibilityService() {

    private var overlayController: BorderlineOverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info

        overlayController = BorderlineOverlayController(
            context = this,
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        overlayController?.dispose()
        overlayController = null
        super.onDestroy()
    }
}
