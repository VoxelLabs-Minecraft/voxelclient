# VoxelClient

> Ein Fabric Client-Mod für Minecraft 1.21.4 mit Custom HUD, Zoom, Freelook, Cosmetics, Utility-Features und vielen Quality-of-Life Verbesserungen.

---

## 📥 Installation

### Automatisch (empfohlen)

1. **Installer herunterladen** → [Releases](cdn.voxellabs.de/s/installer)
2. `voxelclient-installer.exe` ausführen
3. Den gewünschten `mods/`-Ordner auswählen (wird automatisch erkannt)
4. Auf **„Jetzt installieren"** klicken
5. Minecraft mit dem **Fabric Launcher** starten

Der Installer lädt automatisch die neueste Version sowie die benötigte **Fabric API** herunter.

---

### Manuell

1. [Fabric Loader](https://fabricmc.net/use/installer) ≥ 0.16 installieren
2. [Fabric API](https://modrinth.com/mod/fabric-api) herunterladen
3. Neueste `voxelclient-x.x.x.jar` von den [Releases](https://github.com/VoxelLabs-Minecraft/voxelclient/releases/latest) herunterladen
4. Beide `.jar`-Dateien in `.minecraft/mods/` legen
5. Minecraft 1.21.4 starten

---

# ✦ Features

## 📊 Custom HUD

Ein übersichtliches, anpassbares In-Game-Display:

- FPS-Counter
- XYZ-Koordinaten
- Blickrichtung
- Rüstungsanzeige
- Speedometer
- Ping-Anzeige (farbcodiert)
- CPS Counter (LMB / RMB)
- Keystrokes Overlay (WASD, SPACE, LMB, RMB)

Alle HUD-Elemente sind frei verschiebbar.

---

## 🖱️ HUD-Editor

Unter **Settings → UI → HUD-Editor** kannst du:

- Alle HUD-Elemente per Drag-and-Drop verschieben
- Positionen dauerhaft speichern
- Layout individuell anpassen

Gespeichert wird in:

```
voxelclient/hud_positions.json
```

Zusätzlich besitzen alle UI-Screens eine moderne **Fade + Slide Animation (250ms, easeOutCubic)**.

---

## 🎮 Gameplay Features

### 🏃 Toggle Sprint
Sprint dauerhaft aktiv halten (Standard: `R`).  
Deaktiviert sich automatisch beim Stoppen.

### 🕵️ Toggle Sneak
Sneak dauerhaft aktivieren (Taste konfigurierbar).

### 🧭 Snap Look
Rastet deine Kamera auf den nächsten 45°-Winkel ein:  
N, NE, E, SE, S, SW, W, NW  
(Taste konfigurierbar)

---

## 🔍 Smooth Zoom

Sanftes Hereinzoomen mit cinematischem Easing-Effekt.  
Zoom-Stärke per Mausrad feinjustierbar.

**Standard-Taste:** `C`

---

## 👁️ Freelook

Die Kamera unabhängig vom Spielermodell frei drehen — ideal für PvP, Erkundung und Cinematics.

**Standard-Taste:** `Left Alt`

---

## 📍 Waypoints System

Speichere und verwalte Wegpunkte direkt im Spiel.

- Wegpunkte erstellen & löschen
- Richtungsanzeige am Bildschirmrand
- Verwaltung unter **Settings → Utility → Wegpunkte**

Speicherung in:

```
voxelclient/waypoints.json
```

### ☠ Death Waypoint
Beim Tod wird automatisch ein Wegpunkt erstellt:

- Speichert Koordinaten + Uhrzeit
- Maximal 10 Death-Waypoints gespeichert

---

## 🛡️ Armor Durability Warning

- Zeigt Haltbarkeit aller Rüstungsteile
- 🟠 Orange bei < 50%
- 🔴 Rot bei < 20%

---

## 📊 CPS Counter

Zeigt Klicks pro Sekunde für:

- LMB
- RMB

Basiert auf einem 1-Sekunden-Sliding-Window.

---

## 🌐 Ping Anzeige

Zeigt aktuellen Server-Ping:

- 🟢 Grün < 50ms
- 🟡 Mittel
- 🔴 Rot > 300ms

---

## ⌨️ Keystrokes Overlay

Animierte Anzeige von:

- W, A, S, D
- SPACE
- LMB
- RMB

Standard-Position: unten rechts  
Im HUD-Editor frei verschiebbar.

---

## 💬 Chat Zeitstempel

Fügt automatisch einen Zeitstempel vor jede Nachricht:

```
[HH:mm] Nachricht
```

---

## 🎭 Cosmetics

Lade deinen eigenen Cape über eine beliebige PNG-URL.  
Das Cape wird asynchron geladen, ohne den Spielstart zu verzögern.

---

## ✦ Spieler-Badges

Vor jedem Spielernamen erscheint ein Badge:

- **Grau** `✦` → normaler Spieler
- **Dunkelrot** `✦` → VoxelClient Creator

Sichtbar:

- In der Tab-Liste
- Über dem Kopf

---

## ⚙️ Settings GUI

Modernes In-Game-Menü mit Tab-Navigation.

- Alle Features konfigurierbar
- JSON-basierte Speicherung
- Utility-, UI- und Gameplay-Bereiche

**Standard-Taste:** `Right Shift`

---

## 🔔 Auto-Updater

Beim Start wird automatisch geprüft, ob eine neue Version verfügbar ist.

- Goldenes Banner im Hauptmenü
- Direktlink zur Download-Seite
- Taste `U` öffnet Update-Seite

---

## 🎮 Discord Rich Presence

Zeigt deinen aktuellen Status in Discord:

- Hauptmenü
- Singleplayer
- Multiplayer
- Realms

Server-Logos werden automatisch angezeigt.

---

# ⌨️ Keybinds (Standard)

| Funktion | Taste |
|----------|-------|
| Settings öffnen | `Right Shift` |
| Zoom | `C` (halten) |
| Freelook | `Left Alt` (halten) |
| Toggle Sprint | `R` |
| Update-Seite öffnen | `U` |
| Snap Look | konfigurierbar |
| Toggle Sneak | konfigurierbar |

Alle Tasten sind unter  
**Minecraft → Optionen → Tastenbelegung → VoxelClient**  
frei anpassbar.

---

# 📋 Voraussetzungen

| Was | Version |
|------|----------|
| Minecraft | 1.21.4 |
| Fabric Loader | ≥ 0.16 |
| Fabric API | 1.21.4 |
| Java | 21+ |

---

# 🏗️ Selbst bauen

```bash
git clone https://github.com/VoxelLabs-Minecraft/voxelclient.git
cd voxelclient
./gradlew build
# → build/libs/voxelclient-x.x.x.jar
```

Client direkt starten:

```bash
./gradlew runClient
```

---

# 📄 Lizenz

MIT License — siehe  
https://github.com/VoxelLabs-Minecraft/voxelclient/blob/main/LICENSE.txt

---

<div align="center">
  Made with ♥ by VoxelLabs · Plantaria.net · ave.rip
</div>