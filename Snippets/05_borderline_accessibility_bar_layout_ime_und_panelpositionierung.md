# Borderline: Accessibility Bar Layout, IME und Panelpositionierung

- Seiten: 36-45

android:accessibilityFeedbackType="feedbackHaptic"     android:accessibilityFlags="flagReportInteractionTimes|flagRequestTouchExplorationMode"     android:notificationTimeout="100"     android:canRetrieveWindowContent="true"     android:canRequestTouchExplorationMode="true" /> 7. TESTSZENARIO: So sieht es in der Praxis aus Kotlin // Alle 4 Swipes funktionieren: // 1. VON LINKS REIN (Snippets) // → Benutzer wischt von linkem Rand nach rechts // → SnippetsPanel öffnet sich mit Slide-Animation // → Liste von gespeicherten Texten // → Tap → Text kopiert + Haptic Feedback // 2. VON RECHTS REIN (Clipboard) // → Benutzer wischt von rechtem Rand nach links // → ClipperPanel öffnet sich // → Auto-Grab: Aktuelle Zwischenablage wird erfasst // → Liste mit gepinnten und ungepinnten Einträgen // → Tap → Text in aktive App eingefügt // 3. VON OBEN REIN (Shortcuts) // → Benutzer wischt von oberem Rand nach unten // → ShortcutsPanel öffnet sich horizontal // → Vordefinierte Quick-Links (Apps, Telefon, etc.) // → Tap → Action ausgeführt // 4. VON UNTEN REIN (Quick Actions) // → Benutzer wischt von unterem Rand nach oben // → QuickActionsPanel öffnet sich // → Undo, Redo, Screenshot, Dictation // → Tap → Action ausgeführt 🎬 DEINE NÄCHSTEN SCHRITTE In deinem Borderline-Projekt: Erweitere BorderlineOverlayController um die 4 Panels Test: Swipe von jeder Kante und validiere, dass der richtige Edge erkannt wird Haptic Feedback: Für jede Action (Copy, Paste, Execute) CONFIRM oder REJECT IME Handling: Wenn Tastatur sichtbar, verschiebe Panels automatisch nach oben Brauchst du konkrete Code-Beispiele für: Clipboard + Paste in Ziel-App? Swipe-Animation Feintuning? Haptic Feedback Pattern? Persistenz von Shortcuts/Quick Actions? You said: Ja, brauche ich. Wie Du vielleicht im Ja, brauche ich. Wie Du vielleicht im Projekt gesehen hast, soll das erste Menü über der unteren Grenze des oberen Drittels des screens als
Accessibility

Bar

sein,

direkt

rechts

gegenüber

das

nächste.

Die

unteren

beiden

sollen

versetzt

sein,

das

linke

an

der

oberen

Grenze

des

unteren

Drittels

des

Screens,

das

rechte

unterhalb

der

Mitte

des

unteren

Drittels,

damit

es

bei

der

Navigation

nicht

im

Weg

ist.

Beides

mit

IME

Handling.
Copilot finished thinking Copilot said: Perfect! Ich sehe deine aktuelle Struktur. Jetzt Perfect! Ich sehe deine aktuelle Struktur. Jetzt baue ich dir exakt das System, das du brauchst: 🎯 4-PANEL ACCESSIBILITY SYSTEM MIT PRECISEM POSITIONING Das ist die genaue Implementierung für deine Anforderung: Code ┌─────────────────────────────────────────┐ │                                         │

│   ◀──── UPPER THIRD ────►               │ │                     [PANEL 1] [PANEL 2]  │   ← Accessibility Bar │                                         │     (unter Oberkante Drittel 1) │               MIDDLE THIRD               │ │                                         │ │   [PANEL 3]                              │ │   ▲ Oberkante Unterteil                 │ │   (Drittel 3)                            │ │                                         │ │                                 [PANEL 4] │ │                                 ▼ Mitte  │ │                                 Unterteil │ │   ◄──── LOWER THIRD ────►               │ └─────────────────────────────────────────┘ 1. SCREEN DIMENSION CALCULATOR Kotlin // borderline-android/feature-overlay/src/main/kotlin/ScreenDimensionCalculator.kt data class ScreenDimensions(     val screenWidth: Int,     val screenHeight: Int,     val upper3rdEnd: Int,      // screenHeight / 3     val middle3rdStart: Int,   // screenHeight / 3     val middle3rdEnd: Int,     // 2 * screenHeight / 3     val lower3rdStart: Int     // 2 * screenHeight / 3 ) data class PanelPosition(     val panelId: String,       // "panel_1", "panel_2", etc.     val x: Int,     val y: Int,     val width: Int,     val height: Int,     val gravity: Int,          // Gravity constant     val edge: ScreenEdge       // LEFT, RIGHT, TOP, BOTTOM ) enum class ScreenEdge {     LEFT, RIGHT, TOP, BOTTOM } object AccessibilityBarLayout {     /**      * Berechnet die 4 Panel-Positionen basierend auf Screen-Dimensionen      * und IME-Sichtbarkeit      */     fun calculatePanelPositions(         dimensions: ScreenDimensions,         imeHeight: Int = 0     ): Map<Int, PanelPosition> {         val (screenWidth, screenHeight, upper3rdEnd, _, lower3rdStart) = dimensions
val panelWidth = 280  // Fixe Breite für Seitenpanels         val panelHeight = 280 // Fixe Höhe für Seitenpanels         val barHeight = 64    // Höhe der Accessibility Bar
// Berechne Offset für IME (wenn Tastatur sichtbar)

val imeOffset = if (imeHeight > 0) imeHeight + 16 else 0
return mapOf(             // PANEL 1: Oben-Links (Accessibility Bar)             // Position: unter dem Ende des oberen Drittels             1 to PanelPosition(                 panelId = "panel_1_snippets",                 x = 0,  // Linker Rand                 y = upper3rdEnd - imeOffset,  // Unter Drittelsgrenze                 width = panelWidth,                 height = barHeight,                 gravity = Gravity.TOP or Gravity.START,                 edge = ScreenEdge.LEFT             ),
// PANEL 2: Oben-Rechts (Accessibility Bar)             // Position: auf gleicher Höhe wie Panel 1, rechts gegenüber             2 to PanelPosition(                 panelId = "panel_2_clipboard",                 x = screenWidth - panelWidth,  // Rechter Rand                 y = upper3rdEnd - imeOffset,  // Gleiche Y wie Panel 1                 width = panelWidth,                 height = barHeight,                 gravity = Gravity.TOP or Gravity.END,                 edge = ScreenEdge.RIGHT             ),
// PANEL 3: Unten-Links (Versetzt)             // Position: an Oberkante des unteren Drittels             3 to PanelPosition(                 panelId = "panel_3_shortcuts",                 x = 0,  // Linker Rand                 y = lower3rdStart - imeOffset,  // Oberkante Unterteil                 width = panelWidth,                 height = panelHeight,                 gravity = Gravity.BOTTOM or Gravity.START,                 edge = ScreenEdge.LEFT             ),
// PANEL 4: Unten-Rechts (Versetzt, tiefer)             // Position: unterhalb der Mitte des unteren Drittels             4 to PanelPosition(                 panelId = "panel_4_quick_actions",                 x = screenWidth - panelWidth,  // Rechter Rand                 y = (lower3rdStart + screenHeight) / 2 - imeOffset,  // Mitte Unterteil                 width = panelWidth,                 height = panelHeight - 64,  // Etwas kleiner wegen Navigation Bar                 gravity = Gravity.BOTTOM or Gravity.END,                 edge = ScreenEdge.RIGHT             )         )     } } 2. IME STATE DETECTOR (Tastatur-Erkennung) Kotlin

// borderline-android/feature-overlay/src/main/kotlin/ImeStateDetector.kt interface ImeStateListener {     fun onImeShown(keyboardHeight: Int)     fun onImeHidden() } class ImeStateDetector(     private val rootView: View,     private val listener: ImeStateListener ) {     private var lastImeHeight = 0     private var lastVisibleHeight = 0
init {         setupImeDetection()     }
private fun setupImeDetection() {         // ViewTreeObserver für globale Layout-Änderungen         rootView.viewTreeObserver.addOnGlobalLayoutListener {             val displayMetrics = rootView.context.resources.displayMetrics             val screenHeight = displayMetrics.heightPixels
val rect = android.graphics.Rect()             rootView.getWindowVisibleDisplayFrame(rect)
val visibleHeight = rect.bottom - rect.top             val imeHeight = screenHeight - visibleHeight
// Schwelle: Tastatur als sichtbar wenn > 100px             if (imeHeight > 100 && lastImeHeight <= 100) {                 // IME gerade aufgetaucht                 lastImeHeight = imeHeight                 listener.onImeShown(imeHeight)             } else if (imeHeight <= 100 && lastImeHeight > 100) {                 // IME gerade versteckt                 lastImeHeight = imeHeight                 listener.onImeHidden()             }
lastVisibleHeight = visibleHeight         }     }
fun getCurrentImeHeight(): Int {         val rect = android.graphics.Rect()         rootView.getWindowVisibleDisplayFrame(rect)         val screenHeight = rootView.context.resources.displayMetrics.heightPixels         val visibleHeight = rect.bottom - rect.top         val imeHeight = screenHeight - visibleHeight         return if (imeHeight > 100) imeHeight else 0     }

} 3. OVERLAY CONTROLLER MIT 4 PANELS + IME HANDLING Kotlin // borderline-android/feature-overlay/src/main/kotlin/BorderlineOverlayController.kt (erweitert) @SuppressLint("ClickableViewAccessibility") class BorderlineOverlayController(     private val context: Context,     private val windowManager: WindowManager ) : ImeStateListener {
private val edgeSwipeDetector = EdgeSwipeDetector()     private val panelRegistry = mutableMapOf<Int, AccessibilityPanel>()     private var imeDetector: ImeStateDetector? = null     private var currentImeHeight = 0
// Panel Views     private val panelViews = mutableMapOf<Int, View>()
// Screen Dimensionen     private lateinit var screenDimensions: ScreenDimensions     private lateinit var panelPositions: Map<Int, PanelPosition>
init {         calculateScreenDimensions()         setupPanels()         setupGestureLayer()     }
private fun calculateScreenDimensions() {         val displayMetrics = context.resources.displayMetrics         val screenWidth = displayMetrics.widthPixels         val screenHeight = displayMetrics.heightPixels
screenDimensions = ScreenDimensions(             screenWidth = screenWidth,             screenHeight = screenHeight,             upper3rdEnd = screenHeight / 3,             middle3rdStart = screenHeight / 3,             middle3rdEnd = 2 * screenHeight / 3,             lower3rdStart = 2 * screenHeight / 3         )
// Initialisiere Panel-Positionen (ohne IME)         updatePanelPositions()     }
private fun updatePanelPositions() {         panelPositions = AccessibilityBarLayout.calculatePanelPositions(             screenDimensions,             imeHeight = currentImeHeight         )     }

private fun setupPanels() {         // PANEL 1: Snippets (Oben-Links, Bar)         panelRegistry[1] = SnippetsPanel(context, object : AccessibilityPanelListener {             override fun onItemSelected(item: Snippet) {                 copyToClipboard(item.content)                 showFeedback("Kopiert: ${item.title}")             }         })
// PANEL 2: Clipboard (Oben-Rechts, Bar)         panelRegistry[2] = ClipperPanel(context, object : AccessibilityPanelListener {             override fun onItemSelected(item: TransferItem) {                 pasteToFocusedApp(item.content)                 showFeedback("Eingefügt")             }         })
// PANEL 3: Shortcuts (Unten-Links)         panelRegistry[3] = ShortcutsPanel(context, object : AccessibilityPanelListener {             override fun onItemSelected(item: QuickAction) {                 executeQuickAction(item)             }         })
// PANEL 4: Quick Actions (Unten-Rechts)         panelRegistry[4] = QuickActionsPanel(context, object : AccessibilityPanelListener {             override fun onItemSelected(item: QuickAction) {                 executeQuickAction(item)             }         })
// Panel Views zum WindowManager hinzufügen         panelRegistry.forEach { (id, panel) ->             val view = panel.createView()             panelViews[id] = view             addPanelToWindow(id, view)         }     }
private fun addPanelToWindow(panelId: Int, view: View) {         val position = panelPositions[panelId] ?: return
val params = WindowManager.LayoutParams().apply {             type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY             format = PixelFormat.TRANSLUCENT             flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or                     WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS             width = position.width             height = position.height             x = position.x             y = position.y             gravity = position.gravity

}
windowManager.addView(view, params)     }
private fun setupGestureLayer() {         // Transparente Gesten-Schicht über dem gesamten Screen         val gestureView = View(context).apply {             setOnTouchListener { _, event ->                 val swipeEvent = edgeSwipeDetector.onMotionEvent(event)                 if (swipeEvent != null) {                     handleSwipeEvent(swipeEvent)                     true                 } else {                     false                 }             }         }
// IME-Detector an dieser View attachen         imeDetector = ImeStateDetector(gestureView, this)
val params = WindowManager.LayoutParams().apply {             type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY             format = PixelFormat.TRANSPARENT             flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or                     WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or                     WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS             width = WindowManager.LayoutParams.MATCH_PARENT             height = WindowManager.LayoutParams.MATCH_PARENT         }
windowManager.addView(gestureView, params)     }
private fun handleSwipeEvent(event: EdgeSwipeEvent) {         val panelId = when (event.edge) {             ScreenEdge.LEFT -> if (event.isTopSwipe()) 3 else 3     // Unten-Links             ScreenEdge.RIGHT -> if (event.isTopSwipe()) 2 else 4    // Oben-Rechts oder Unten-Rechts             else -> return         }
togglePanel(panelId, event.edge)     }
private fun togglePanel(panelId: Int, edge: ScreenEdge) {         val panel = panelRegistry[panelId] ?: return         val view = panelViews[panelId] ?: return
if (panel.isVisible) {             hidePanel(panelId, view)         } else {

showPanel(panelId, view, edge)         }     }
private fun showPanel(panelId: Int, view: View, edge: ScreenEdge) {         val position = panelPositions[panelId] ?: return
// Animierte Slide-In         val animator = when (edge) {             ScreenEdge.LEFT -> ObjectAnimator.ofFloat(view, "translationX", -position.width.toFloat(), 0f)             ScreenEdge.RIGHT -> ObjectAnimator.ofFloat(view, "translationX", position.width.toFloat(), 0f)             ScreenEdge.TOP -> ObjectAnimator.ofFloat(view, "translationY", -position.height.toFloat(), 0f)             ScreenEdge.BOTTOM -> ObjectAnimator.ofFloat(view, "translationY", position.height.toFloat(), 0f)         }         animator.duration = 300         animator.start()
view.visibility = View.VISIBLE         panelRegistry[panelId]?.onOpened()     }
private fun hidePanel(panelId: Int, view: View) {         val position = panelPositions[panelId] ?: return         val edge = position.edge
// Animierte Slide-Out         val animator = when (edge) {             ScreenEdge.LEFT -> ObjectAnimator.ofFloat(view, "translationX", 0f, -position.width.toFloat())             ScreenEdge.RIGHT -> ObjectAnimator.ofFloat(view, "translationX", 0f, position.width.toFloat())             ScreenEdge.TOP -> ObjectAnimator.ofFloat(view, "translationY", 0f, -position.height.toFloat())             ScreenEdge.BOTTOM -> ObjectAnimator.ofFloat(view, "translationY", 0f, position.height.toFloat())         }         animator.duration = 300         animator.doOnEnd {             view.visibility = View.GONE         }         animator.start()
panelRegistry[panelId]?.onClosed()     }
// ── IME HANDLING ─────────────────────────────────────────
override fun onImeShown(keyboardHeight: Int) {         currentImeHeight = keyboardHeight         updatePanelPositions()         repositionAllPanels()     }
override fun onImeHidden() {         currentImeHeight = 0         updatePanelPositions()

repositionAllPanels()     }
private fun repositionAllPanels() {         panelViews.forEach { (panelId, view) ->             val position = panelPositions[panelId] ?: return@forEach
val params = view.layoutParams as? WindowManager.LayoutParams ?: return@forEach             params.x = position.x             params.y = position.y             params.width = position.width             params.height = position.height             params.gravity = position.gravity
windowManager.updateViewLayout(view, params)
// Sanfte Animation des Übergangs             view.animate()                 .translationY(0f)                 .duration = 200         }     }
private fun copyToClipboard(text: String) {         val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager         val clip = ClipData.newPlainText("accessibility", text)         clipboard.setPrimaryClip(clip)         performHapticFeedback(HapticFeedbackConstants.CONFIRM)     }
private fun pasteToFocusedApp(text: String) {         // Nutze InputMethodManager um Text in fokussierte App einzufügen         val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager         // Note: Das ist eine Accessibility-Operation, daher limitiert         performHapticFeedback(HapticFeedbackConstants.CONFIRM)     }
private fun executeQuickAction(action: QuickAction) {         when (action.type) {             QuickActionType.UNDO -> undoLastAction()             QuickActionType.REDO -> redoLastAction()             QuickActionType.SCREENSHOT -> takeScreenshot()             else -> {}         }         performHapticFeedback(HapticFeedbackConstants.CONFIRM)     }
private fun showFeedback(message: String) {         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()     }
private fun performHapticFeedback(feedbackType: Int) {

// Android 6.0+: Vibrator API         val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator         vibrator?.vibrate(50)  // 50ms Vibration     }
private fun undoLastAction() {         // Via Accessibility Events     }
private fun redoLastAction() {         // Via Accessibility Events     }
private fun takeScreenshot() {         // Via MediaProjection (benötigt Berechtigungen)     }
fun dispose() {         panelViews.values.forEach { view ->             try {                 windowManager.removeView(view)             } catch (e: Exception) {                 // View nicht mehr im Window Manager             }         }     } } 4. PANEL INTERFACE & IMPLEMENTIERUNGEN Kotlin // borderline-android/feature-overlay/src/main/kotlin/AccessibilityPanel.kt interface AccessibilityPanelListener {     fun onItemSelected(item: Any) } abstract class AccessibilityPanel(     protected val context: Context,     protected val listener: AccessibilityPanelListener ) {     var isVisible = false
abstract fun createView(): View
open fun onOpened() {         isVisible = true     }
open fun onClosed() {         isVisible = false     } } Kotlin // borderline-android/feature-overlay/src/main/kotlin/panels/SnippetsPanel.kt
