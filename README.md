# Wayfarer – companion project

Real Kotlin Multiplatform code for the **Compose Multiplatform + Clean Architecture guide** (the "Two-day sprint" slice):
one shared module, Android + iOS, offline only, entries (create, edit, delete) + photos + timeline + share card (system sheet / Instagram Stories).
Server sync, trip list, location and desktop are intentionally left out (the DB already has `id`, `updatedAt`, `deleted` so sync can be added later).

## Read it in this order (file -> guide section)
| # | File | Guide |
|---|------|-------|
| 1 | `shared/build.gradle.kts`, `gradle/libs.versions.toml` | Module 2, Sprint block 1 |
| 2 | `domain/Core.kt`, `Ports.kt`, `UseCases.kt` | Module 3 (domain, ports, use cases) |
| 3 | `commonTest/*` | Module 12 (fakes, use-case tests) |
| 4 | `data/Db.kt`, `data/RoomEntryRepository.kt` | Module 4 (Room, repository) |
| 5 | `presentation/Theme.kt`, `Timeline.kt`, `Editor.kt` | Modules 5-6 (state, events, effects); `Theme.kt` holds the design tokens |
| 6 | `presentation/Navigation.kt`, `WayfarerApp.kt` | Modules 2.4, 7 |
| 7 | `platform/Platform.kt` + `androidMain` / `iosMain` platform files | Module 8 (pickers, expect/actual) |
| 8 | `presentation/SharePreview.kt` + share services | Module 9 (share card, Android/iOS/Instagram) |
| 9 | `di/*` | Module 10 (Koin) |
| 10 | `androidApp/*`, `iosApp/*` | Module 2.4 (thin app shells), 9.3 (manifest, FileProvider, Info.plist) |

Dependency rule to notice: `domain` imports nothing from `data`, `presentation` or `platform`.

## What the app does
- **Timeline:** entry cards with date, title, place, text preview and a photo strip; Share / Edit / Delete per card; empty state.
- **New / edit entry:** title, place, text, photos from the system picker (up to 5 per pick). Each photo is a thumbnail with a circled-cross to remove it before saving. Tap outside a field or press Done to dismiss the keyboard; Save stays pinned above the keyboard.
- **Share card:** swipe horizontally through all the entry's photos (page dots below); the title / place / WAYFARER overlay stays fixed. Share opens the system sheet, Instagram posts to Stories.
- Edit uses `UpdateEntryUseCase`; `Route.Editor(entryId)` is null for a new entry.

## Set up on another machine
The repo is **private**, so you must be signed in to GitHub as `swapnil-salunke`.

```bash
# one-time: sign in (or have an SSH key added to your GitHub account)
gh auth login

gh repo clone swapnil-salunke/wayfarer-companion
# or: git clone git@github.com:swapnil-salunke/wayfarer-companion.git
cd wayfarer-companion
```

**What to install**
| For | You need |
|-----|----------|
| Everything | JDK 21. The Gradle wrapper (9.6.0) downloads itself; `gradle/gradle-daemon-jvm.properties` asks for a JetBrains JDK 21 and Gradle fetches it automatically if none is installed. |
| Android | Android Studio (current stable) with the Android SDK; the project compiles against API 36 (`compileSdk = 36`, `minSdk = 26`). Create an emulator in Device Manager (a Pixel on API 34 worked). |
| iOS | A Mac with Xcode (built with Xcode 27) and an iOS simulator runtime. Verified on Apple Silicon only (the Xcode build phase links `:shared:linkDebugFrameworkIosSimulatorArm64`). |

**First run**
1. **Open the folder in Android Studio** and let it sync. It creates `local.properties` with your `sdk.dir` (this file is gitignored and machine-specific). Without Android Studio, create it yourself: `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties` (adjust the path), or export `ANDROID_HOME`.
2. **Android:** start an emulator, then `./gradlew :androidApp:installDebug` (or press Run on `androidApp`). First sync downloads a lot of dependencies, so give it a few minutes.
3. **iOS:** `./run-ios.sh "iPhone 17 Pro"`. The script boots the simulator, runs Xcode's build (which calls Gradle to build the shared framework) and launches the app. If your Xcode has a different iOS runtime, edit the `OS=26.2` in the `-destination` line of `run-ios.sh` or pick a simulator you actually have (`xcrun simctl list devices available`).
4. **Real iPhone (optional):** open `iosApp/iosApp.xcodeproj`, set your own Team under Signing (the project uses a `${TEAM_ID}` placeholder), and change the bundle id `com.wayfarer.app.ios` if it is taken.

**Gotchas**
- The first iOS build is slow (Kotlin/Native); later ones are fast.
- Photos: a fresh simulator or emulator gallery may be empty. iOS Simulator has sample photos; for Android `adb push` JPEGs to `/sdcard/Pictures/`.
- Data lives only on the device (Room database in the app's storage), so a new machine starts with an empty timeline.
- Nothing secret is committed. Instagram Stories needs your own Facebook App ID in `MainActivity.kt` and `IosPlatformModule.kt`.

## Run it
**iOS (simulator, Mac only):** `./run-ios.sh` builds the `iosApp` Xcode project (it links the shared framework) and launches it on an iPhone 17 Pro simulator. Pass another simulator name as the first argument; the script pins `OS=26.2` in the destination, so edit it if you use a different runtime.
**Android:** `./gradlew :androidApp:assembleDebug`, install `androidApp/build/outputs/apk/debug/androidApp-debug.apk` on an emulator or device, or run `androidApp` from Android Studio.
**Instagram Stories:** replace `YOUR_FACEBOOK_APP_ID` (`MainActivity.kt`, `IosPlatformModule.kt`) with your Facebook App ID.
**Emulator photos:** a fresh emulator has an empty gallery; `adb push` some JPEGs to `/sdcard/Pictures/` and run a media scan to see them in the picker.

## Honest status
- **Built and run:** the app compiles and runs on the iOS Simulator (iPhone 17 Pro, iOS 27.1, Xcode 27) and the Android emulator (Pixel 3a, API 34). Walked through by hand on both: create, edit, remove a photo, save, timeline, share card pager, keyboard dismissal, system share sheet.
- **Not verified:** the Instagram Stories path (needs a real Facebook App ID and Instagram installed), the image attached to the Android share sheet via FileProvider, any physical device, and the `commonTest` unit tests (not run in this pass). `UpdateEntryUseCase` has no tests yet.
- **Known quirk:** photos picked in an earlier build can render as blank teal squares (teal is the image placeholder colour). It cleared once the photos were re-added in the edit screen; root cause not confirmed.
- **Versions:** the versions in `gradle/libs.versions.toml` resolve and build today. Entries marked `check` there were guesses that happened to work; bump them deliberately rather than assuming they are canonical.
- **If Gradle won't sync:** create a fresh project with the official Kotlin Multiplatform wizard (Android + iOS, Compose UI shared), then copy `shared/src/**` over its `composeApp` sources and add the dependencies from `shared/build.gradle.kts`.
- **Simplifications vs the guide:** plain back stack instead of Navigation 3 (swap per Module 7); share card captured at device density, not fixed 1080x1920 (Module 9.4); no location, no "save to Photos"; system fonts (the design mockups use Bricolage Grotesque + DM Sans, not bundled yet).
