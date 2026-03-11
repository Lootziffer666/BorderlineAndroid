package de.lootz.borderline.feature.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import de.lootz.borderline.core.AccessibilityStateStore
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.feature.shortcuts.QuickActionRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BorderlineOverlayController(
    private val context: Context,
    private val modulePrefs: ModulePrefs
) {
    private enum class HandleZone { LEFT_TOP_SAVED, LEFT_BOTTOM_CLIPBOARD, RIGHT_TOP_ACCESSIBILITY, RIGHT_BOTTOM_QUICK }

    private data class MenuAction(val label: String, val action: () -> String)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handles = mutableMapOf<HandleZone, View>()
    private var panelView: View? = null
    private val overlayScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateCollectionJob: Job? = null
    private var activeZone: HandleZone? = null
    private var state = OverlaySessionState()

    fun ensureState() {
        if (!modulePrefs.isEnabled(ModuleId.OVERLAY)) {
            hideAll()
            return
        }
        if (handles.isEmpty()) {
            showHandles()
        }
    }

    private fun showHandles() {
        HandleZone.entries.forEach { zone ->
            val view = LayoutInflater.from(context).inflate(R.layout.view_edge_handle, null)
            val label = view.findViewById<TextView>(R.id.handleLabel)
            label.text = when (zone) {
                HandleZone.LEFT_TOP_SAVED -> "TXT"
                HandleZone.LEFT_BOTTOM_CLIPBOARD -> "CLP"
                HandleZone.RIGHT_TOP_ACCESSIBILITY -> "ACC"
                HandleZone.RIGHT_BOTTOM_QUICK -> "QCK"
            }
            view.background = context.getDrawable(
                if (zone == HandleZone.LEFT_TOP_SAVED || zone == HandleZone.LEFT_BOTTOM_CLIPBOARD) {
                    R.drawable.bg_edge_handle_left
                } else {
                    R.drawable.bg_edge_handle_right
                }
            )
            view.contentDescription = "Borderline ${label.text}"
            view.setOnClickListener { togglePanel(zone) }
            view.setOnLongClickListener {
                modulePrefs.setEnabled(ModuleId.OVERLAY, false)
                modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
                hideAll()
                Toast.makeText(context, "Overlay deaktiviert", Toast.LENGTH_SHORT).show()
                true
            }
            windowManager.addView(view, handleParams(zone))
            handles[zone] = view
        }
    }

    private fun handleParams(zone: HandleZone): WindowManager.LayoutParams {
        val yTop = (context.resources.displayMetrics.heightPixels * 0.24f).toInt()
        val yBottom = (context.resources.displayMetrics.heightPixels * 0.68f).toInt()
        return baseParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = when (zone) {
                HandleZone.LEFT_TOP_SAVED,
                HandleZone.LEFT_BOTTOM_CLIPBOARD -> Gravity.START or Gravity.TOP

                HandleZone.RIGHT_TOP_ACCESSIBILITY,
                HandleZone.RIGHT_BOTTOM_QUICK -> Gravity.END or Gravity.TOP
            }
            x = -10
            y = if (zone == HandleZone.LEFT_TOP_SAVED || zone == HandleZone.RIGHT_TOP_ACCESSIBILITY) yTop else yBottom
        }
    }

    private fun togglePanel(zone: HandleZone) {
        if (panelView != null && activeZone == zone) {
            hidePanel()
        } else {
            showPanel(zone)
        }
    }

    private fun showPanel(zone: HandleZone) {
        hidePanel()
        val view = LayoutInflater.from(context).inflate(R.layout.view_overlay_panel, null)
        val params = baseParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = when (zone) {
                HandleZone.LEFT_TOP_SAVED,
                HandleZone.LEFT_BOTTOM_CLIPBOARD -> Gravity.TOP or Gravity.START

                HandleZone.RIGHT_TOP_ACCESSIBILITY,
                HandleZone.RIGHT_BOTTOM_QUICK -> Gravity.TOP or Gravity.END
            }
            x = 24
            y = if (zone == HandleZone.LEFT_TOP_SAVED || zone == HandleZone.RIGHT_TOP_ACCESSIBILITY) 96 else 360
        }

        val title = view.findViewById<TextView>(R.id.overlayTitle)
        val status = view.findViewById<TextView>(R.id.overlayStatus)
        val actionButtons = listOf(
            view.findViewById<Button>(R.id.actionOneButton),
            view.findViewById<Button>(R.id.actionTwoButton),
            view.findViewById<Button>(R.id.actionThreeButton),
            view.findViewById<Button>(R.id.actionFourButton)
        )
        val closeButton = view.findViewById<Button>(R.id.closeOverlayButton)

        closeButton.setOnClickListener { hidePanel() }

        val menuTitle = when (zone) {
            HandleZone.LEFT_TOP_SAVED -> "Gespeicherte Textblöcke"
            HandleZone.LEFT_BOTTOM_CLIPBOARD -> "Erweiterte Zwischenablage"
            HandleZone.RIGHT_TOP_ACCESSIBILITY -> "Accessibility Shortcuts"
            HandleZone.RIGHT_BOTTOM_QUICK -> "QuickActions"
        }
        title.text = menuTitle

        val actions = menuActionsFor(zone)
        actionButtons.forEachIndexed { idx, button ->
            val item = actions.getOrNull(idx)
            if (item == null) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
                button.text = item.label
                button.setOnClickListener {
                    status.text = item.action.invoke()
                }
            }
        }

        stateCollectionJob?.cancel()
        stateCollectionJob = AccessibilityStateStore.state.onEach { snapshot ->
            if (status.text.isNullOrBlank() || status.text == "Bereit") {
                status.text = "In ${snapshot.packageName}"
            }
        }.launchIn(overlayScope)

        windowManager.addView(view, params)
        panelView = view
        activeZone = zone
        state = state.copy(visible = true)
    }

    private fun menuActionsFor(zone: HandleZone): List<MenuAction> {
        return when (zone) {
            HandleZone.LEFT_TOP_SAVED -> listOf(
                MenuAction("Prompt: Kurz & klar") { copyToClipboard("Antworte kurz, klar und strukturiert.") },
                MenuAction("Prompt: Bulletpoints") { copyToClipboard("Fasse das in maximal 5 Bulletpoints zusammen.") },
                MenuAction("Borderline öffnen") { openBorderlineApp() },
                MenuAction("Aktive App kopieren") { copyCurrentPackage() }
            )

            HandleZone.LEFT_BOTTOM_CLIPBOARD -> listOf(
                MenuAction("Paket kopieren") { copyCurrentPackage() },
                MenuAction("Screen kopieren") { copyCurrentScreen() },
                MenuAction("App-Info öffnen") { openCurrentAppInfo() },
                MenuAction("Text exportieren") { copyToClipboard("Export: ${AccessibilityStateStore.state.value.packageName}") }
            )

            HandleZone.RIGHT_TOP_ACCESSIBILITY -> listOf(
                MenuAction("Accessibility öffnen") { openAccessibilitySettings() },
                MenuAction("Bedienungshilfen-Button") { openAccessibilityButtonSettings() },
                MenuAction("Borderline öffnen") { openBorderlineApp() },
                MenuAction("Gesten: Hinweis") { "Navigationsgesten bleiben OEM-begrenzt (Android-Sicherheitsgrenze)." }
            )

            HandleZone.RIGHT_BOTTOM_QUICK -> {
                val shortcutsEnabled = modulePrefs.isEnabled(ModuleId.SHORTCUTS)
                val quickActions = if (shortcutsEnabled) QuickActionRegistry(context).actions() else emptyList()
                listOf(
                    MenuAction(quickActions.getOrNull(0)?.label ?: "Borderline öffnen") {
                        quickActions.getOrNull(0)?.handler?.invoke() ?: openBorderlineApp()
                    },
                    MenuAction(quickActions.getOrNull(1)?.label ?: "Accessibility öffnen") {
                        quickActions.getOrNull(1)?.handler?.invoke() ?: openAccessibilitySettings()
                    },
                    MenuAction("Kontext-App öffnen") { openCurrentAppInfo() },
                    MenuAction("QuickActions Status") {
                        if (shortcutsEnabled) "QuickActions aktiv" else "QuickActions aus"
                    }
                )
            }
        }
    }

    private fun copyCurrentPackage(): String {
        val packageName = AccessibilityStateStore.state.value.packageName
        return copyToClipboard(packageName)
    }

    private fun copyCurrentScreen(): String {
        val className = AccessibilityStateStore.state.value.className
        return copyToClipboard(className)
    }

    private fun copyToClipboard(text: String): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("borderline", text))
        Toast.makeText(context, "Kopiert", Toast.LENGTH_SHORT).show()
        return "Kopiert: $text"
    }

    private fun openBorderlineApp(): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchIntent != null) {
            context.startActivity(launchIntent)
            "Borderline geöffnet"
        } else {
            "Konnte Borderline nicht öffnen"
        }
    }

    private fun openAccessibilitySettings(): String {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Accessibility-Einstellungen geöffnet"
    }

    private fun openAccessibilityButtonSettings(): String {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Öffne Bedienungshilfen → Schaltfläche 'Bedienungshilfen'"
    }

    private fun openCurrentAppInfo(): String {
        val pkg = AccessibilityStateStore.state.value.packageName
        if (pkg == "unknown") return "Noch keine aktive App erkannt"
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "App-Info geöffnet: $pkg"
    }

    private fun hidePanel() {
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        panelView?.let { windowManager.removeView(it) }
        panelView = null
        activeZone = null
        state = state.copy(visible = false)
    }

    private fun hideAll() {
        hidePanel()
        handles.values.forEach { windowManager.removeView(it) }
        handles.clear()
    }

    fun dispose() {
        hideAll()
        overlayScope.cancel()
    }

    private fun baseParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }
}
