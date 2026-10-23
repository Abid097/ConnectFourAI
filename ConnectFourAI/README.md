# Connect Four AI for Android (TikPvP prototype)

This is a native Kotlin Android Studio project for a local-screen Connect Four assistant. It includes:

- Android MediaProjection consent flow and foreground screen-capture service.
- Basic pixel-based detection for the dark TikPvP Connect Four board and orange/pink discs.
- A depth-limited alpha-beta minimax move engine with immediate win/block checks.
- AccessibilityService gesture taps, turn-count gating, signature-based duplicate-tap prevention, and emergency stop.

## Important status

**This folder is source code, not a built APK.** The current execution environment has Java but no Android SDK, Gradle installation, or network access to download Android build tools, so I could not compile or test an APK here. Do not treat this prototype as ready for competitive or staked games. Screen recognition and tap coordinates must be validated on the actual phone and TikPvP layout.

## Build an APK

1. Install Android Studio on a computer and open this `ConnectFourAI` folder.
2. Allow Android Studio to install Android SDK Platform 35 and the matching build tools.
3. Sync the Gradle project and build using **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the resulting debug APK on your Android phone.
5. Open the app and grant screen-capture permission. Enable **Connect Four AI** in Android Settings > Accessibility.
6. Open TikPvP, return to the app, select your disc color and who moves first, then start auto-play. Keep the game visible. Use Emergency Stop to halt tapping.

## Known limitations

- Board detection is tuned to the dark board with a pink outline shown in the supplied screenshots. Other resolutions, themes, UI changes, popups, rotations, and animations may confuse it.
- Pixel classification is intentionally lightweight; check its status and test only in a non-staked match first.
- The AI uses depth-limited search (depth 7) and is strong but not guaranteed to win against optimal play.
- Android may stop screen capture when consent is revoked, the app is killed, or the screen is locked.
- Automated play may be disallowed by the game/service's rules. Use only where permitted.
