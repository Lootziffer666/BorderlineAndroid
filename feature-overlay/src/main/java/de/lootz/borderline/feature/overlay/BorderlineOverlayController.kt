package de.lootz.borderline.feature.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import de.lootz.borderline.core.AccessibilityStateStore
import de.lootz.borderline.core.BorderlineLogger
import de.lootz.borderline.core.ClipboardGrabber
import de.lootz.borderline.core.JsonSnippetRepository
import de.lootz.borderline.core.JsonTransferItemRepository
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.core.Snippet
import de.lootz.borderline.core.SnippetRepository
import de.lootz.borderline.core.TransferItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BorderlineOverlayController(
    private val context: Context,
    private val modulePrefs: ModulePrefs
) {
    /**
     * Two primary overlay zones (MVP): SNIPPETS on the left edge, CLIPPER on the right.
     */
    enum class HandleZone { SNIPPETS, CLIPPER }

    private data class MenuAction(val label: String, val action: () -> String)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handles = mutableMapOf<HandleZone, View>()
    private var panelView: View? = null
    private val overlayScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateCollectionJob: Job? = null
    private var activeZone: HandleZone? = null
    private var state = OverlaySessionState()
    private var imeDetector: ImeStateDetector? = null
    private var imeVisible = false

    // Persistence
    private val snippetRepo: SnippetRepository = JsonSnippetRepository(context).also { repo ->
        repo.seedDefaults(
            listOf(
                Snippet(title = context.getString(R.string.action_prompt_short), content = context.getString(R.string.prompt_short_clear), category = "prompt"),
                Snippet(title = context.getString(R.string.action_prompt_bullets), content = context.getString(R.string.prompt_bulletpoints), category = "prompt")
            )
        )
    }
    private val transferRepo: TransferItemRepository = JsonTransferItemRepository(context)

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
                HandleZone.SNIPPETS -> context.getString(R.string.handle_label_snippets)
                HandleZone.CLIPPER -> context.getString(R.string.handle_label_clipper)
            }
            val isLeft = zone == HandleZone.SNIPPETS
            view.background = context.getDrawable(
                if (isLeft) R.drawable.bg_edge_handle_left else R.drawable.bg_edge_handle_right
            )
            view.contentDescription = context.getString(R.string.handle_content_desc_format, label.text)

            // Swipe-in or tap to toggle the panel
            @Suppress("ClickableViewAccessibility")
            view.setOnTouchListener(EdgeSwipeDetector(
                context = context,
                isLeftEdge = isLeft,
                onSwipeIn = {
                    performHapticTick(view)
                    togglePanel(zone)
                },
                onTap = {
                    performHapticTick(view)
                    togglePanel(zone)
                }
            ))
            view.setOnLongClickListener {
                performHapticHeavy(view)
                modulePrefs.setEnabled(ModuleId.OVERLAY, false)
                modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
                hideAll()
                Toast.makeText(context, R.string.overlay_disabled_toast, Toast.LENGTH_SHORT).show()
                true
            }
            safeAddView(view, handleParams(zone))
            handles[zone] = view

            // IME detection: register on first handle added
            if (imeDetector == null) {
                imeDetector = ImeStateDetector(view) { visible ->
                    imeVisible = visible
                    repositionHandles()
                }
                imeDetector?.register()
            }
        }
    }

    private fun handleParams(zone: HandleZone): WindowManager.LayoutParams {
        val yCenter = (context.resources.displayMetrics.heightPixels * 0.45f).toInt()
        val yIme = (context.resources.displayMetrics.heightPixels * 0.20f).toInt()
        return baseParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = when (zone) {
                HandleZone.SNIPPETS -> Gravity.START or Gravity.TOP
                HandleZone.CLIPPER -> Gravity.END or Gravity.TOP
            }
            x = 0
            y = if (imeVisible) yIme else yCenter
        }
    }

    /** Re-position both handles when IME visibility changes. */
    private fun repositionHandles() {
        handles.forEach { (zone, view) ->
            try {
                windowManager.updateViewLayout(view, handleParams(zone))
            } catch (e: Exception) {
                BorderlineLogger.w("Failed to reposition handle: ${e.message}")
            }
        }
        // If panel is open, reposition it too
        panelView?.let { panel ->
            activeZone?.let { zone -> repositionPanel(panel, zone) }
        }
    }

    private fun repositionPanel(view: View, zone: HandleZone) {
        try {
            windowManager.updateViewLayout(view, panelParams(zone))
        } catch (e: Exception) {
            BorderlineLogger.w("Failed to reposition panel: ${e.message}")
        }
    }

    private fun togglePanel(zone: HandleZone) {
        if (panelView != null && activeZone == zone) {
            hidePanel()
        } else {
            showPanel(zone)
        }
    }

    private fun panelParams(zone: HandleZone): WindowManager.LayoutParams {
        val yDefault = (context.resources.displayMetrics.heightPixels * 0.18f).toInt()
        val yIme = (context.resources.displayMetrics.heightPixels * 0.04f).toInt()
        return baseParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = when (zone) {
                HandleZone.SNIPPETS -> Gravity.TOP or Gravity.START
                HandleZone.CLIPPER -> Gravity.TOP or Gravity.END
            }
            x = 16
            y = if (imeVisible) yIme else yDefault
        }
    }

    private fun showPanel(zone: HandleZone) {
        hidePanel()

        // Auto-grab clipboard when opening the Clipper panel
        if (zone == HandleZone.CLIPPER) {
            ClipboardGrabber.grab(context)?.let { grabbed ->
                overlayScope.launch { transferRepo.add(grabbed) }
            }
        }

        val view = LayoutInflater.from(context).inflate(R.layout.view_overlay_panel, null)
        val params = panelParams(zone)

        val title = view.findViewById<TextView>(R.id.overlayTitle)
        val status = view.findViewById<TextView>(R.id.overlayStatus)
        val actionButtons = listOf(
            view.findViewById<Button>(R.id.actionOneButton),
            view.findViewById<Button>(R.id.actionTwoButton),
            view.findViewById<Button>(R.id.actionThreeButton),
            view.findViewById<Button>(R.id.actionFourButton)
        )
        val closeButton = view.findViewById<Button>(R.id.closeOverlayButton)

        closeButton.setOnClickListener {
            performHapticTick(view)
            animatePanelOut(view)
        }

        title.text = when (zone) {
            HandleZone.SNIPPETS -> context.getString(R.string.panel_title_snippets)
            HandleZone.CLIPPER -> context.getString(R.string.panel_title_clipper)
        }

        val actions = menuActionsFor(zone)
        actionButtons.forEachIndexed { idx, button ->
            val item = actions.getOrNull(idx)
            if (item == null) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
                button.text = item.label
                button.setOnClickListener {
                    performHapticTick(button)
                    status.text = item.action.invoke()
                }
            }
        }

        stateCollectionJob?.cancel()
        @Suppress("OPT_IN_USAGE")
        stateCollectionJob = AccessibilityStateStore.state
            .debounce(150)
            .onEach { snapshot ->
                val readyText = context.getString(R.string.status_ready)
                if (status.text.isNullOrBlank() || status.text == readyText) {
                    status.text = context.getString(R.string.status_in_app_format, snapshot.packageName)
                }
            }.launchIn(overlayScope)

        safeAddView(view, params)
        panelView = view
        activeZone = zone
        state = state.copy(visible = true)

        animatePanelIn(view, zone)
    }

    private fun menuActionsFor(zone: HandleZone): List<MenuAction> {
        return when (zone) {
            HandleZone.SNIPPETS -> {
                val snippets = snippetRepo.snippets.value
                val list = mutableListOf<MenuAction>()
                // Show up to 3 snippets + an "open Borderline" action
                snippets.take(3).forEach { s ->
                    list.add(MenuAction(s.title) { copyToClipboard(s.content) })
                }
                list.add(MenuAction(context.getString(R.string.action_open_borderline)) { openBorderlineApp() })
                list
            }

            HandleZone.CLIPPER -> {
                val items = transferRepo.items.value
                val list = mutableListOf<MenuAction>()
                if (items.isNotEmpty()) {
                    // Show most-recent clipboard item
                    val latest = items.first()
                    list.add(MenuAction(context.getString(R.string.action_paste_latest)) {
                        copyToClipboard(latest.preview)
                    })
                }
                list.add(MenuAction(context.getString(R.string.action_copy_package)) { copyCurrentPackage() })
                list.add(MenuAction(context.getString(R.string.action_copy_screen)) { copyCurrentScreen() })
                list.add(MenuAction(context.getString(R.string.action_open_app_info)) { openCurrentAppInfo() })
                list.take(4)
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
        Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_SHORT).show()
        return context.getString(R.string.copied_format, text)
    }

    private fun openBorderlineApp(): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launchIntent != null) {
            context.startActivity(launchIntent)
            context.getString(R.string.borderline_opened)
        } else {
            context.getString(R.string.borderline_open_failed)
        }
    }

    private fun openCurrentAppInfo(): String {
        val pkg = AccessibilityStateStore.state.value.packageName
        if (pkg == "unknown") return context.getString(R.string.no_active_app)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return context.getString(R.string.app_info_opened_format, pkg)
    }

    private fun hidePanel() {
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        panelView?.let { safeRemoveView(it) }
        panelView = null
        activeZone = null
        state = state.copy(visible = false)
    }

    private fun hideAll() {
        hidePanel()
        imeDetector?.unregister()
        imeDetector = null
        handles.values.forEach { safeRemoveView(it) }
        handles.clear()
    }

    fun dispose() {
        hideAll()
        overlayScope.cancel()
    }

    // --- Animation helpers ---

    private fun animatePanelIn(view: View, zone: HandleZone) {
        val isLeft = zone == HandleZone.SNIPPETS
        val translationStart = if (isLeft) -40f else 40f

        view.alpha = 0f
        view.translationX = translationStart
        view.scaleX = 0.96f
        view.scaleY = 0.96f

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val slideIn = ObjectAnimator.ofFloat(view, "translationX", translationStart, 0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.96f, 1f)

        AnimatorSet().apply {
            playTogether(fadeIn, slideIn, scaleX, scaleY)
            duration = 200
            interpolator = DecelerateInterpolator(2.0f)
            start()
        }
    }

    private fun animatePanelOut(view: View) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.96f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.96f)

        AnimatorSet().apply {
            playTogether(fadeOut, scaleX, scaleY)
            duration = 150
            interpolator = DecelerateInterpolator(1.5f)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hidePanel()
                }
            })
            start()
        }
    }

    // --- Haptic feedback helpers ---

    private fun performHapticTick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun performHapticHeavy(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // --- Safe WindowManager operations ---

    private fun safeAddView(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            BorderlineLogger.w("Failed to add overlay view: ${e.message}")
        }
    }

    private fun safeRemoveView(view: View) {
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            BorderlineLogger.w("Failed to remove overlay view: ${e.message}")
        }
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
