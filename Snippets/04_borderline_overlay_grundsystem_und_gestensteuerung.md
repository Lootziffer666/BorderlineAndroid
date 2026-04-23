# Borderline: Overlay-Grundsystem und Gestensteuerung

- Seiten: 23-35

Text(         text = data.subheading,         fontSize = 16.sp,         fontWeight = FontWeight.W400,         color = Color.DarkGray,         modifier = Modifier             .align(Alignment.Start)             .padding(start = 4.dp, top = 8.dp)     ) } // ... weitere Anchor-Typen ... fun configStateGradient(state: ScreenState): Brush {     return when (state) {         ScreenState.BLUE -> Brush.verticalGradient(             colors = listOf(Color(0xFFD9ECF1), Color(0xFFF6F3EC))         )         ScreenState.GREEN -> Brush.verticalGradient(             colors = listOf(Color(0xFFD9ECDD), Color(0xFFF6F3EC))         )         ScreenState.YELLOW -> Brush.verticalGradient(             colors = listOf(Color(0xFFECE0B9), Color(0xFFF6F3EC))         )         ScreenState.ORANGE -> Brush.verticalGradient(             colors = listOf(Color(0xFFECCAB0), Color(0xFFF6F3EC))         )         ScreenState.RED -> Brush.verticalGradient(             colors = listOf(Color(0xFFE2BBB5), Color(0xFFF6F3EC))         )     } } fun configSupportMargin(state: ScreenState): Dp {     return when (state) {         ScreenState.BLUE -> 24.dp         ScreenState.GREEN -> 24.dp         ScreenState.YELLOW -> 24.dp         ScreenState.ORANGE -> 22.dp         ScreenState.RED -> 22.dp     } } 💼 KONKRET: Nächste Schritte 1. HTML-Dateien → Documentation bash mkdir -p docs/ui # kopiere die 4 HTML-Dateien hin # schreib eine docs/ui/README.md 2. Kotlin-Scaffold erstellen Kotlin // catchit-android/core/ui/src/main/kotlin/com/flow/catchit/ui/ScreenConfig.kt // (ScreenConfig Datensatzklasse wie oben) // catchit-android/core/ui/src/main/kotlin/com/flow/catchit/ui/CatchItScreenComposer.kt // (der @Composable Generator) 3. Testierbar machen Kotlin

// catchit-android/core/ui/src/test/kotlin/com/flow/catchit/ui/ScreenConfigTest.kt class ScreenConfigTest {     @Test     fun `Blue + Countdown should render with 80px padding`() {         val config = ScreenConfig(             state = ScreenState.BLUE,             anchorType = AnchorType.COUNTDOWN,             anchorData = AnchorData.CountdownData(minutes = 15)         )         // assertion     }
@Test     fun `Orange + Instruction should render with 48px padding`() {         // ...     } } 4. Im App integrieren Kotlin // catchit-android/app/src/main/kotlin/com/flow/catchit/MainActivity.kt @Composable fun MainScreenContainer() {     val screenConfig = rememberScreenConfig()  // von ViewModel/State     CatchItScreen(screenConfig) } 🎬 WAS WILLST DU ZUERST? Soll ich die Kotlin-ScreenConfig-Klasse vollständig ausschreiben? (inklusive aller Anchor-Typen + Tests) Soll ich die HTML-Dateien in eine docs/ui/ Struktur mit README umorganisieren? Brauchst du ein Mapping-Dokument: "von Wireframe zu ScreenConfig"? (z.B. wie du einen Screen aus v3 in Kotlin-Code übersetzt) Willst du, dass ich im Wizard-Modul konkrete ScreenConfigs generiere? (z.B. WizardStep1.config(), WizardStep2.config(), etc.) Was ist dein Priorität? Lootziffer666/BORDERLINE You said: Kannst Du mir bitte hier detailliert sagen, Kannst Du mir bitte hier detailliert sagen, wie ich richtig accessibility Overlay menüs erstelle und nutzen kann. Ich möchte Clipboard+, Shortcuts usw.
in

4

Side

swipe

menüs

unterbringen.

Kannst

du

mir

da

helfen?
Copilot finished thinking Copilot said: Perfekt! Du hast **BorderlineAndroid** — ein echtes Perfekt! Du hast BorderlineAndroid — ein echtes Accessibility-Overlay-Projekt mit Swipe-Gesten, Menüs an den 4 Rändern, Clipboard-Management,
Shortcuts

und

Snippet-Verwaltung.
Das ist genau das Use-Case für das, was du brauchst. Lass mich dir eine vollständige Anleitung geben, wie man korrekt
4-Seiten-Accessibility-Overlay-Menüs

mit

Swipe-Gesten

baut.
🎯 ACCESSIBILITY OVERLAY MENÜS — DETAILLIERTER BAUPLAN 1. ARCHITEKTUR: Die 4 Edge-Menüs Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/EdgeSwipeDetector.kt /**  * Erkennt Swipes von allen 4 Kanten:  * - Links: Snippets  * - Rechts: Clipper    * - Oben: Shortcuts (neu)  * - Unten: Quick Actions (neu)

*/ data class EdgeSwipeEvent(     val edge: Edge,     val velocity: Float,     val distance: Float,     val timestamp: Long ) enum class Edge {     LEFT,    // Snippets     RIGHT,   // Clipboard     TOP,     // Shortcuts     BOTTOM   // Quick Actions } class EdgeSwipeDetector {     private val velocityTracker = VelocityTracker.obtain()     private val SWIPE_THRESHOLD_VELOCITY = 120f  // px/s     private val SWIPE_THRESHOLD_DISTANCE = 40f   // px     private val EDGE_DETECTION_ZONE = 40f        // px von Rand
fun onMotionEvent(event: MotionEvent): EdgeSwipeEvent? {         velocityTracker.addMovement(event)
return when (event.action) {             MotionEvent.ACTION_DOWN -> {                 startX = event.x                 startY = event.y                 detectEdge(event.x, event.y)                 null             }             MotionEvent.ACTION_MOVE -> null  // nur bei UP auswerten             MotionEvent.ACTION_UP -> {                 velocityTracker.computeCurrentVelocity(1000)  // px/ms → px/s                 val vx = velocityTracker.xVelocity                 val vy = velocityTracker.yVelocity                 val velocity = sqrt(vx * vx + vy * vy)
val dx = event.x - startX                 val dy = event.y - startY
// Prüfe: Swipe ausreichend weit + schnell + von Rand?                 if (shouldTriggerSwipe(dx, dy, velocity, screenWidth, screenHeight)) {                     val edge = determineEdge(dx, dy, startX, startY, screenWidth, screenHeight)                     EdgeSwipeEvent(                         edge = edge,                         velocity = velocity,                         distance = sqrt(dx * dx + dy * dy),                         timestamp = System.currentTimeMillis()                     )                 } else {                     null                 }             }             else -> null

}     }
private fun detectEdge(x: Float, y: Float): Edge? {         return when {             x < EDGE_DETECTION_ZONE -> Edge.LEFT             x > screenWidth - EDGE_DETECTION_ZONE -> Edge.RIGHT             y < EDGE_DETECTION_ZONE -> Edge.TOP             y > screenHeight - EDGE_DETECTION_ZONE -> Edge.BOTTOM             else -> null         }     }
private fun determineEdge(dx: Float, dy: Float, startX: Float, startY: Float,                                screenWidth: Float, screenHeight: Float): Edge {         val absDx = abs(dx)         val absDy = abs(dy)
return when {             startX < EDGE_DETECTION_ZONE && dx > 0 -> Edge.LEFT      // von links raus             startX > screenWidth - EDGE_DETECTION_ZONE && dx < 0 -> Edge.RIGHT  // von rechts raus             startY < EDGE_DETECTION_ZONE && dy > 0 -> Edge.TOP       // von oben raus             startY > screenHeight - EDGE_DETECTION_ZONE && dy < 0 -> Edge.BOTTOM  // von unten raus             else -> Edge.LEFT  // default         }     }
private fun shouldTriggerSwipe(dx: Float, dy: Float, velocity: Float,                                    screenWidth: Float, screenHeight: Float): Boolean {         val distance = sqrt(dx * dx + dy * dy)         return distance >= SWIPE_THRESHOLD_DISTANCE && velocity >= SWIPE_THRESHOLD_VELOCITY     } } 2. OVERLAY CONTROLLER: WindowManager + 4 Panels Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/BorderlineOverlayController.kt class BorderlineOverlayController(     private val context: Context,     private val windowManager: WindowManager ) {     private val edgeSwipeDetector = EdgeSwipeDetector()     private val panelRegistry = mutableMapOf<Edge, OverlayPanel>()
init {         setupPanels()         setupGestureLayer()     }
private fun setupPanels() {         panelRegistry[Edge.LEFT] = SnippetsPanel(context)         panelRegistry[Edge.RIGHT] = ClipperPanel(context)         panelRegistry[Edge.TOP] = ShortcutsPanel(context)         panelRegistry[Edge.BOTTOM] = QuickActionsPanel(context)

}
private fun setupGestureLayer() {         val gestureView = View(context).apply {             setOnTouchListener { _, event ->                 val swipeEvent = edgeSwipeDetector.onMotionEvent(event)                 if (swipeEvent != null) {                     handleSwipeEvent(swipeEvent)                     true                 } else {                     false                 }             }         }
// Ganz dünn transparent, nimmt keinen Platz ein         val params = WindowManager.LayoutParams().apply {             type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY             format = PixelFormat.TRANSPARENT             flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or                     WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH             width = WindowManager.LayoutParams.MATCH_PARENT             height = WindowManager.LayoutParams.MATCH_PARENT         }
windowManager.addView(gestureView, params)     }
private fun handleSwipeEvent(event: EdgeSwipeEvent) {         val panel = panelRegistry[event.edge] ?: return
// Panel mit Animation zeigen         showPanel(panel, event.edge, animationDuration = 300L)     }
private fun showPanel(panel: OverlayPanel, edge: Edge, animationDuration: Long) {         // Animation: Slide-in von der Kante         val anim = when (edge) {             Edge.LEFT -> ObjectAnimator.ofFloat(panel.view, "translationX", -panel.width, 0f)             Edge.RIGHT -> ObjectAnimator.ofFloat(panel.view, "translationX", panel.width, 0f)             Edge.TOP -> ObjectAnimator.ofFloat(panel.view, "translationY", -panel.height, 0f)             Edge.BOTTOM -> ObjectAnimator.ofFloat(panel.view, "translationY", panel.height, 0f)         }         anim.duration = animationDuration         anim.start()     } } 3. PANEL-INTERFACE: Alle 4 Menüs folgen der gleichen Struktur Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/OverlayPanel.kt abstract class OverlayPanel(     protected val context: Context,     val edge: Edge

) {     abstract val view: View     abstract val width: Int     abstract val height: Int
abstract fun onOpen()     abstract fun onClose()     abstract fun onItemSelected(item: Any)
/**      * Standard-Layout für alle Panels:      * - Header mit Icon + Titel      * - Scrollbare Content-Liste      * - Footer mit ggf. Einstellungen      */     protected fun buildStandardPanel(         title: String,         icon: Int,         items: List<Any>,         onItemClick: (Any) -> Unit     ): View {         return FrameLayout(context).apply {             addView(buildHeader(title, icon))             addView(buildContentList(items, onItemClick))             addView(buildFooter())         }     }
private fun buildHeader(title: String, icon: Int): View {         return LinearLayout(context).apply {             orientation = LinearLayout.HORIZONTAL             layoutParams = FrameLayout.LayoutParams(                 FrameLayout.LayoutParams.MATCH_PARENT, 56.dp             ).apply { gravity = Gravity.TOP }
addView(ImageView(context).apply {                 setImageResource(icon)                 layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)             })
addView(TextView(context).apply {                 text = title                 textSize = 16f                 setTextColor(Color.BLACK)                 layoutParams = LinearLayout.LayoutParams(                     0, LinearLayout.LayoutParams.MATCH_PARENT, 1f                 )             })         }     }
private fun buildContentList(items: List<Any>, onItemClick: (Any) -> Unit): View {         return RecyclerView(context).apply {             layoutParams = FrameLayout.LayoutParams(

FrameLayout.LayoutParams.MATCH_PARENT, 0             ).apply {                 topMargin = 56.dp                 bottomMargin = 56.dp                 gravity = Gravity.TOP or Gravity.START             }
adapter = StandardPanelAdapter(items) { item ->                 onItemClick(item)                 performHapticFeedback(HapticFeedbackConstants.CONFIRM)             }         }     }
private fun buildFooter(): View {         return LinearLayout(context).apply {             orientation = LinearLayout.HORIZONTAL             layoutParams = FrameLayout.LayoutParams(                 FrameLayout.LayoutParams.MATCH_PARENT, 56.dp             ).apply { gravity = Gravity.BOTTOM }
addView(ImageButton(context).apply {                 setImageResource(android.R.drawable.ic_menu_preferences)                 layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)                 setOnClickListener { onFooterSettingsClick() }             })         }     }
protected open fun onFooterSettingsClick() {} } 4. KONKRETE PANEL-IMPLEMENTIERUNGEN A) SNIPPETS PANEL (Links) Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/panels/SnippetsPanel.kt class SnippetsPanel(     context: Context,     private val snippetRepository: SnippetRepository ) : OverlayPanel(context, Edge.LEFT) {
override val width = 280     override val height = WindowManager.LayoutParams.MATCH_PARENT
override val view: View by lazy {         buildSnippetsUI()     }
private fun buildSnippetsUI(): View {         val snippets = snippetRepository.getAllSnippets()
return buildStandardPanel(             title = "Snippets",

icon = R.drawable.ic_snippets,             items = snippets,             onItemClick = { item ->                 val snippet = item as Snippet                 copyToClipboard(snippet.content)                 showFeedback("Kopiert: ${snippet.title}")             }         )     }
override fun onOpen() {         // Snippets laden     }
override fun onClose() {         // Cleanup     }
override fun onItemSelected(item: Any) {         if (item is Snippet) {             copyToClipboard(item.content)         }     }
private fun copyToClipboard(text: String) {         val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager         val clip = ClipData.newPlainText("snippet", text)         clipboard.setPrimaryClip(clip)     }
override fun onFooterSettingsClick() {         // Snippet-Editor öffnen     } } B) CLIPPER PANEL (Rechts) Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/panels/ClipperPanel.kt class ClipperPanel(     context: Context,     private val transferItemRepository: TransferItemRepository,     private val clipboardGrabber: ClipboardGrabber ) : OverlayPanel(context, Edge.RIGHT) {
override val width = 280     override val height = WindowManager.LayoutParams.MATCH_PARENT
override val view: View by lazy {         buildClipperUI()     }
private fun buildClipperUI(): View {         val items = transferItemRepository.getRecentItems()

return buildStandardPanel(             title = "Clipboard",             icon = R.drawable.ic_clipboard,             items = items,             onItemClick = { item ->                 val transferItem = item as TransferItem                 pasteItem(transferItem)             }         )     }
override fun onOpen() {         // Auto-Grab Zwischenablage         val newItem = clipboardGrabber.grabCurrent()         if (newItem != null) {             transferItemRepository.addItem(newItem)             showFeedback("✓ Zwischenablage erfasst")         }     }
override fun onClose() {         // Cleanup     }
override fun onItemSelected(item: Any) {         if (item is TransferItem) {             pasteItem(item)         }     }
private fun pasteItem(item: TransferItem) {         // Inject in Ziel-App (via InputMethodManager)         val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager         imm.inputExtractEditText?.setText(item.content)     }
override fun onFooterSettingsClick() {         // Pin/Unpin-Optionen     } } C) SHORTCUTS PANEL (Oben) Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/panels/ShortcutsPanel.kt class ShortcutsPanel(     context: Context,     private val shortcutRegistry: QuickActionRegistry ) : OverlayPanel(context, Edge.TOP) {
override val width = WindowManager.LayoutParams.MATCH_PARENT     override val height = 240  // Horizontal Panel

override val view: View by lazy {         buildShortcutsUI()     }
private fun buildShortcutsUI(): View {         val shortcuts = shortcutRegistry.getAll()
return buildStandardPanel(             title = "Shortcuts",             icon = R.drawable.ic_shortcuts,             items = shortcuts,             onItemClick = { item ->                 val action = item as QuickAction                 executeAction(action)             }         )     }
override fun onOpen() {         // Shortcuts UI aktualisieren     }
override fun onClose() {         // Cleanup     }
override fun onItemSelected(item: Any) {         if (item is QuickAction) {             executeAction(item)         }     }
private fun executeAction(action: QuickAction) {         when (action.type) {             QuickActionType.OPEN_APP -> launchApp(action.target)             QuickActionType.SEND_TEXT -> sendText(action.target)             QuickActionType.CALL -> initiateCall(action.target)             QuickActionType.CUSTOM -> action.customAction?.invoke()         }     }
private fun launchApp(packageName: String) {         val intent = context.packageManager.getLaunchIntentForPackage(packageName)         if (intent != null) {             context.startActivity(intent)         }     }
private fun sendText(text: String) {         val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager         clipboard.setPrimaryClip(ClipData.newPlainText("action", text))     }

private fun initiateCall(phoneNumber: String) {         val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))         context.startActivity(intent)     } } D) QUICK ACTIONS PANEL (Unten) Kotlin // borderline-android/feature-overlay/src/main/kotlin/com/borderline/overlay/panels/QuickActionsPanel.kt class QuickActionsPanel(     context: Context,     private val actionRegistry: QuickActionRegistry ) : OverlayPanel(context, Edge.BOTTOM) {
override val width = WindowManager.LayoutParams.MATCH_PARENT     override val height = 200
override val view: View by lazy {         buildActionsUI()     }
private fun buildActionsUI(): View {         val actions = actionRegistry.getQuickActions()
// Horizontal scrollable buttons         return HorizontalScrollView(context).apply {             layoutParams = FrameLayout.LayoutParams(                 FrameLayout.LayoutParams.MATCH_PARENT,                 FrameLayout.LayoutParams.WRAP_CONTENT             ).apply { gravity = Gravity.BOTTOM }
addView(LinearLayout(context).apply {                 orientation = LinearLayout.HORIZONTAL
actions.forEach { action ->                     addView(createActionButton(action))                 }             })         }     }
private fun createActionButton(action: QuickAction): View {         return Button(context).apply {             text = action.label             setCompoundDrawablesWithIntrinsicBounds(action.icon, 0, 0, 0)             layoutParams = LinearLayout.LayoutParams(
120.dp, 100.dp             ).apply { setMargins(8.dp, 8.dp, 8.dp, 8.dp) }
setOnClickListener {

executeAction(action)                 performHapticFeedback(HapticFeedbackConstants.CONFIRM)             }         }     }
override fun onOpen() {         // Actions aktualisieren     }
override fun onClose() {         // Cleanup     }
override fun onItemSelected(item: Any) {         if (item is QuickAction) {             executeAction(item)         }     }
private fun executeAction(action: QuickAction) {         when (action.type) {             QuickActionType.UNDO -> undoLastAction()             QuickActionType.REDO -> redoLastAction()             QuickActionType.SCREENSHOT -> takeScreenshot()             QuickActionType.DICTATION -> startDictation()         }     }
private fun undoLastAction() {         // undo via accessibility events     }
private fun redoLastAction() {         // redo via accessibility events     }
private fun takeScreenshot() {         // screenshot functionality     }
private fun startDictation() {         // voice input     } } 5. ACCESSIBILITY SERVICE: Der Backbone Kotlin // borderline-android/feature-accessibility/src/main/kotlin/com/borderline/service/BorderlineAccessibilityService.kt class BorderlineAccessibilityService : AccessibilityService() {
private lateinit var overlayController: BorderlineOverlayController

override fun onServiceConnected() {         super.onServiceConnected()
// Service-Konfiguration         val serviceInfo = AccessibilityServiceInfo().apply {             eventTypes = AccessibilityEvent.TYPES_ALL_MASK             feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC             flags = AccessibilityServiceInfo.FLAG_REPORT_INTERACTION_TIMES or                     AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE             notificationTimeout = 100         }         setServiceInfo(serviceInfo)
// Overlay starten         overlayController = BorderlineOverlayController(             context = this,             windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager         )     }
override fun onAccessibilityEvent(event: AccessibilityEvent?) {         // Accessibility-Events verarbeiten (z.B. Text-Eingabe erkannt)     }
override fun onInterrupt() {         // Service wurde unterbrochen     } } 6. BERECHTIGUNGEN & MANIFEST XML <!-- AndroidManifest.xml --> <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" /> <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" /> <uses-permission android:name="android.permission.READ_CLIPBOARD" /> <uses-permission android:name="android.permission.WRITE_CLIPBOARD" /> <service     android:name=".service.BorderlineAccessibilityService"     android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"     android:exported="true">     <intent-filter>         <action android:name="android.accessibilityservice.AccessibilityService" />     </intent-filter>     <meta-data         android:name="android.accessibilityservice"         android:resource="@xml/accessibility_service_config" /> </service> XML <!-- res/xml/accessibility_service_config.xml --> <?xml version="1.0" encoding="utf-8"?> <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"     android:accessibilityEventTypes="typeAllMask"
