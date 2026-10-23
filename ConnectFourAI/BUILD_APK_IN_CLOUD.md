# Build the APK in GitHub Actions

This environment does not contain the Android SDK or Gradle, and network access is unavailable, so it cannot compile an APK locally. The included workflow lets GitHub's hosted runner build it instead.

1. Create a GitHub repository and upload the contents of this folder to its `main` branch.
2. Open the repository's **Actions** tab.
3. Select **Build Android APK** and choose **Run workflow** (or wait for the first push workflow).
4. Open the completed run and download the `ConnectFourAI-debug-apk` artifact.
5. Extract the downloaded artifact ZIP and install `app-debug.apk` on your Android phone. You may need to allow installation from your file manager.

This creates a debug APK, not a Play Store release. The app remains an untested prototype; validate board recognition and tap coordinates on the actual TikPvP screen before enabling automatic play.
