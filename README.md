<div align="center">
  <img src="assets/banners/gh-banner.png" width="auto" height="auto" alt="LibreTube">

[![GPL-v3](assets/widgets/license-widget.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
</div><div align="center" style="width:100%; display:flex; justify-content:space-between;">

[![Matrix](assets/widgets/mat-widget.svg)](https://matrix.to/#/#LibreTube:matrix.org)
[![Mastodon](assets/widgets/mast-widget.svg)](https://fosstodon.org/@libretube)
[![Lemmy](assets/widgets/lemmy-widget.svg)](https://feddit.rocks/c/libretube)


</div>

<br>

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


---

<details>
  <summary>📜️ Credits</summary>

<sub>Original Project by [Bnyro](https://github.com/Bnyro) - A huge thanks for creating and maintaining LibreTube!</sub> <br>
<sub>Readme Design and Banners by [XelXen](https://github.com/XelXen)</sub> <br>
<sub>Readme Screenshots by [ARBoyGo](https://github.com/ARBoyGo)</sub> <br>
<sub>Readme Emoji is from [openmoji](https://openmoji.org)</sub>

  <summary>Icons</summary>

<sub>[Default App Icon](https://github.com/libre-tube/LibreTube/blob/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png) by [XelXen](https://github.com/XelXen)</sub> <br>
<sub>[Boosted Bird](https://github.com/libre-tube/LibreTube/blob/master/app/src/main/res/mipmap-xxxhdpi/ic_bird_round.png) by [Margot Albert-Heuzey](https://margotdesign.ovh)</sub>

</details>

<h2 align="left">
<sub>
<img  src="assets/readme/about.svg"
      height="30"
      width="30">
</sub>
About
</h2>

YouTube has an extremely invasive [privacy policy](https://support.google.com/youtube/answer/10364219) which relies on using user data in unethical ways. They store a lot of your personal data - ranging from ideas, music taste, content, political opinions, and much more than you think.

This project is aimed at improving the users' privacy by being independent from Google and bypassing their data collection.

Therefore, the app is using the [Piped API](https://github.com/TeamPiped/Piped), which uses proxies to circumvent Google's data collection and includes some other additional features.

If you have questions or need help, please make sure to read the [FAQ](https://libre-tube.github.io/#faq) before asking for help at the community channels. The [Matrix room](https://matrix.to/#/#LibreTube:matrix.org) is considered as the main communication channel, all other forums or social media accounts are maintained by volunteers from the community but not the developer(s).

<h2 align="left">
<sub>
<img  src="assets/readme/phone.svg"
      height="30"
      width="30">
</sub>
Screenshots
</h2>

<div style="width:100%; display:flex; justify-content:space-between;">

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg" width=19% alt="Home">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_1.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg" width=19% alt="Home">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_2.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg" width=19% alt="Subscriptions">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_3.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg" width=19% alt="Library">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_4.jpg)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_9.jpg" width=19% alt="Channel Overview">](fastlane/metadata/android/en-US/images/phoneScreenshots/Screenshot_9.jpg)

* More screenshots can be found [here](https://github.com/libre-tube/LibreTube/blob/master/SCREEN_SHOT.md)

</div>

<h2 align="left">
<sub>
<img  src="assets/readme/feature.svg"
      height="30"
      width="30">
</sub>
Features
</h2>

- [x] No Ads or Tracking
- [x] Subscriptions
- [x] Subscription Groups
- [x] User Playlists
- [x] Playlist Bookmarks
- [x] Watch/Search History
- [x] SponsorBlock
- [x] ReturnYouTubeDislike
- [x] DeArrow
- [x] Downloads
- [x] Background playback
- [x] User Accounts via Piped (optional)

<h2 align="left">
<sub>
<img  src="assets/readme/community.svg"
      height="30"
      width="30">
</sub>
Contributing
</h2>

Whether you have ideas, translations, design changes, code cleaning or really heavy code changes, help is always welcome. The more is done, the better it gets! Please respect our [Code of Conduct](https://github.com/libre-tube/LibreTube/blob/master/CODE_OF_CONDUCT.md) in order to keep all interactions and discussions healthy.

You can open and build the project like any other normal Android project by using Android Studio.

Please make sure the title of your pull request and the commit messages follow the [conventional commit types](https://github.com/commitizen/conventional-commit-types/blob/master/index.json) (e.g. `feat: support for xy`).
For instance, the most common commit types are "feat", "fix", "refactor", "ci" and "chore".

> [!NOTE]
> Any issue avoiding the issue template will be ignored and forced to be closed.

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
