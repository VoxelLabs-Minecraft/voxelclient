# VoxelClient

> Ein Fabric Client-Mod für Minecraft 1.21.4 mit Custom HUD, Zoom, Freelook, Cosmetics und mehr.

---

## 📥 Installation

### Automatisch (empfohlen)

1. **Installer herunterladen** → [Releases](cdn.voxellabs.de/s/installer)
2. `voxelclient-installer.exe` ausführen
3. Den gewünschten `mods/`-Ordner auswählen (wird automatisch erkannt)
4. Auf **„Jetzt installieren"** klicken
5. Minecraft mit dem **Fabric Launcher** starten

Der Installer lädt automatisch die neueste Version sowie die benötigte **Fabric API** herunter.

### Manuell

1. [Fabric Loader](https://fabricmc.net/use/installer) ≥ 0.16 installieren
2. [Fabric API](https://modrinth.com/mod/fabric-api) herunterladen
3. Neueste `voxelclient-x.x.x.jar` von den [Releases](https://github.com/VoxelLabs-Minecraft/voxelclient/releases/latest) herunterladen
4. Beide `.jar`-Dateien in `.minecraft/mods/` legen
5. Minecraft 1.21.4 starten

---

## ✦ Features

### 📊 Custom HUD
Ein übersichtliches In-Game-Display mit FPS-Counter, XYZ-Koordinaten, Blickrichtung, Rüstungszustand und Speedometer — alles farbig und klar dargestellt direkt im Sichtfeld.

### 🔍 Smooth Zoom
Sanftes Hereinzoomen mit cinematischem Easing-Effekt. Die Stärke lässt sich bequem per Mausrad feinjustieren. Standard-Taste: `C`

### 👁️ Freelook
Die Kamera unabhängig vom Spielermodell frei drehen — ideal für PvP, Erkundung und Cinematics. Standard-Taste: `Left Alt`

### 🎭 Cosmetics
Lade deinen eigenen Cape über eine beliebige PNG-URL. Das Cape wird asynchron im Hintergrund geladen, ohne den Spielstart zu verzögern.

### ✦ Spieler-Badges
Vor jedem Spielernamen erscheint ein kleines Badge — in der Tab-Liste sowie über dem Kopf:
- **Grau** `✦` → normaler Spieler
- **Dunkelrot** `✦` → VoxelClient Creator

### ⚙️ Settings GUI
Ein vollständiges In-Game-Einstellungsmenü mit Tab-Navigation. Alle Einstellungen werden automatisch als JSON gespeichert. Standard-Taste: `Right Shift`

### 🔔 Auto-Updater
Beim Start wird automatisch geprüft ob eine neue Version auf GitHub verfügbar ist. Ein goldenes Banner im Hauptmenü informiert dich — ein Klick öffnet die Download-Seite.

### 🎮 Discord Rich Presence
Zeigt deinen aktuellen Status in Discord an — ob du im Hauptmenü bist, auf einem Server spielst, Singleplayer oder Realms. Server-Logos werden automatisch angezeigt.

---

## ⌨️ Keybinds

| Funktion | Taste |
|---|---|
| Settings öffnen | `Right Shift` |
| Zoom | `C` (halten) |
| Freelook | `Left Alt` (halten) |
| Update-Seite öffnen | `U` |

Alle Tasten sind in den Minecraft-Einstellungen unter **Tastenbelegung → VoxelClient** frei anpassbar.

---

## 📋 Voraussetzungen

| Was | Version |
|---|---|
| Minecraft | 1.21.4 |
| Fabric Loader | ≥ 0.16 |
| Fabric API | beliebig (1.21.4) |
| Java | 21+ |

---

## 🏗️ Selbst bauen

```bash
git clone https://github.com/yourname/voxelclient
cd voxelclient
./gradlew build
# → build/libs/voxelclient-x.x.x.jar
```

Zum Testen direkt in Minecraft starten:
```bash
./gradlew runClient
```

---

## 📄 Lizenz

MIT License — siehe [LICENSE](https://github.com/VoxelLabs-Minecraft/voxelclient/blob/main/LICENSE.txt)

---

<div align="center">
  Made with ♥ by VoxelLabs &nbsp;·&nbsp; Plantaria.net &nbsp;·&nbsp; ave.rip
</div>