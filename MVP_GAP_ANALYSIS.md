# MVP-Gap-Analyse: Ist-Zustand vs. MVP-Definition

Stand: v0.4.0 · Analyse-Datum: 2026-03-26

---

## Zusammenfassung

Borderline v0.4.0 liefert ein funktionierendes **Accessibility-Overlay-Fundament** mit Modul-
system, Edge-Handles und Panels. Die Architektur ist erweiterbar angelegt.

Gemessen an der MVP-Definition fehlen jedoch **wesentliche Kernstücke**: ein echtes Snippet-
Werkzeug (CRUD), ein echter Clipper mit Auto-Grab und das bewusste Reduzieren auf genau
zwei Primärzonen. Der aktuelle Stand ist eher ein **technischer Prototyp** als das beschriebene MVP.

---

## 1. Produktkern

| MVP-Definition | Ist-Zustand v0.4.0 | Bewertung |
|---|---|---|
| Leise, immer verfügbare Bedienungsschicht | ✅ Accessibility-Overlay ist immer verfügbar | **Erfüllt** |
| Feste Swipe-Einstiege | ⚠️ Feste Tap-Handles (kein Swipe, nur Tap/Toggle) | **Teilweise** |
| Spezialisierte Mikro-Werkzeuge | ⚠️ Panels zeigen Aktionslisten, keine spezialisierten Werkzeugräume | **Teilweise** |
| Genau zwei Primärwerkzeuge: Snippets + Clipper | ❌ Vier gleichwertige Zonen (TXT, CLP, ACC, QCK) | **Nicht erfüllt** |
| Soll sich nicht wie App-Wechsel anfühlen | ⚠️ Panels haben eigene UI, aber kein App-Feeling durch Overlay-Architektur | **Teilweise** |
| Soll verschwinden | ⚠️ Handles sind sichtbar, aber dezent | **Teilweise** |

---

## 2. Interaktionsprinzip

### 2.1 Feste Edge-Einstiege statt App-Navigation

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Nutzer denkt: „Ich hole meine Snippets" | ❌ Nutzer muss wissen: TXT-Handle oben links = Textblöcke | **Nicht erfüllt** |
| Kein „Ich öffne Borderline"-Gefühl | ⚠️ Handles öffnen ein benanntes Panel mit Titel und Buttons | **Teilweise** |
| Swipe-Geste als Einstieg | ❌ Tap auf kleinen Handle (14×56dp), kein Swipe | **Nicht erfüllt** |

### 2.2 Jede Zone hat genau ein Primärversprechen

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Keine Mischcontainer | ❌ LEFT_TOP_SAVED mixt Prompts + „Open Borderline" + „Copy Package" | **Nicht erfüllt** |
| Keine Startseite | ✅ Kein Home-Screen vor Werkzeug | **Erfüllt** |
| Keine Meta-Navigation | ✅ Handle → Panel → direkte Aktion | **Erfüllt** |

### 2.3 Motorisch lernbar

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Wiedererkennbarkeit | ⚠️ Positionen fest, aber vier Zonen statt zwei | **Teilweise** |
| Verlässlichkeit | ✅ Handles erscheinen zuverlässig | **Erfüllt** |
| Muskelgedächtnis | ⚠️ Vier kleine Tap-Targets, keine Swipe-Motorik | **Teilweise** |

### 2.4 Unsichtbarkeit

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Nachgerüstete Selbstverständlichkeit | ⚠️ Dezent, aber vier sichtbare Handles wirken nach Feature-Fülle | **Teilweise** |
| Systemnahe Bedienung | ⚠️ Accessibility-Overlay korrekt, aber Tap-Panels sind eigene UI | **Teilweise** |
| Kein Overlay-Spielerei-Feeling | ⚠️ Glasmorphismus-Panels mit Animationen wirken poliert, aber auch nach Overlay-App | **Teilweise** |

---

## 3. Primärwerkzeug A: Snippets

### 3.1 Funktionsvergleich

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Snippets anzeigen | ⚠️ 2 hardcodierte Prompts als Buttons in Panel | **Minimal** |
| Snippets suchen | ❌ Nicht vorhanden | **Fehlt** |
| Snippets kopieren | ✅ Button-Tap kopiert in Zwischenablage | **Erfüllt** |
| Snippets anlegen | ❌ Nicht vorhanden, nur Ressourcen-Strings | **Fehlt** |
| Snippets bearbeiten | ❌ Nicht vorhanden | **Fehlt** |
| Snippets löschen | ❌ Nicht vorhanden | **Fehlt** |

### 3.2 UX-Anforderung

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Nach Swipe direkt Snippet-Raum | ❌ Tap öffnet generisches Panel mit 4 Buttons (2 Prompts + 2 Systemaktionen) | **Nicht erfüllt** |
| Kein Home, kein Extra-Menü | ✅ Direkter Zugang über Handle | **Erfüllt** |
| Snippets als Leitstern des MVP | ❌ Snippets sind ein Nebenteil eines generischen Panels | **Nicht erfüllt** |

### 3.3 Datenmodell

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Snippet: id, title, content, category_optional | ❌ Kein Datenmodell. Nur 2 Ressourcen-Strings | **Fehlt komplett** |
| Persistenz für nutzererstellte Snippets | ❌ Keine Datenbank/Speicherung | **Fehlt komplett** |

---

## 4. Primärwerkzeug B: Clipper

### 4.1 Funktionsvergleich

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Clipboard-Inhalt beim Öffnen automatisch übernehmen (Auto-Grab) | ❌ Nicht vorhanden. CLP-Panel zeigt nur Kopier-Aktionen | **Fehlt** |
| Minimales visuelles Feedback | ❌ Kein Grab-Feedback | **Fehlt** |
| Inhalt anzeigen | ❌ Clipboard-Inhalt wird nicht im Panel dargestellt | **Fehlt** |
| Pinnen | ❌ Nicht vorhanden | **Fehlt** |
| Löschen | ❌ Nicht vorhanden | **Fehlt** |
| Kopieren (zurück in Clipboard) | ⚠️ Nur vordefinierte Werte (Package, Screen), nicht den gegrabbten Inhalt | **Fehlt** |
| Weiterreichen | ❌ Nicht vorhanden | **Fehlt** |

### 4.2 Auto-Grab-Verhalten

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Quick Grab: Swipe → Auto-Grab → Häkchen → fertig | ❌ Nicht vorhanden | **Fehlt komplett** |
| Erweiterter Modus: Clipper bleibt offen für Folgeaktionen | ❌ Panel zeigt keine Clipboard-Inhalte | **Fehlt komplett** |

### 4.3 Unterstützte Inhaltstypen

| MVP-Anforderung | Ist-Zustand | Bewertung |
|---|---|---|
| Text | ⚠️ Clipboard wird nur zum Schreiben genutzt (ClipData.newPlainText), nicht zum Lesen | **Nicht erfüllt** |
| Bilder | ❌ Nicht vorhanden | **Fehlt** |
| Dateien | ❌ Nicht vorhanden | **Fehlt** |

### 4.4 Datenmodell

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| TransferItem: id, kind, label_optional, preview_optional, timestamp, pinned | ❌ Kein Datenmodell vorhanden | **Fehlt komplett** |

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
| 2 feste Primärzonen | ❌ 4 gleichwertige Zonen | **Nicht erfüllt** |
| Zone 1: Snippets | ⚠️ LEFT_TOP_SAVED existiert, ist aber generisches Panel | **Ansatz vorhanden** |
| Zone 2: Clipper | ⚠️ LEFT_BOTTOM_CLIPBOARD existiert, ist aber generisches Panel | **Ansatz vorhanden** |
| Klare Zuordnung | ⚠️ Zuordnung existiert per Enum, aber Inhalt passt nicht zur MVP-Vision | **Teilweise** |
| Minimale Gestensprache | ❌ 4 Tap-Handles statt 2 Swipe-Zonen | **Nicht erfüllt** |
| ACC-Zone nicht im MVP | ❌ RIGHT_TOP_ACCESSIBILITY ist aktiv | **Überschuss** |
| QCK-Zone nicht im MVP | ❌ RIGHT_BOTTOM_QUICK ist aktiv | **Überschuss** |

---

## 6. Tastatur-/IME-Verhalten

| MVP-Definition | Ist-Zustand | Bewertung |
|---|---|---|
| Untere Bars snappen bei Tastatur nach oben | ❌ Kein IME-Handling implementiert | **Fehlt komplett** |
| Ruheposition / IME-Shifted Position | ❌ Keine Position-Varianten | **Fehlt komplett** |

---

## 7. Nicht-Ziele: Abgleich

Prüfung, ob der aktuelle Stand die Nicht-Ziele respektiert:

| Nicht-Ziel | Ist-Zustand | Bewertung |
|---|---|---|
| Kein Launcher | ✅ Kein Launcher-Verhalten | **Korrekt** |
| Kein Tasker-Klon | ✅ Keine Automatisierungslogik | **Korrekt** |
| Kein universeller Accessibility-Automat | ⚠️ ACC-Zone zeigt Accessibility-Settings, wirkt nach Tool | **Grenzwertig** |
| Kein Notiz-App-Ersatz | ✅ Keine Notiz-Funktionen | **Korrekt** |
| Kein Dateimanager | ✅ Kein Datei-Handling | **Korrekt** |
| Kein Sharesheet-Ersatz | ✅ Kein Share-Verhalten | **Korrekt** |
| Kein Vollbild-Arbeitsraum | ✅ Panels sind klein und überlagert | **Korrekt** |
| Kein Produktivitäts-Bauchladen | ⚠️ 4 Zonen mit 16 Aktionen wirken nach Feature-Sammlung | **Grenzwertig** |

---

## 8. Architektur und Technik

| Aspekt | Ist-Zustand | MVP-Relevanz |
|---|---|---|
| Modulares Build-System | ✅ 5 Module sauber getrennt | Gute Basis |
| Dependency-Chain (Module) | ✅ Kaskaden-Logik funktioniert | Gute Basis |
| WindowManager-Overlay | ✅ Robustes try-catch-Pattern | Gute Basis |
| StateFlow-Architektur | ✅ Accessibility → State → UI Subscription | Gute Basis |
| Haptic Feedback | ✅ CONFIRM/REJECT auf Handles | Gute Basis |
| Datenbank/Persistenz | ❌ Keine (kein Room, kein SQLite) | **Kritische Lücke** |
| Test-Abdeckung | ⚠️ 1 Unit-Test | **Minimale Basis** |

---

## 9. Gesamtbewertung nach MVP-Erfolgskriterien

### Nutzungswahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| „Ich komme schneller an meine Prompts/Textbausteine." | ❌ Nein – nur 2 hardcodierte Prompts, kein CRUD |
| „Ich greife Zwischenablage-Inhalte leichter ab." | ❌ Nein – kein Auto-Grab, kein Clipboard-Lesen |
| „Ich habe seltener das Gefühl, ein Tool öffnen zu müssen." | ⚠️ Teilweise – Overlay ist da, aber Inhalte fehlen |

### Bedienwahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| Ich kenne schnell die zwei Zonen | ❌ Nein – vier Zonen, nicht zwei |
| Ich muss nicht nachdenken | ⚠️ Handles haben Kürzel, aber 4 Punkte = Überlegung nötig |
| Kein Menügefühl | ⚠️ Panels mit 4 Buttons sind Menüs |
| Kein App-Gefühl | ✅ Overlay wirkt systemisch |

### Unsichtbarkeitswahrheit

| Kriterium | Erreichbar mit v0.4.0? |
|---|---|
| Borderline wirkt nicht störend | ⚠️ 4 Handle-Punkte an den Rändern sind sichtbar |
| Es ist einfach da | ✅ Accessibility-Service startet automatisch |
| Es drängt sich nicht auf | ⚠️ 4 Handles drängen mehr als 2 |
| Es steht nie im Weg | ❌ Kein IME-Handling → kann im Weg stehen |

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
