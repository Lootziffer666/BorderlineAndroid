package com.borderline.feature.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.borderline.core.models.ContextType
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

interface ContextListener {
    fun onContextChanged(context: ContextType)
}

class ContextAnalyzer(
    private val accessibilityService: AccessibilityService,
    listener: ContextListener
) {
    @Volatile
    private var currentContext: ContextType = ContextType.UNKNOWN

    private val observers = CopyOnWriteArrayList<ContextListener>()
    private val running = AtomicBoolean(false)
    private var monitorThread: Thread? = null

    init {
        observers.add(listener)
        startMonitoring()
    }

    fun startMonitoring() {
        if (running.getAndSet(true)) return

        monitorThread = Thread {
            while (running.get()) {
                try {
                    val newContext = analyzeCurrentContext()
                    if (newContext != currentContext) {
                        currentContext = newContext
                        notifyListeners(newContext)
                    }
                    Thread.sleep(500)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    running.set(false)
                } catch (_: Exception) {
                }
            }
        }.apply {
            name = "ContextAnalyzer"
            isDaemon = true
            start()
        }
    }

    fun stopMonitoring() {
        running.set(false)
        monitorThread?.interrupt()
        monitorThread = null
    }

    fun addListener(listener: ContextListener) {
        observers.add(listener)
    }

    fun removeListener(listener: ContextListener) {
        observers.remove(listener)
    }

    fun getCurrentContext(): ContextType = currentContext

    private fun analyzeCurrentContext(): ContextType {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return ContextType.UNKNOWN

            when {
                hasVisibleInputMethod() -> ContextType.TEXT_EDITING
                hasSelectedText(rootNode) -> ContextType.READING
                isNavigationApp(rootNode.packageName?.toString().orEmpty()) -> ContextType.NAVIGATION
                isMediaApp(rootNode.packageName?.toString().orEmpty()) -> ContextType.MEDIA_VIEWING
                else -> ContextType.UNKNOWN
            }
        } catch (_: Exception) {
            ContextType.UNKNOWN
        }
    }

    private fun hasVisibleInputMethod(): Boolean {
        val imm = accessibilityService.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        return imm?.isAcceptingText == true
    }

    private fun hasSelectedText(node: AccessibilityNodeInfo?): Boolean {
        node ?: return false

        val selectionFound =
            node.textSelectionStart >= 0 &&
            node.textSelectionEnd > node.textSelectionStart

        if (selectionFound) return true

        for (i in 0 until node.childCount) {
            if (hasSelectedText(node.getChild(i))) {
                return true
            }
        }

        return false
    }

    private fun isNavigationApp(packageName: String): Boolean {
        return packageName.contains("maps", ignoreCase = true) ||
            packageName.contains("navigation", ignoreCase = true) ||
            packageName.contains("waze", ignoreCase = true)
    }

    private fun isMediaApp(packageName: String): Boolean {
        return packageName.contains("gallery", ignoreCase = true) ||
            packageName.contains("photos", ignoreCase = true) ||
            packageName.contains("camera", ignoreCase = true) ||
            packageName.contains("video", ignoreCase = true)
    }

    private fun notifyListeners(context: ContextType) {
        observers.forEach { it.onContextChanged(context) }
    }
}
