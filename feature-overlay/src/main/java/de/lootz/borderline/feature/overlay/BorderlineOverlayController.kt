package de.lootz.borderline.feature.overlay

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
import de.lootz.borderline.core.TransferItem
import de.lootz.borderline.core.TransferItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BorderlineOverlayController(
    private val context: Context,
    private val modulePrefs: ModulePrefs
) {
    enum class HandleZone { SNIPPETS, CLIPPER }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handles = mutableMapOf<HandleZone, View>()
    private var panelView: View? = null
    private val overlayScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeZone: HandleZone? = null
    private var state = OverlaySessionState()
    private var imeDetector: ImeStateDetector? = null
    private var imeVisible = false
    private var panelFocusable = false

    private var editingSnippetId: String? = null

    // Persistence
    private val snippetRepo: SnippetRepository = JsonSnippetRepository(context)
    private val transferRepo: TransferItemRepository = JsonTransferItemRepository(context)

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Active breathing animators per handle — cancelled on touch, resumed on idle. */
    private val breatheAnimators = mutableMapOf<HandleZone, ValueAnimator>()

    /** Active panel-in/out animator — cancelled if panel toggles rapidly. */
    private var panelAnimator: AnimatorSet? = null

    init {
        (snippetRepo as? JsonSnippetRepository)?.seedDefaults(
            listOf(
                Snippet(title = context.getString(R.string.action_prompt_short), content = context.getString(R.string.prompt_short_clear), category = "prompt"),
                Snippet(title = context.getString(R.string.action_prompt_bullets), content = context.getString(R.string.prompt_bulletpoints), category = "prompt")
            )
        )
    }

    fun ensureState() {
        if (!modulePrefs.isEnabled(ModuleId.OVERLAY)) {
            hideAll()
            return
        }
        if (handles.isEmpty()) {
            showHandles()
        }
    }

    // ── Handles ──────────────────────────────────────────────

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

            val haloOverlay = view.findViewById<View>(R.id.handleHalo)

            @Suppress("ClickableViewAccessibility")
            view.setOnTouchListener(object : View.OnTouchListener {
                private var isPressed = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            stopBreathing(zone)
                            showHalo(haloOverlay)
                            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_LIGHT)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isPressed) {
                                isPressed = false
                                hideHalo(haloOverlay)
                                startBreathing(zone, view)
                            }
                        }
                    }
                    // Delegate gesture detection
                    return gestureDetector(zone, isLeft).onTouchEvent(event)
                }
            })

            view.setOnLongClickListener {
                BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_REJECT)
                modulePrefs.setEnabled(ModuleId.OVERLAY, false)
                modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
                hideAll()
                Toast.makeText(context, R.string.overlay_disabled_toast, Toast.LENGTH_SHORT).show()
                true
            }

            safeAddView(view, handleParams(zone))
            handles[zone] = view

            // Start idle breathing
            startBreathing(zone, view)

            if (imeDetector == null) {
                imeDetector = ImeStateDetector(view) { visible ->
                    imeVisible = visible
                    repositionHandles()
                }
                imeDetector?.register()
            }
        }
    }

    private fun gestureDetector(zone: HandleZone, isLeft: Boolean) = EdgeSwipeDetector(
        context = context,
        isLeftEdge = isLeft,
        onSwipeIn = {
            BorderlineMotion.haptic(handles[zone]!!, BorderlineMotion.HAPTIC_CONFIRM)
            togglePanel(zone)
        },
        onTap = {
            BorderlineMotion.haptic(handles[zone]!!, BorderlineMotion.HAPTIC_LIGHT)
            togglePanel(zone)
        }
    )

    // ── Handle breathing ─────────────────────────────────────

    private fun startBreathing(zone: HandleZone, view: View) {
        stopBreathing(zone)
        val animator = BorderlineMotion.startHandleBreathing(view)
        breatheAnimators[zone] = animator
    }

    private fun stopBreathing(zone: HandleZone) {
        breatheAnimators[zone]?.cancel()
        breatheAnimators.remove(zone)
    }

    private fun dimHandles() {
        handles.forEach { (zone, view) ->
            stopBreathing(zone)
            view.animate()
                .alpha(BorderlineMotion.HANDLE_DIMMED_ALPHA)
                .setDuration(BorderlineMotion.PANEL_OUT_DURATION)
                .start()
        }
    }

    private fun restoreHandles() {
        handles.forEach { (zone, view) ->
            view.animate()
                .alpha(BorderlineMotion.HANDLE_IDLE_ALPHA_MAX)
                .setDuration(BorderlineMotion.PANEL_IN_DURATION)
                .withEndAction { startBreathing(zone, view) }
                .start()
        }
    }

    // ── Halo effect ──────────────────────────────────────────

    private fun showHalo(halo: View?) {
        halo?.animate()?.cancel()
        halo?.alpha = 0f
        halo?.visibility = View.VISIBLE
        halo?.animate()
            ?.alpha(1f)
            ?.setDuration(BorderlineMotion.HALO_IN_DURATION)
            ?.start()
    }

    private fun hideHalo(halo: View?) {
        halo?.animate()?.cancel()
        halo?.animate()
            ?.alpha(0f)
            ?.setDuration(BorderlineMotion.HALO_OUT_DURATION)
            ?.withEndAction { halo?.visibility = View.INVISIBLE }
            ?.start()
    }

    // ── Handle layout params ─────────────────────────────────

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

    private fun repositionHandles() {
        handles.forEach { (zone, view) ->
            try {
                windowManager.updateViewLayout(view, handleParams(zone))
            } catch (e: Exception) {
                BorderlineLogger.w("Failed to reposition handle: ${e.message}")
            }
        }
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

    // ── Panel lifecycle ──────────────────────────────────────

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
            if (panelFocusable) {
                flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            }
        }
    }

    private fun showPanel(zone: HandleZone) {
        // Cancel any running panel animation
        panelAnimator?.cancel()
        hidePanel()

        val view = when (zone) {
            HandleZone.SNIPPETS -> buildSnippetPanel()
            HandleZone.CLIPPER -> buildClipperPanel()
        }

        panelFocusable = false
        val params = panelParams(zone)
        safeAddView(view, params)
        panelView = view
        activeZone = zone
        state = state.copy(visible = true)

        // Apply glass blur on API 31+
        GlassEffect.apply(view)

        // Dim handles while panel is open
        dimHandles()

        // Calm entrance: 160ms fade + 8dp translate, no scale
        val isLeft = zone == HandleZone.SNIPPETS
        val translateDx = if (isLeft) -1f else 1f
        panelAnimator = BorderlineMotion.panelIn(view, translateDx).also { it.start() }
    }

    private fun setPanelFocusable(focusable: Boolean) {
        if (panelFocusable == focusable) return
        panelFocusable = focusable
        panelView?.let { view ->
            activeZone?.let { zone ->
                try {
                    windowManager.updateViewLayout(view, panelParams(zone))
                } catch (e: Exception) {
                    BorderlineLogger.w("Failed to update panel focus: ${e.message}")
                }
            }
        }
    }

    private fun hidePanel() {
        panelView?.let { safeRemoveView(it) }
        panelView = null
        activeZone = null
        panelFocusable = false
        editingSnippetId = null
        state = state.copy(visible = false)
        restoreHandles()
    }

    private fun hideAll() {
        panelAnimator?.cancel()
        panelAnimator = null
        hidePanel()
        imeDetector?.unregister()
        imeDetector = null
        breatheAnimators.values.forEach { it.cancel() }
        breatheAnimators.clear()
        handles.values.forEach { safeRemoveView(it) }
        handles.clear()
    }

    fun dispose() {
        hideAll()
        overlayScope.cancel()
    }

    // ── Snippet panel ────────────────────────────────────────

    private fun buildSnippetPanel(): View {
        val view = LayoutInflater.from(context).inflate(R.layout.view_snippet_panel, null)

        val closeBtn = view.findViewById<View>(R.id.snippetCloseButton)
        val searchField = view.findViewById<EditText>(R.id.snippetSearchField)
        val listContainer = view.findViewById<LinearLayout>(R.id.snippetListContainer)
        val scrollView = view.findViewById<ScrollView>(R.id.snippetScrollView)
        val emptyState = view.findViewById<TextView>(R.id.snippetEmptyState)
        val addButton = view.findViewById<View>(R.id.snippetAddButton)

        scrollView.post {
            val maxH = (context.resources.displayMetrics.heightPixels * 0.40f).toInt()
            if (scrollView.height > maxH) {
                scrollView.layoutParams = scrollView.layoutParams.apply { height = maxH }
            }
        }

        closeBtn.setOnClickListener {
            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_LIGHT)
            animatePanelOut(view)
        }

        addButton.setOnClickListener {
            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_LIGHT)
            showSnippetEditForm(view, null)
        }

        refreshSnippetList(view, null)

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()
                refreshSnippetList(view, query)
            }
        })

        searchField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) setPanelFocusable(true)
        }

        return view
    }

    private fun refreshSnippetList(panelView: View, query: String?) {
        val listContainer = panelView.findViewById<LinearLayout>(R.id.snippetListContainer)
        val emptyState = panelView.findViewById<TextView>(R.id.snippetEmptyState)

        listContainer.removeAllViews()

        val allSnippets = snippetRepo.snippets.value
        val filtered = if (query.isNullOrBlank()) {
            allSnippets
        } else {
            val q = query.lowercase()
            allSnippets.filter {
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }
        }

        if (filtered.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            listContainer.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            listContainer.visibility = View.VISIBLE
            filtered.forEach { snippet -> addSnippetItemView(panelView, listContainer, snippet) }
        }
    }

    private fun addSnippetItemView(panelView: View, container: LinearLayout, snippet: Snippet) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.view_snippet_item, container, false)
        val titleView = itemView.findViewById<TextView>(R.id.snippetItemTitle)
        val previewView = itemView.findViewById<TextView>(R.id.snippetItemPreview)
        val editBtn = itemView.findViewById<View>(R.id.snippetItemEdit)
        val deleteBtn = itemView.findViewById<View>(R.id.snippetItemDelete)

        titleView.text = snippet.title
        previewView.text = snippet.content.take(PREVIEW_LENGTH)

        itemView.findViewById<View>(R.id.snippetItemContent).setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_CONFIRM)
            copyToClipboard(snippet.content)
        }

        editBtn.setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_LIGHT)
            showSnippetEditForm(panelView, snippet)
        }

        deleteBtn.setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_REJECT)
            overlayScope.launch {
                snippetRepo.delete(snippet.id)
                refreshSnippetList(panelView, currentSearchQuery(panelView))
            }
            Toast.makeText(context, R.string.snippet_deleted, Toast.LENGTH_SHORT).show()
        }

        container.addView(itemView)
    }

    private fun currentSearchQuery(panelView: View): String? {
        return panelView.findViewById<EditText>(R.id.snippetSearchField)?.text?.toString()?.trim()
            .takeIf { !it.isNullOrBlank() }
    }

    private fun showSnippetEditForm(panelView: View, snippet: Snippet?) {
        val editContainer = panelView.findViewById<View>(R.id.snippetEditContainer)
        val listSection = panelView.findViewById<View>(R.id.snippetScrollView)
        val addButton = panelView.findViewById<View>(R.id.snippetAddButton)
        val searchField = panelView.findViewById<View>(R.id.snippetSearchField)
        val emptyState = panelView.findViewById<View>(R.id.snippetEmptyState)
        val editLabel = panelView.findViewById<TextView>(R.id.snippetEditLabel)
        val titleField = panelView.findViewById<EditText>(R.id.snippetEditTitle)
        val contentField = panelView.findViewById<EditText>(R.id.snippetEditContent)
        val cancelBtn = panelView.findViewById<View>(R.id.snippetEditCancel)
        val saveBtn = panelView.findViewById<View>(R.id.snippetEditSave)

        listSection.visibility = View.GONE
        addButton.visibility = View.GONE
        searchField.visibility = View.GONE
        emptyState.visibility = View.GONE
        editContainer.visibility = View.VISIBLE

        editingSnippetId = snippet?.id

        if (snippet != null) {
            editLabel.text = context.getString(R.string.snippet_edit)
            titleField.setText(snippet.title)
            contentField.setText(snippet.content)
        } else {
            editLabel.text = context.getString(R.string.snippet_new)
            titleField.text.clear()
            contentField.text.clear()
        }

        setPanelFocusable(true)
        titleField.requestFocus()

        cancelBtn.setOnClickListener {
            BorderlineMotion.haptic(panelView, BorderlineMotion.HAPTIC_LIGHT)
            hideSnippetEditForm(panelView)
        }

        saveBtn.setOnClickListener {
            val title = titleField.text.toString().trim()
            val content = contentField.text.toString().trim()

            if (title.isEmpty()) {
                BorderlineMotion.haptic(panelView, BorderlineMotion.HAPTIC_REJECT)
                Toast.makeText(context, R.string.snippet_title_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                BorderlineMotion.haptic(panelView, BorderlineMotion.HAPTIC_REJECT)
                Toast.makeText(context, R.string.snippet_content_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            BorderlineMotion.haptic(panelView, BorderlineMotion.HAPTIC_CONFIRM)
            overlayScope.launch {
                val existingId = editingSnippetId
                if (existingId != null) {
                    val existing = snippetRepo.getById(existingId)
                    if (existing != null) {
                        snippetRepo.update(existing.copy(title = title, content = content))
                    }
                } else {
                    snippetRepo.add(Snippet(title = title, content = content))
                }
                editingSnippetId = null
                hideSnippetEditForm(panelView)
                Toast.makeText(context, R.string.snippet_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideSnippetEditForm(panelView: View) {
        val editContainer = panelView.findViewById<View>(R.id.snippetEditContainer)
        val listSection = panelView.findViewById<View>(R.id.snippetScrollView)
        val addButton = panelView.findViewById<View>(R.id.snippetAddButton)
        val searchField = panelView.findViewById<View>(R.id.snippetSearchField)

        editContainer.visibility = View.GONE
        listSection.visibility = View.VISIBLE
        addButton.visibility = View.VISIBLE
        searchField.visibility = View.VISIBLE

        setPanelFocusable(false)
        editingSnippetId = null
        refreshSnippetList(panelView, currentSearchQuery(panelView))
    }

    // ── Clipper panel ────────────────────────────────────────

    private fun buildClipperPanel(): View {
        val view = LayoutInflater.from(context).inflate(R.layout.view_clipper_panel, null)

        val closeBtn = view.findViewById<View>(R.id.clipperCloseButton)
        val grabStatus = view.findViewById<View>(R.id.clipperGrabStatus)
        val grabText = view.findViewById<TextView>(R.id.clipperGrabText)
        val copyPackageBtn = view.findViewById<View>(R.id.clipperCopyPackageButton)
        val copyScreenBtn = view.findViewById<View>(R.id.clipperCopyScreenButton)

        closeBtn.setOnClickListener {
            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_LIGHT)
            animatePanelOut(view)
        }

        val grabbed = ClipboardGrabber.grab(context)
        if (grabbed != null) {
            overlayScope.launch { transferRepo.add(grabbed) }
            grabStatus.visibility = View.VISIBLE
            grabText.text = context.getString(R.string.grab_success)
            mainHandler.postDelayed({ grabStatus.visibility = View.GONE }, BorderlineMotion.GRAB_FEEDBACK_MS)
        }

        copyPackageBtn.setOnClickListener {
            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_CONFIRM)
            copyCurrentPackage()
        }
        copyScreenBtn.setOnClickListener {
            BorderlineMotion.haptic(view, BorderlineMotion.HAPTIC_CONFIRM)
            copyCurrentScreen()
        }

        val scrollView = view.findViewById<ScrollView>(R.id.clipperScrollView)
        scrollView.post {
            val maxH = (context.resources.displayMetrics.heightPixels * 0.40f).toInt()
            if (scrollView.height > maxH) {
                scrollView.layoutParams = scrollView.layoutParams.apply { height = maxH }
            }
        }

        refreshClipperList(view)

        return view
    }

    private fun refreshClipperList(panelView: View) {
        val listContainer = panelView.findViewById<LinearLayout>(R.id.clipperListContainer)
        val emptyState = panelView.findViewById<TextView>(R.id.clipperEmptyState)

        listContainer.removeAllViews()

        val items = transferRepo.items.value
        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            listContainer.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            listContainer.visibility = View.VISIBLE
            items.forEach { item -> addTransferItemView(panelView, listContainer, item) }
        }
    }

    private fun addTransferItemView(panelView: View, container: LinearLayout, item: TransferItem) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.view_transfer_item, container, false)
        val previewView = itemView.findViewById<TextView>(R.id.transferItemPreview)
        val timestampView = itemView.findViewById<TextView>(R.id.transferItemTimestamp)
        val pinBtn = itemView.findViewById<ImageView>(R.id.transferItemPin)
        val deleteBtn = itemView.findViewById<View>(R.id.transferItemDelete)

        previewView.text = item.preview.take(PREVIEW_LENGTH)
        timestampView.text = formatRelativeTime(item.timestamp)

        if (item.pinned) {
            pinBtn.alpha = 1.0f
        } else {
            pinBtn.alpha = 0.4f
        }

        itemView.findViewById<View>(R.id.transferItemContent).setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_CONFIRM)
            copyToClipboard(item.preview)
        }

        pinBtn.setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_CONFIRM)
            overlayScope.launch {
                transferRepo.pin(item.id, !item.pinned)
                refreshClipperList(panelView)
            }
            val msg = if (item.pinned) R.string.item_unpinned else R.string.item_pinned
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        deleteBtn.setOnClickListener {
            BorderlineMotion.haptic(itemView, BorderlineMotion.HAPTIC_REJECT)
            overlayScope.launch {
                transferRepo.delete(item.id)
                refreshClipperList(panelView)
            }
            Toast.makeText(context, R.string.item_deleted, Toast.LENGTH_SHORT).show()
        }

        container.addView(itemView)
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.getString(R.string.time_minutes_ago_format, minutes.toInt())
            hours < 24 -> context.getString(R.string.time_hours_ago_format, hours.toInt())
            else -> context.getString(R.string.time_days_ago_format, days.toInt())
        }
    }

    // ── Clipboard helpers ────────────────────────────────────

    private fun copyCurrentPackage() {
        val packageName = AccessibilityStateStore.state.value.packageName
        copyToClipboard(packageName)
    }

    private fun copyCurrentScreen() {
        val className = AccessibilityStateStore.state.value.className
        copyToClipboard(className)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("borderline", text))
        Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_SHORT).show()
    }

    // ── Animation helpers ────────────────────────────────────

    private fun animatePanelOut(view: View) {
        panelAnimator?.cancel()
        dimHandles() // keep dimmed until removal
        panelAnimator = BorderlineMotion.panelOut(view) {
            hidePanel()
        }.also { it.start() }
    }

    // ── Safe WindowManager operations ────────────────────────

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

    companion object {
        private const val PREVIEW_LENGTH = 80
    }
}
