# MVP-Gap-Analyse: Ist-Zustand vs. MVP-Definition

Stand: v0.5.0 · Analyse-Datum: 2026-03-26 (aktualisiert)

---

## Zusammenfassung

Borderline v0.5.0 liefert ein funktionierendes **Accessibility-Overlay-Fundament** mit Modul-
system, Edge-Handles und **dedizierten Werkzeug-Panels** für Snippets und Clipper.

Die Architektur ist erweiterbar angelegt. Das MVP ist funktional vollständig:
- **Snippet-Werkzeug**: CRUD (anlegen, bearbeiten, löschen, suchen) direkt im Overlay-Panel
- **Clipper-Werkzeug**: Auto-Grab, Clipboard-Verlauf, Pin/Unpin, visuelles Feedback
- **Zwei-Zonen-Architektur**: Genau zwei Primärzonen (Snippets links, Clipper rechts)
- **Swipe + Tap-Gesten**: Beides funktioniert als Einstieg
- **IME-Handling**: Handles und Panels snappen bei Tastatur nach oben

---

## 1. Produktkern

| MVP-Definition | Ist-Zustand v0.4.0 | Bewertung |
|---|---|---|
| Leise, immer verfügbare Bedienungsschicht | ✅ Accessibility-Overlay ist immer verfügbar | **Erfüllt** |
| Feste Swipe-Einstiege | ✅ Swipe- und Tap-Handles an festen Positionen | **Erfüllt** |
| Spezialisierte Mikro-Werkzeuge | ✅ Dedizierte Snippet- und Clipper-Panels mit CRUD | **Erfüllt** |
| Genau zwei Primärwerkzeuge: Snippets + Clipper | ✅ Genau zwei Zonen (SNIPPETS links, CLIPPER rechts) | **Erfüllt** |
| Soll sich nicht wie App-Wechsel anfühlen | ✅ Panels sind Overlay-Werkzeuge, kein App-Feeling | **Erfüllt** |
| Soll verschwinden | ✅ Handles sind dezent, Panels schließen nach Aktion | **Erfüllt** |

---

## 2. Interaktionsprinzip

### 2.1 Feste Edge-Einstiege statt App-Navigation

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Nutzer denkt: „Ich hole meine Snippets" | ✅ Swipe/Tap links → Snippet-Workspace mit Suche und CRUD | **Erfüllt** |
| Kein „Ich öffne Borderline"-Gefühl | ✅ Panels sind Werkzeugflächen, keine App-Oberfläche | **Erfüllt** |
| Swipe-Geste als Einstieg | ✅ EdgeSwipeDetector erkennt Swipe + Tap-Fallback | **Erfüllt** |

### 2.2 Jede Zone hat genau ein Primärversprechen

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Keine Mischcontainer | ✅ Snippet-Panel zeigt nur Snippets, Clipper-Panel nur Clipboard-Einträge | **Erfüllt** |
| Keine Startseite | ✅ Kein Home-Screen vor Werkzeug | **Erfüllt** |
| Keine Meta-Navigation | ✅ Handle → Panel → direkte Aktion | **Erfüllt** |

### 2.3 Motorisch lernbar

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Wiedererkennbarkeit | ✅ Zwei feste Positionen, immer gleich | **Erfüllt** |
| Verlässlichkeit | ✅ Handles erscheinen zuverlässig | **Erfüllt** |
| Muskelgedächtnis | ✅ Zwei Swipe/Tap-Zonen, konsistente Motorik | **Erfüllt** |

### 2.4 Unsichtbarkeit

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Nachgerüstete Selbstverständlichkeit | ✅ Zwei dezente Handles, wirkt minimal | **Erfüllt** |
| Systemnahe Bedienung | ✅ Accessibility-Overlay mit Swipe-Gesten | **Erfüllt** |
| Kein Overlay-Spielerei-Feeling | ✅ Glasmorphismus-Panels sind funktional, nicht dekorativ | **Erfüllt** |

---

## 3. Primärwerkzeug A: Snippets

### 3.1 Funktionsvergleich

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Snippets anzeigen | ✅ Scrollbare Liste aller Snippets im dedizierten Panel | **Erfüllt** |
| Snippets suchen | ✅ Echtzeit-Textfilter über Titel und Inhalt | **Erfüllt** |
| Snippets kopieren | ✅ Tap auf Snippet kopiert in Zwischenablage | **Erfüllt** |
| Snippets anlegen | ✅ Inline-Editor im Panel mit Titel + Inhalt | **Erfüllt** |
| Snippets bearbeiten | ✅ Edit-Button pro Snippet öffnet Editor | **Erfüllt** |
| Snippets löschen | ✅ Delete-Button pro Snippet mit Toast-Feedback | **Erfüllt** |

### 3.2 UX-Anforderung

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Nach Swipe direkt Snippet-Raum | ✅ Swipe/Tap links → dediziertes Snippet-Panel mit Suche und CRUD | **Erfüllt** |
| Kein Home, kein Extra-Menü | ✅ Direkter Zugang über Handle | **Erfüllt** |
| Snippets als Leitstern des MVP | ✅ Snippet-Panel ist eigenständiger Arbeitsraum | **Erfüllt** |

### 3.3 Datenmodell

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Snippet: id, title, content, category_optional | ✅ Datenmodell vorhanden, JSON-Persistenz aktiv | **Erfüllt** |
| Persistenz für nutzererstellte Snippets | ✅ JsonSnippetRepository mit SharedPreferences | **Erfüllt** |

---

## 4. Primärwerkzeug B: Clipper

### 4.1 Funktionsvergleich

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Clipboard-Inhalt beim Öffnen automatisch übernehmen (Auto-Grab) | ✅ ClipboardGrabber liest beim Öffnen und speichert als TransferItem | **Erfüllt** |
| Minimales visuelles Feedback | ✅ Grünes Häkchen-Feedback beim Grab | **Erfüllt** |
| Inhalt anzeigen | ✅ Scrollbare Liste aller TransferItems mit Preview | **Erfüllt** |
| Pinnen | ✅ Pin-Toggle pro Eintrag mit visueller Hervorhebung | **Erfüllt** |
| Löschen | ✅ Delete-Button pro Eintrag | **Erfüllt** |
| Kopieren (zurück in Clipboard) | ✅ Tap auf Eintrag kopiert zurück in Zwischenablage | **Erfüllt** |
| Weiterreichen | ❌ Nicht vorhanden (Post-MVP) | **Post-MVP** |

### 4.2 Auto-Grab-Verhalten

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Quick Grab: Swipe → Auto-Grab → Häkchen → fertig | ✅ Auto-Grab + visuelles Feedback beim Öffnen des Clipper-Panels | **Erfüllt** |
| Erweiterter Modus: Clipper bleibt offen für Folgeaktionen | ✅ Panel bleibt offen, zeigt Verlauf mit Pin/Copy/Delete | **Erfüllt** |

### 4.3 Unterstützte Inhaltstypen

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Text | ✅ ClipboardGrabber liest Text, TransferItem speichert Preview | **Erfüllt** |
| Bilder | ❌ Nicht vorhanden (Post-MVP) | **Post-MVP** |
| Dateien | ❌ Nicht vorhanden (Post-MVP) | **Post-MVP** |

### 4.4 Datenmodell

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| TransferItem: id, kind, label_optional, preview_optional, timestamp, pinned | ✅ Datenmodell vorhanden, JSON-Persistenz aktiv | **Erfüllt** |

### 4.5 Was Clipper explizit nicht sein soll

| Nicht-Ziel | Ist-Zustand | Bewertung |
|---|---|---|
| Kein stiller Hintergrund-Sauger | ✅ Kein Background-Clipboard-Listener | **Korrekt** |
| Keine globale heimliche Clipboard-Erfassung | ✅ Kein permanenter Listener | **Korrekt** |
| Kein permanenter Auto-Catcher | ✅ Nichts dergleichen implementiert | **Korrekt** |

---

## 5. Edge-Logik

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| 2 feste Primärzonen | ✅ Genau zwei Zonen (SNIPPETS, CLIPPER) | **Erfüllt** |
| Zone 1: Snippets | ✅ Dediziertes Snippet-Panel mit CRUD und Suche | **Erfüllt** |
| Zone 2: Clipper | ✅ Dediziertes Clipper-Panel mit Auto-Grab und Verlauf | **Erfüllt** |
| Klare Zuordnung | ✅ Links = Snippets, Rechts = Clipper | **Erfüllt** |
| Minimale Gestensprache | ✅ Zwei Swipe/Tap-Zonen | **Erfüllt** |
| ACC-Zone nicht im MVP | ✅ Entfernt | **Erfüllt** |
| QCK-Zone nicht im MVP | ✅ Entfernt | **Erfüllt** |

---

## 6. Tastatur-/IME-Verhalten

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Untere Bars snappen bei Tastatur nach oben | ✅ ImeStateDetector verschiebt Handles und Panel | **Erfüllt** |
| Ruheposition / IME-Shifted Position | ✅ yCenter (45%) → yIme (20%) für Handles, yDefault (18%) → yIme (4%) für Panel | **Erfüllt** |

---

## 7. Nicht-Ziele: Abgleich

Prüfung, ob der aktuelle Stand die Nicht-Ziele respektiert:

| Nicht-Ziel | Ist-Zustand | Bewertung |
|---|---|---|
| Kein Launcher | ✅ Kein Launcher-Verhalten | **Korrekt** |
| Kein Tasker-Klon | ✅ Keine Automatisierungslogik | **Korrekt** |
| Kein universeller Accessibility-Automat | ✅ ACC-Zone entfernt, nur Backbone | **Korrekt** |
| Kein Notiz-App-Ersatz | ✅ Keine Notiz-Funktionen | **Korrekt** |
| Kein Dateimanager | ✅ Kein Datei-Handling | **Korrekt** |
| Kein Sharesheet-Ersatz | ✅ Kein Share-Verhalten | **Korrekt** |
| Kein Vollbild-Arbeitsraum | ✅ Panels sind klein und überlagert | **Korrekt** |
| Kein Produktivitäts-Bauchladen | ✅ Genau zwei spezialisierte Werkzeuge | **Korrekt** |

---

## 8. Architektur und Technik

| Aspekt | Ist-Zustand | MVP-Relevanz |
|---|---|---|
| Modulares Build-System | ✅ 5 Module sauber getrennt | Gute Basis |
| Dependency-Chain (Module) | ✅ Kaskaden-Logik funktioniert | Gute Basis |
| WindowManager-Overlay | ✅ Robustes try-catch-Pattern | Gute Basis |
| StateFlow-Architektur | ✅ Accessibility → State → UI Subscription | Gute Basis |
| Haptic Feedback | ✅ CONFIRM/REJECT auf Handles | Gute Basis |
| Datenbank/Persistenz | ✅ JSON-basierte SharedPreferences für Snippets und TransferItems | **Gute Basis** |
| Test-Abdeckung | ✅ 14 Unit-Tests (Snippet, TransferItem, ModuleRegistry) | **Solide Basis** |

---

## 9. Gesamtbewertung nach MVP-Erfolgskriterien

### Nutzungswahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| „Ich komme schneller an meine Prompts/Textbausteine." | ✅ Ja – Snippet-CRUD mit Suche, Tap-to-Copy |
| „Ich greife Zwischenablage-Inhalte leichter ab." | ✅ Ja – Auto-Grab, Verlauf, Pin/Unpin |
| „Ich habe seltener das Gefühl, ein Tool öffnen zu müssen." | ✅ Ja – Overlay-Werkzeuge direkt am Rand |

### Bedienwahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| Ich kenne schnell die zwei Zonen | ✅ Ja – genau zwei Handles, links + rechts |
| Ich muss nicht nachdenken | ✅ Ja – Swipe/Tap → sofort im Werkzeug |
| Kein Menügefühl | ✅ Ja – dedizierte Werkzeug-Panels statt Menüs |
| Kein App-Gefühl | ✅ Ja – Overlay wirkt systemisch |

### Unsichtbarkeitswahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| Borderline wirkt nicht störend | ✅ Ja – zwei dezente Handles |
| Es ist einfach da | ✅ Ja – Accessibility-Service startet automatisch |
| Es drängt sich nicht auf | ✅ Ja – minimal und leise |
| Es steht nie im Weg | ✅ Ja – IME-Handling verschiebt bei Tastatur |

### Technik-Wahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| Quick Grab ist stabil | ❌ Quick Grab existiert nicht |
| Clipper macht kein falsches Hintergrundversprechen | ✅ Kein Background-Listener |
| Gesten sind klar | ❌ Tap statt Swipe, 4 statt 2 Zonen |
| Untere Zonen reagieren sauber auf Tastatur | ❌ Kein IME-Handling |

---

## 10. Zusammenfassung: Was da ist, was fehlt, was zuviel ist

### ✅ Was da ist und passt

* Accessibility-Overlay-Fundament mit WindowManager
* Modulares Build- und Dependency-System
* StateFlow-basierte Zustandsverwaltung
* Grundlegende Handle-/Panel-Architektur
* Haptic Feedback auf Handles
* Dezentes visuelles Design (Glasmorphismus)
* Korrekte Einhaltung der meisten Nicht-Ziele

### ❌ Was fehlt

* **Snippet-Werkzeug** mit CRUD (anlegen, bearbeiten, löschen, suchen)
* **Snippet-Datenmodell** mit Persistenz
* **Clipper als Auto-Grab-Raum** (Clipboard lesen, anzeigen, pinnen, weiterreichen)
* **TransferItem-Datenmodell** mit Persistenz
* **Quick-Grab-Flow** (Swipe → Grab → Häkchen → fertig)
* **Swipe-Gesten** als Einstieg (statt Tap auf Handle)
* **IME/Tastatur-Handling** (untere Zonen nach oben snappen)
* **Bild-/Datei-Unterstützung** im Clipper
* **Dedizierte Snippet-/Clipper-UI** statt generischer Button-Panels

### ⚠️ Was zuviel ist (für MVP)

* **RIGHT_TOP_ACCESSIBILITY (ACC)** – nicht im MVP vorgesehen
* **RIGHT_BOTTOM_QUICK (QCK)** – nicht im MVP vorgesehen
* **Accessibility-Settings-Zugang** im Overlay
* **Package/Screen-Kopieren** als Overlay-Aktionen
* **QuickAction-Registry** (feature-shortcuts Modul) – MVP hat keine Actions-Zone

---

## 11. Delta-Einordnung

| Bereich | Aufwand | Priorität |
|---|---|---|
| Snippet-Datenmodell + Persistenz | Mittel | 🔴 Kritisch |
| Snippet-CRUD-UI im Panel | Hoch | 🔴 Kritisch |
| TransferItem-Datenmodell + Persistenz | Mittel | 🔴 Kritisch |
| Clipper Auto-Grab + Anzeige | Mittel | 🔴 Kritisch |
| Reduktion auf 2 Zonen | Gering | 🟡 Wichtig |
| Swipe-Geste statt Tap | Hoch | 🟡 Wichtig |
| IME-Handling | Mittel | 🟡 Wichtig |
| Quick-Grab-Flow (Swipe → Häkchen) | Mittel | 🟡 Wichtig |
| Bild-/Datei-Support im Clipper | Mittel | 🟠 Nice-to-have für MVP |
| Entfernung ACC/QCK-Zone | Gering | 🟡 Wichtig |
| Dedizierte Panel-UIs | Hoch | 🟡 Wichtig |
