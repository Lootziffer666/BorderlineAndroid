# Borderline: Panel-Implementierungen und Service-Backbone

- Seiten: 46-50

class SnippetsPanel(     context: Context,     listener: AccessibilityPanelListener ) : AccessibilityPanel(context, listener) {
private lateinit var rootView: View
override fun createView(): View {         rootView = FrameLayout(context).apply {             layoutParams = FrameLayout.LayoutParams(280, 64)  // Bar-Größe
// Header             addView(LinearLayout(context).apply {                 orientation = LinearLayout.HORIZONTAL                 layoutParams = FrameLayout.LayoutParams(                     FrameLayout.LayoutParams.MATCH_PARENT,                     FrameLayout.LayoutParams.MATCH_PARENT                 )
// Icon                 addView(ImageView(context).apply {                     setImageResource(android.R.drawable.ic_menu_edit)                     layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)                 })
// Snippet Preview                 addView(TextView(context).apply {                     text = "Snippets"                     textSize = 14f                     setTextColor(Color.BLACK)                     layoutParams = LinearLayout.LayoutParams(                         0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f                     ).apply { gravity = Gravity.CENTER_VERTICAL }                 })             })         }         return rootView     } } Kotlin // borderline-android/feature-overlay/src/main/kotlin/panels/QuickActionsPanel.kt class QuickActionsPanel(     context: Context,     listener: AccessibilityPanelListener ) : AccessibilityPanel(context, listener) {
private lateinit var rootView: View
override fun createView(): View {         rootView = FrameLayout(context).apply {             layoutParams = FrameLayout.LayoutParams(280, 216)  // 3x 64px + Spacing             backgroundColor = Color.parseColor("#F5F5F5")

addView(LinearLayout(context).apply {                 orientation = LinearLayout.VERTICAL                 layoutParams = FrameLayout.LayoutParams(                     FrameLayout.LayoutParams.MATCH_PARENT,                     FrameLayout.LayoutParams.MATCH_PARENT                 )
// Quick Action Buttons (3 vertikal)                 val actions = listOf(                     Pair("Undo", android.R.drawable.ic_menu_revert),                     Pair("Redo", android.R.drawable.ic_menu_redo),                     Pair("Screenshot", android.R.drawable.ic_menu_camera)                 )
actions.forEach { (label, icon) ->                     addView(Button(context).apply {                         text = label                         layoutParams = LinearLayout.LayoutParams(                             LinearLayout.LayoutParams.MATCH_PARENT, 64.dp                         ).apply { setMargins(4.dp, 4.dp, 4.dp, 4.dp) }
setOnClickListener {                             listener.onItemSelected(label)                         }                     })                 }             })         }         return rootView     } } 5. EDGE SWIPE DETECTOR MIT TOP/BOTTOM ERKENNUNG Kotlin // borderline-android/feature-overlay/src/main/kotlin/EdgeSwipeDetector.kt data class EdgeSwipeEvent(     val edge: ScreenEdge,     val velocity: Float,     val distance: Float,     val startY: Float,  // ← NEU für Top/Bottom Erkennung     val timestamp: Long ) {     /**      * Erkennt ob Swipe von oben kam (von Y < 1/3)      */     fun isTopSwipe(screenHeight: Int): Boolean {         return startY < (screenHeight / 3)     }
/**      * Erkennt ob Swipe von unten kam (von Y > 2/3)      */     fun isBottomSwipe(screenHeight: Int): Boolean {

return startY > (2 * screenHeight / 3)     } } class EdgeSwipeDetector {     private val velocityTracker: VelocityTracker? = null     private var startX = 0f     private var startY = 0f
private val SWIPE_THRESHOLD_VELOCITY = 120f  // px/s     private val SWIPE_THRESHOLD_DISTANCE = 40f     private val EDGE_DETECTION_ZONE = 40f
fun onMotionEvent(event: MotionEvent, screenWidth: Int, screenHeight: Int): EdgeSwipeEvent? {         return when (event.action) {             MotionEvent.ACTION_DOWN -> {                 startX = event.x                 startY = event.y                 null             }             MotionEvent.ACTION_UP -> {                 val dx = event.x - startX                 val dy = event.y - startY                 val distance = sqrt(dx * dx + dy * dy)
// Velocity berechnen (simplified)                 val velocity = distance / (event.eventTime - event.downTime).coerceAtLeast(1L).toFloat() * 1000
when {                     // Von Links raus (dx > 0)                     startX < EDGE_DETECTION_ZONE && dx > SWIPE_THRESHOLD_DISTANCE &&                      velocity > SWIPE_THRESHOLD_VELOCITY -> {                         EdgeSwipeEvent(                             edge = ScreenEdge.LEFT,                             velocity = velocity,                             distance = distance,                             startY = startY,                             timestamp = System.currentTimeMillis()                         )                     }
// Von Rechts raus (dx < 0)                     startX > screenWidth - EDGE_DETECTION_ZONE && dx < -SWIPE_THRESHOLD_DISTANCE &&                     velocity > SWIPE_THRESHOLD_VELOCITY -> {                         EdgeSwipeEvent(                             edge = ScreenEdge.RIGHT,                             velocity = velocity,                             distance = distance,                             startY = startY,                             timestamp = System.currentTimeMillis()                         )                     }
// Von Oben raus (dy > 0)

startY < EDGE_DETECTION_ZONE && dy > SWIPE_THRESHOLD_DISTANCE &&                     velocity > SWIPE_THRESHOLD_VELOCITY -> {                         EdgeSwipeEvent(                             edge = ScreenEdge.TOP,                             velocity = velocity,                             distance = distance,                             startY = startY,                             timestamp = System.currentTimeMillis()                         )                     }
// Von Unten raus (dy < 0)                     startY > screenHeight - EDGE_DETECTION_ZONE && dy < -SWIPE_THRESHOLD_DISTANCE &&                     velocity > SWIPE_THRESHOLD_VELOCITY -> {                         EdgeSwipeEvent(                             edge = ScreenEdge.BOTTOM,                             velocity = velocity,                             distance = distance,                             startY = startY,                             timestamp = System.currentTimeMillis()                         )                     }
else -> null                 }             }             else -> null         }     } } 6. HELPER EXTENSIONS Kotlin // borderline-android/feature-overlay/src/main/kotlin/Extensions.kt val Int.dp: Int     get() = (this * Resources.getSystem().displayMetrics.density).toInt() fun ObjectAnimator.doOnEnd(action: () -> Unit) {     addListener(object : android.animation.Animator.AnimatorListener {         override fun onAnimationStart(animation: android.animation.Animator) {}         override fun onAnimationEnd(animation: android.animation.Animator) {             action()         }         override fun onAnimationCancel(animation: android.animation.Animator) {}         override fun onAnimationRepeat(animation: android.animation.Animator) {}     }) } fun View.backgroundColor(color: Int) {     setBackgroundColor(color) } 7. INTEGRATION IN SERVICE Kotlin // borderline-android/feature-accessibility/src/main/kotlin/BorderlineAccessibilityService.kt (erweitert) class BorderlineAccessibilityService : AccessibilityService() {

private lateinit var overlayController: BorderlineOverlayController
override fun onServiceConnected() {         super.onServiceConnected()
overlayController = BorderlineOverlayController(             context = this,             windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager         )
BorderlineLogger.i("Accessibility service connected with 4-panel layout")     }
override fun onAccessibilityEvent(event: AccessibilityEvent?) {         // Event-handling     }
override fun onInterrupt() {         BorderlineLogger.w("Accessibility interrupted")     }
override fun onDestroy() {         overlayController.dispose()         super.onDestroy()     } } 🎬 ZUSAMMENFASSUNG: DIE 4 PANELS
Panel

Position

IME-Verhalten

Funktion

1

(Snippets)

Oben-Links,

64px

Bar

Zieht

nach

oben

um

IME-Höhe

Copy-Paste

Snippets

2

(Clipboard)

Oben-Rechts,

64px

Bar

Zieht

nach

oben

um

IME-Höhe

Clipboard-Verlauf

3

(Shortcuts)

Unten-Links,

unter

1/3

Zieht

nach

oben

um

IME-Höhe

App-Links,

Phone

etc.

4

(Quick

Actions)

Unten-Rechts,

Mitte

2/3

Zieht

nach

oben

um

IME-Höhe

Undo,

Redo,

Screenshot
Swipe-Gesten: Von Links → Panel 1 oder 3 (je nach startY) Von Rechts → Panel 2 oder 4 (je nach startY) Von Oben → Panel 1/2 Von Unten → Panel 3/4 IME-Handling: Alle Panels verschieben sich synchronized nach oben, wenn Tastatur sichtbar wird. ImeStateDetector gibt die aktuelle Höhe,
repositionAllPanels()

aktualisiert

alle

4

Positionen.
Brauchst du noch Refinements beim Swipe-Detection oder Custom Animations? You said: Nee,keine Swipe Menüs von oben und unt…
