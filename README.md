# BorderlineAndroid

> Ein leises, immer verfügbares Accessibility-Overlay für Android – zwei Werkzeuge, keine App-Hülle.

---

## Was ist Borderline?

Borderline ist eine Android-Accessibility-App, die eine unsichtbare Bedienungsschicht über jede andere App legt.  
Kein App-Wechsel. Keine Navigation. Zwei kleine Handles an den Bildschirmrändern – und die Werkzeuge sind sofort da.

**Primärversprechen:**  
Borderline reduziert Interaktionsreibung. Es tut genau zwei Dinge – und die schnell.

### Die zwei Zonen

| Zone | Position | Was es tut |
|------|----------|------------|
| **Snippets** | Linker Rand | Zeigt gespeicherte Textbausteine zum direkten Kopieren |
| **Clipper** | Rechter Rand | Greift die Zwischenablage automatisch ab und zeigt den Inhalt |

---

## Aktueller Zustand – v0.4.0

### ✅ Implementiert und funktionsfähig

- **Accessibility-Overlay-Fundament** – WindowManager-gestütztes Overlay, läuft über allen Apps
- **Zwei Edge-Handles** – links (Snippets) und rechts (Clipper), dezent am Bildschirmrand
- **Swipe-Gesten** – horizontales Einwischen vom Rand öffnet das Panel (`EdgeSwipeDetector`)
- **Tap-Fallback** – Tap auf den Handle öffnet ebenfalls das Panel
- **Snippets-Persistenz** – `JsonSnippetRepository` speichert Textbausteine per SharedPreferences (CRUD-Interface vorhanden)
- **Clipboard-Persistenz** – `JsonTransferItemRepository` speichert bis zu 20 ungepinnte + beliebig viele gepinnte Einträge
- **Clipboard Auto-Grab** – `ClipboardGrabber` liest die Zwischenablage automatisch, wenn der Clipper-Bereich geöffnet wird
- **IME-Handling** – `ImeStateDetector` erkennt die Tastatur und verschiebt die Handles nach oben
- **Haptic Feedback** – CONFIRM beim Ausführen einer Aktion, REJECT beim Deaktivieren per Longpress
- **Modulares Build-System** – 5 klar getrennte Module mit Dependency-Prüfung
- **StateFlow-Architektur** – Accessibility-Events → AccessibilityStateStore → UI-Subscription

### ❌ Noch nicht implementiert (MVP-Lücken)

- **Snippet-CRUD-UI im Overlay** – Anlegen, Bearbeiten, Löschen von Snippets direkt aus dem Panel heraus
- **Snippet-Suche im Panel** – Textfilter für gespeicherte Bausteine
- **Clipboard-Verlauf im Panel** – Gespeicherte `TransferItem`-Einträge werden noch nicht als scrollbare Liste angezeigt
- **Pin/Unpin-UI** – Repository-Logik vorhanden, aber keine Steuerelemente im Panel
- **Quick-Grab-Flow** – Swipe → Auto-Grab → Häkchen → fertig (noch nicht als eigenständiger Flow umgesetzt)
- **Bild- und Datei-Unterstützung im Clipper** – aktuell nur Text

---

## Architektur

```
app/
├── core/                        # Gemeinsame Datenmodelle, State, Logging, Module-Registry
│   ├── Snippet.kt               # id, title, content, category, createdAt
│   ├── TransferItem.kt          # id, kind (TEXT/URI), label, preview, timestamp, pinned
│   ├── ClipboardGrabber.kt      # Liest Zwischenablage on-demand, gibt TransferItem zurück
│   ├── JsonSnippetRepository.kt # SharedPreferences-basierte Persistenz für Snippets
│   ├── JsonTransferItemRepository.kt  # Persistenz für Clipboard-Einträge (max 20 ungepinnt)
│   ├── AccessibilityStateStore.kt     # GlobalStateFlow mit aktuellem AccessibilitySnapshot
│   ├── ModuleRegistry.kt        # Dependency-Auswertung für Module-Enable/Disable
│   └── ModulePrefs.kt           # SharedPreferences-Wrapper für Modul-Schalter
│
├── feature-overlay/             # WindowManager-Overlay, Handles, Panels, Gesten
│   ├── BorderlineOverlayController.kt  # Hauptcontroller: Handles, Panels, Auto-Grab, IME
│   ├── EdgeSwipeDetector.kt     # Swipe-Erkennung (40px Threshold, 120px/s Velocity)
│   └── ImeStateDetector.kt      # Keyboard-Visibility via ViewTreeObserver
│
├── feature-accessibility/       # Accessibility-Service-Backbone
│   └── BorderlineAccessibilityService.kt
│
├── feature-shortcuts/           # Quick-Actions-Registry (für spätere QCK-Zone)
│   ├── QuickAction.kt
│   └── QuickActionRegistry.kt
│
└── app/                         # Host-App mit Diagnose-UI und Modul-Schaltern
```

### Aktivierungslogik

```
Accessibility-Dienst aktivieren (Einstellungen → Eingabehilfen → Borderline)
    └── ModuleRegistry prüft Abhängigkeiten
        └── Feature-Overlay wird gestartet
            └── Handles erscheinen am Bildschirmrand
                └── Swipe / Tap → Panel öffnet sich
```

---

## Was Borderline nicht ist

- ❌ Kein Launcher-Ersatz
- ❌ Kein Tasker-Klon / Automatisierungsframework
- ❌ Kein stiller Hintergrund-Clipboard-Logger (Grab passiert nur beim aktiven Öffnen)
- ❌ Kein Vollbild-Arbeitsraum
- ❌ Kein universeller Accessibility-Automat
- ❌ Kein Notiz-App-Ersatz
- ❌ Kein Produktivitäts-Bauchladen mit 20 Funktionen

---

## Technische Voraussetzungen

| | |
|---|---|
| **Min. Android-Version** | Android 9 (API 28) |
| **Ziel-SDK** | Android 15 (API 36) |
| **Kotlin** | 2.3.10 |
| **AGP** | 9.1.0 |
| **Gradle** | 9.3.1 |
| **JDK** | JetBrains JDK 21 (erforderlich für `android.builtInKotlin=true`) |

### Bauen

```bash
./gradlew assembleDebug
```

> ⚠️ Für den Build wird ein JetBrains JDK 21 benötigt (z. B. über Android Studio).  
> Standard-Temurin-JDKs sind mit `android.builtInKotlin=true` + AGP 9.x nicht kompatibel.

### Berechtigung aktivieren

Nach der Installation muss der Accessibility-Dienst manuell aktiviert werden:  
**Einstellungen → Eingabehilfen → Heruntergeladene Apps → Borderline → Aktivieren**

---

## Roadmap zum MVP

| Priorität | Feature | Status |
|-----------|---------|--------|
| 🔴 Kritisch | Snippet-CRUD-UI im Panel (anlegen, bearbeiten, löschen) | ❌ Offen |
| 🔴 Kritisch | Clipboard-Verlauf als scrollbare Liste im Panel | ❌ Offen |
| 🔴 Kritisch | Quick-Grab-Flow (Swipe → Grab → Häkchen) | ❌ Offen |
| 🟡 Wichtig | Snippet-Suche im Panel | ❌ Offen |
| 🟡 Wichtig | Pin/Unpin-UI für Clipboard-Einträge | ❌ Offen |
| 🟡 Wichtig | Bild- und Datei-Unterstützung im Clipper | ❌ Offen |
| ✅ Erledigt | Zwei-Zonen-Architektur (Snippets + Clipper) | ✅ |
| ✅ Erledigt | Swipe-Gesten + Tap-Fallback | ✅ |
| ✅ Erledigt | JSON-Persistenz (Snippets + TransferItems) | ✅ |
| ✅ Erledigt | Clipboard Auto-Grab beim Öffnen | ✅ |
| ✅ Erledigt | IME-Handling (Handles snappen bei Tastatur) | ✅ |

---

## Weiterführende Dokumente

- [`ARCHITECTURE.md`](./ARCHITECTURE.md) – Designprinzipien, Modultrennung, Grenzen
- [`MVP_GAP_ANALYSIS.md`](./MVP_GAP_ANALYSIS.md) – Detaillierter Ist-/Soll-Vergleich zum MVP (Stand v0.4.0)
- [`NOT_IMPLEMENTED.md`](./NOT_IMPLEMENTED.md) – Bewusst nicht umgesetzte Features mit Begründung

