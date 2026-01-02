# LibreTube - Local Edition

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Logo" width="128" height="128" />
  <br>
  <i>A private, local-only Android frontend for YouTube.</i>
</div>

<br>

**LibreTube Local Edition** is a specialized fork developed to transform LibreTube into a fully self-contained media player. It eliminates all reliance on external Piped instances, ensuring absolute privacy and zero data leakage. Your subscriptions, playlists, and history never leave your device.

---

## 🚀 Exclusive Features

This fork introduces powerful capabilities not found in the official release:

### 🎧 Advanced Audio Experience
- **Inline Video Switch**: Instantly toggle between audio-only and video modes directly within the Audio player interface by replace the thumbnail with the video player.
### 📥 Power-User Downloads
- **Massive Playlist Support**: Specifically engineered to handle downloading huge local playlists. The optimized engine ensures stability and performance even when processing libraries with hundreds of tracks.

### 🔄 Integrated Self-Update System
- **Fork-Aware Updates**: Includes a custom `UpdateManager` that tracks *this specific repository*, ensuring you receive updates for the Local Edition rather than the official mainline.
- **Smart Background Checks**: A configurable `UpdateWorker` (15m to 24h) silently checks for new releases, keeping your local version current without manual intervention.

---

## 🛠️ Building from Source

To build this project locally, you will need Android Studio and the Android SDK.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/akashsriramganapathy/LibreTube.git
    cd LibreTube
    ```

2.  **Open in Android Studio:**
    - Select `File > Open` and choose the cloned directory.
    - Allow Gradle to sync.

3.  **Build:**
    - Select `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

---

## 🤝 Credits

This project is a personal fork of **[LibreTube](https://github.com/libre-tube/LibreTube)**, originally created and maintained by **[Bnyro](https://github.com/Bnyro)**. Massive credit to the original team for their incredible work on the base application.

**Fork Contributions:**
- **Design & Assets**: [XelXen](https://github.com/XelXen), [Margot Albert-Heuzey](https://margotdesign.ovh)
- **Screenshots & Testing**: [ARBoyGo](https://github.com/ARBoyGo)
- **Emoji Assets**: [OpenMoji](https://openmoji.org)

---

## 📄 License

LibreTube is Free Software: You can use, study, share and modify it at your will. The app can be redistributed and/or modified under the terms of the [GNU General Public License version 3 or later](https://www.gnu.org/licenses/gpl.html).

<div align="center">
  <img src="https://www.gnu.org/graphics/gplv3-127x51.png" alt="GPLv3" />
</div>