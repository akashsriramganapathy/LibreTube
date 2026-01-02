## 🤖 About this Fork
**This is a personal, "Local-Only" fork of LibreTube, maintained and enhanced using Artificial Intelligence.**
It transforms LibreTube into a fully self-contained media player that prioritizes local data privacy and offline capability, removing dependence on external Piped instances.

### ✨ Key Features

#### 🔒 Full Local Mode
- **No Piped Backend**: All reliance on Piped instances, proxy servers, and remote account management has been completely removed.
- **Local Data Only**: All playlists, subscriptions, and watch history are stored locally on your device in a secure database.
- **Privacy First**: Your viewing habits and data never leave your device.

#### 🔄 Self-Updater
- **Integrated Updates**: Includes a custom `UpdateManager` that downloads and installs updates directly from this repository.
- **Background Checks**: `UpdateWorker` automatically checks for updates in the background (configurable from 15m to 24h) to ensure you're always running the latest version.
- **Seamless Upgrade**: Uses Android's `PackageInstaller` for smooth in-app updates.
- **Custom Source**: Hardcoded to track *this* specific fork, ensuring you don't accidentally revert to the official mainline release.

#### 🎧 Audio Player Improvements (Experimental)
- **Inline Video Toggle**: Switch between audio-only and video mode instantly within the player.
- **Persistent Playback**: Video playback continues seamlessly in the mini-player without reverting to a static thumbnail.
- **Picture-in-Picture**: Enhanced PiP support directly from the audio player interface.
- **Better Metadata**: Support for 2-line titles in the player notification and UI, removing the need for slow scrolling marquees.

### ⚡ Automated Upstream Sync
- This repository utilizes a "Smart Auto-Sync" GitHub Action that checks the official LibreTube repository every 30 minutes.
- It automatically merges upstream improvements while resolving conflicts to preserve the "Local Mode" architecture, ensuring you get the best of both worlds: official bug fixes + exclusive local features.

## 🤝 Credits

This project is a personal fork of **LibreTube**, originally created and maintained by [Bnyro](https://github.com/Bnyro). A huge thanks to them for their work!

**Special Thanks:**
- **Design & Banners**: [XelXen](https://github.com/XelXen)
- **Screenshots**: [ARBoyGo](https://github.com/ARBoyGo)
- **Icons**: [XelXen](https://github.com/XelXen) & [Margot Albert-Heuzey](https://margotdesign.ovh)
- **Emojis**: [OpenMoji](https://openmoji.org)


## License
[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

LibreTube is [Free Software](https://en.wikipedia.org/wiki/Free_software): You can use, study, share and modify it at your will. The app can be redistributed and/or modified under the terms of the
[GNU General Public License version 3 or later](https://www.gnu.org/licenses/gpl.html) published by the 
[Free Software Foundation](https://www.fsf.org/).