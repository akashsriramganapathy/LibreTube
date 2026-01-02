<div align="center">
  <img src="assets/banners/gh-banner.png" width="auto" height="auto" alt="LibreTube">

[![GPL-v3](assets/widgets/license-widget.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
</div><div align="center" style="width:100%; display:flex; justify-content:space-between;">

[![Matrix](assets/widgets/mat-widget.svg)](https://matrix.to/#/#LibreTube:matrix.org)
[![Mastodon](assets/widgets/mast-widget.svg)](https://fosstodon.org/@libretube)
[![Lemmy](assets/widgets/lemmy-widget.svg)](https://feddit.rocks/c/libretube)

</div>

> **Note** <br>
> We don't accept feature or bug requests on these platforms. Kindly submit requests only on GitHub.

</div>

## 🤖 About this Fork
**This is a personal fork of LibreTube, maintained and enhanced using Artificial Intelligence.**
It creates a self-sustaining ecosystem where the app updates itself from this repository, and this repository keeps itself up-to-date with the official upstream source.

### ✨ Added Features
- **Self-Updater**: 
  - Integrated `UpdateManager` that downloads and installs updates directly within the app.
  - Uses Android's `PackageInstaller.Session` API for seamless updates.
- **Smart Auto-Sync**: 
  - Automated GitHub Action that polls the official LibreTube repository every 30 minutes.
  - Automatically merges upstream changes and builds a new release only when the official build is successful.
- **Background Updates**:
  - `UpdateWorker` runs in the background (WorkManager) to check for updates even when the app is closed.
  - Configurable update frequency (15m to 24h) with battery drain warnings.
- **Custom Update Source**:
  - The app is hardcoded to check *this* repository for updates, ensuring you stay on this custom fork while getting all official improvements.
- **Audio Player Improvements (Experimental)**:
  - **Inline Video in Audio Mode**: View the video feed directly within audio mode.
  - **Persistent Video on Minimize**: Video continues playing in the mini-player instead of reverting to a thumbnail.
  - **Picture-in-Picture Support**: Enable PiP directly from the audio player.
  - **Enhanced Title Display**: Support for 2-line titles without scrolling marquee.

<h2 align="left">
<sub>
<img  src="assets/readme/ltvnp.svg"
      height="30"
      width="30">
</sub>
Differences to NewPipe
</h2>

With LibreTube, you have the choice to either send all requests directly to YouTube or proxy them via Piped for better privacy. Piped acts as a middleman server between you and YouTube in this case, which prevents YouTube from accessing personal information such as your IP address. 
Additionally, using Piped allows you to sync your subscriptions between LibreTube and Piped, which can be used on desktop too.

While LibreTube only supports YouTube, NewPipe also allows the use of other platforms like SoundCloud, PeerTube, Bandcamp and media.ccc.de.<br>
Both are great clients for watching YouTube videos. It depends on the individual's use case which one fits their needs better.

<h2 align="left">
<sub>
<img  src="assets/readme/privacy.svg"
      height="30"
      width="30">
</sub>
Privacy Policy and Disclaimer
</h2>


LibreTube aims to protect the privacy of its users. [Our Privacy Policy](/PRIVACY_POLICY.md) gives detailed information on which data the app stores in order to work, how it is being used, and how the project protects your personal information. It is recommended to read the privacy policy of LibreTube as well as the privacy policy of the instance you have chosen inside the app.

## License
[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

LibreTube is [Free Software](https://en.wikipedia.org/wiki/Free_software): You can use, study, share and modify it at your will. The app can be redistributed and/or modified under the terms of the
[GNU General Public License version 3 or later](https://www.gnu.org/licenses/gpl.html) published by the 
[Free Software Foundation](https://www.fsf.org/).

<div align="right">
<table><td>
<a href="#start-of-content">↥ Scroll to top</a>
</td></table>
</div>
