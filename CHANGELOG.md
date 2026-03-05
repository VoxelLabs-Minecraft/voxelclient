# Changelog — VoxelClient

## v0.0.2 — Quality of Life
> Grundfunktionen erweitern

### 🎮 Gameplay
- **Toggle Sprint** — Sprint dauerhaft aktiv halten (Taste: `R`). Deaktiviert sich automatisch wenn der Spieler stoppt.
- **Toggle Sneak** — Sneak dauerhaft aktiv halten (konfigurierbare Taste).
- **Snap Look** — Kamera auf den nächsten 45°-Winkel (N, NE, E, SE, S, SW, W, NW) einrasten (konfigurierbare Taste).

### 📊 HUD
- **Keystrokes Overlay (WASD)** — Zeigt W, A, S, D, SPACE, LMB & RMB als animierte Tasten an. Standard-Position: unten rechts.
- **CPS Counter** — Zeigt Klicks pro Sekunde (LMB / RMB) basierend auf 1-Sekunden-Sliding-Window.
- **Armor Durability Warning** — Zeigt Haltbarkeit aller Rüstungsteile. Warnung (🔴) bei < 20%, Orange bei < 50%.
- **Ping Anzeige** — Zeigt aktuellen Server-Ping farbcodiert (Grün < 50ms, Rot > 300ms).

### 🔧 Utility
- **Waypoints System** — Wegpunkte speichern, anzeigen und verwalten. Gespeichert in `voxelclient/waypoints.json`. Richtungsanzeige am Bildschirmrand. Verwaltung via Settings → Utility → Wegpunkte.
- **Death Waypoint** — Speichert automatisch beim Tod einen Wegpunkt mit Koordinaten und Uhrzeit. Bis zu 10 Death-Waypoints werden gespeichert.
- **Chat Zeitstempel** — Fügt automatisch `[HH:mm]` vor jede Chat-Nachricht ein.

### 🖥️ UI
- **UI Animationen** — Alle VoxelClient-Screens blenden jetzt mit einem Fade+Slide-Effekt ein (250ms, easeOutCubic).
- **Drag-and-Drop HUD** — HUD-Editor unter Settings → UI → HUD-Editor. Alle HUD-Elemente frei verschieben. Positionen werden in `voxelclient/hud_positions.json` gespeichert.

---

## v0.0.1 — Initial Release
- Custom HUD (FPS, XYZ, Blickrichtung, Rüstung, Speedometer)
- Smooth Zoom (`C`)
- Freelook (`Left Alt`)
- Cosmetics (Cape via PNG-URL)
- Spieler-Badges
- Settings GUI (`Right Shift`)
- Auto-Updater
- Discord Rich Presence
