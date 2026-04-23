package com.borderline.feature.overlay

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.borderline.core.models.ClipboardContent
import com.borderline.core.models.QuickActionContent
import com.borderline.core.models.QuickActionType
import com.borderline.core.models.ShortcutActionType
import com.borderline.core.models.ShortcutContent
import com.borderline.core.models.SnippetContent
import com.borderline.feature.overlay.panels.ClipboardPanel
import com.borderline.feature.overlay.panels.QuickActionsPanel
import com.borderline.feature.overlay.panels.ShortcutsPanel
import com.borderline.feature.overlay.panels.SmartPanel
import com.borderline.feature.overlay.panels.SnippetsPanel

@SuppressLint("ClickableViewAccessibility")
class BorderlineOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val snippetProvider: () -> List<SnippetContent> = { emptyList() },
    private val clipboardProvider: () -> List<ClipboardContent> = { emptyList() },
    private val shortcutProvider: () -> List<ShortcutContent> = { emptyList() },
    private val quickActionProvider: () -> List<QuickActionContent> = { emptyList() }
) : ImeStateListener {

    private val edgeSwipeDetector = EdgeSwipeDetector()
    private val panelRegistry = mutableMapOf<Int, SmartPanel>()
    private val panelViews = mutableMapOf<Int, View>()
    private var imeDetector: ImeStateDetector? = null
    private var gestureView: View? = null
    private var currentImeHeight = 0

    private lateinit var screenDimensions: ScreenDimensions
    private lateinit var panelPositions: Map<Int, PanelPosition>

    init {
        calculateScreenDimensions()
        setupPanels()
        setupGestureLayer()
    }

    private fun calculateScreenDimensions() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        screenDimensions = ScreenDimensions(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            upper3rdEnd = screenHeight / 3,
            middle3rdStart = screenHeight / 3,
            middle3rdEnd = 2 * screenHeight / 3,
            lower3rdStart = 2 * screenHeight / 3
        )

        updatePanelPositions()
    }

    private fun updatePanelPositions() {
        panelPositions = AccessibilityBarLayout.calculatePanelPositions(
            screenDimensions,
            imeHeight = currentImeHeight
        )
    }

    private fun setupPanels() {
        panelRegistry[1] = SnippetsPanel(
            context = context,
            itemsProvider = snippetProvider,
            onSnippetSelected = { copyToClipboard(it.content) }
        )

        panelRegistry[2] = ClipboardPanel(
            context = context,
            itemsProvider = clipboardProvider,
            onClipboardSelected = { pasteToFocusedApp(it.preview.ifBlank { it.rawContent }) }
        )

        panelRegistry[3] = ShortcutsPanel(
            context = context,
            itemsProvider = shortcutProvider,
            onShortcutSelected = { executeShortcut(it) }
        )

        panelRegistry[4] = QuickActionsPanel(
            context = context,
            itemsProvider = quickActionProvider,
            onQuickActionSelected = { executeQuickAction(it) }
        )

        panelRegistry.forEach { (id, panel) ->
            val view = panel.createView()
            panelViews[id] = view
            addPanelToWindow(id, view)
        }
    }

    private fun addPanelToWindow(panelId: Int, view: View) {
        val position = panelPositions[panelId] ?: return
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = position.width.dp
            height = position.height.dp
            x = position.x
            y = position.y
            gravity = position.gravity
        }
        windowManager.addView(view, params)
    }

    private fun setupGestureLayer() {
        gestureView = View(context).apply {
            setOnTouchListener { _, event ->
                val swipeEvent = edgeSwipeDetector.onMotionEvent(
                    event = event,
                    screenWidth = screenDimensions.screenWidth,
                    screenHeight = screenDimensions.screenHeight
                )

                if (swipeEvent != null) {
                    handleSwipeEvent(swipeEvent)
                    true
                } else {
                    false
                }
            }
        }

        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }

        val gestureView = gestureView ?: return
        windowManager.addView(gestureView, params)
        imeDetector = ImeStateDetector(gestureView, this)
    }

    private fun handleSwipeEvent(event: EdgeSwipeEvent) {
        val panelId = when (event.edge) {
            ScreenEdge.LEFT -> if (event.isTopSwipe(screenDimensions.screenHeight)) 1 else 3
            ScreenEdge.RIGHT -> if (event.isTopSwipe(screenDimensions.screenHeight)) 2 else 4
            ScreenEdge.TOP -> if (event.isTopSwipe(screenDimensions.screenHeight)) 1 else 2
            ScreenEdge.BOTTOM -> if (event.isBottomSwipe(screenDimensions.screenHeight)) 4 else 3
        }

        togglePanel(panelId, panelPositions[panelId]?.edge ?: return)
    }

    private fun togglePanel(panelId: Int, edge: ScreenEdge) {
        val panel = panelRegistry[panelId] ?: return
        val view = panelViews[panelId] ?: return

        if (panel.isVisible) {
            hidePanel(panelId, view)
        } else {
            showPanel(panelId, view, edge)
        }
    }

    private fun showPanel(panelId: Int, view: View, edge: ScreenEdge) {
        val position = panelPositions[panelId] ?: return
        val animator = when (edge) {
            ScreenEdge.LEFT -> ObjectAnimator.ofFloat(view, "translationX", -position.width.dp.toFloat(), 0f)
            ScreenEdge.RIGHT -> ObjectAnimator.ofFloat(view, "translationX", position.width.dp.toFloat(), 0f)
            ScreenEdge.TOP -> ObjectAnimator.ofFloat(view, "translationY", -position.height.dp.toFloat(), 0f)
            ScreenEdge.BOTTOM -> ObjectAnimator.ofFloat(view, "translationY", position.height.dp.toFloat(), 0f)
        }

        view.visibility = View.VISIBLE
        animator.duration = 300
        animator.start()
        panelRegistry[panelId]?.onOpened()
    }

    private fun hidePanel(panelId: Int, view: View) {
        val position = panelPositions[panelId] ?: return
        val edge = position.edge

        val animator = when (edge) {
            ScreenEdge.LEFT -> ObjectAnimator.ofFloat(view, "translationX", 0f, -position.width.dp.toFloat())
            ScreenEdge.RIGHT -> ObjectAnimator.ofFloat(view, "translationX", 0f, position.width.dp.toFloat())
            ScreenEdge.TOP -> ObjectAnimator.ofFloat(view, "translationY", 0f, -position.height.dp.toFloat())
            ScreenEdge.BOTTOM -> ObjectAnimator.ofFloat(view, "translationY", 0f, position.height.dp.toFloat())
        }

        animator.duration = 300
        animator.doOnEnd {
            view.visibility = View.GONE
        }
        animator.start()
        panelRegistry[panelId]?.onClosed()
    }

    override fun onImeShown(keyboardHeight: Int) {
        currentImeHeight = keyboardHeight
        updatePanelPositions()
        repositionAllPanels()
    }

    override fun onImeHidden() {
        currentImeHeight = 0
        updatePanelPositions()
        repositionAllPanels()
    }

    private fun repositionAllPanels() {
        panelViews.forEach { (panelId, view) ->
            val position = panelPositions[panelId] ?: return@forEach
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return@forEach
            params.x = position.x
            params.y = position.y
            params.width = position.width.dp
            params.height = position.height.dp
            params.gravity = position.gravity
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("borderline", text))
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        showFeedback("Kopiert")
    }

    private fun pasteToFocusedApp(text: String) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm?.isAcceptingText == true) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("borderline_paste", text))
            showFeedback("In Zwischenablage gelegt")
        } else {
            showFeedback("Kein aktives Textfeld")
        }
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun executeShortcut(shortcut: ShortcutContent) {
        when (shortcut.actionType) {
            ShortcutActionType.OPEN_APP -> {
                val intent = context.packageManager.getLaunchIntentForPackage(shortcut.target)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) context.startActivity(intent)
            }
            ShortcutActionType.SEND_TEXT -> copyToClipboard(shortcut.target)
            ShortcutActionType.CALL -> {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:${shortcut.target}"))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            ShortcutActionType.WEB_LINK -> {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(shortcut.target))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            ShortcutActionType.CUSTOM -> showFeedback("Custom Shortcut TODO")
        }
    }

    private fun executeQuickAction(action: QuickActionContent) {
        when (action.actionType) {
            QuickActionType.UNDO -> showFeedback("Undo TODO")
            QuickActionType.REDO -> showFeedback("Redo TODO")
            QuickActionType.SCREENSHOT -> showFeedback("Screenshot TODO")
            QuickActionType.DICTATION -> showFeedback("Dictation TODO")
            QuickActionType.CUSTOM -> showFeedback(action.customAction ?: "Custom Action")
        }
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun performHapticFeedback(feedbackType: Int) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrateCompat(50)
    }

    private fun showFeedback(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun dispose() {
        imeDetector?.dispose()
        imeDetector = null

        gestureView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        gestureView = null

        panelViews.values.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        panelViews.clear()
        panelRegistry.clear()
    }
}
