# Wayfarer – companion project

Real Kotlin Multiplatform code for the **Compose Multiplatform + Clean Architecture guide** (the "Two-day sprint" slice):
one shared module, Android + iOS, offline only, entries + photos + timeline + share card (system sheet / Instagram Stories).
Server sync, trip list, location and desktop are intentionally left out (the DB already has `id`, `updatedAt`, `deleted` so sync can be added later).

## Read it in this order (file -> guide section)
| # | File | Guide |
|---|------|-------|
| 1 | `shared/build.gradle.kts`, `gradle/libs.versions.toml` | Module 2, Sprint block 1 |
| 2 | `domain/Core.kt`, `Ports.kt`, `UseCases.kt` | Module 3 (domain, ports, use cases) |
| 3 | `commonTest/*` | Module 12 (fakes, use-case tests) |
| 4 | `data/Db.kt`, `data/RoomEntryRepository.kt` | Module 4 (Room, repository) |
| 5 | `presentation/Timeline.kt`, `Editor.kt` | Modules 5-6 (state, events, effects) |
| 6 | `presentation/Navigation.kt`, `WayfarerApp.kt` | Modules 2.4, 7 |
| 7 | `platform/Platform.kt` + `androidMain` / `iosMain` platform files | Module 8 (pickers, expect/actual) |
| 8 | `presentation/SharePreview.kt` + share services | Module 9 (share card, Android/iOS/Instagram) |
| 9 | `di/*` | Module 10 (Koin) |
| 10 | `androidApp/*`, `iosApp/*` | Module 2.4 (thin app shells), 9.3 (manifest, FileProvider, Info.plist) |

Dependency rule to notice: `domain` imports nothing from `data`, `presentation` or `platform`.

## Run it
**Android:** open this folder in Android Studio, sync, run `androidApp`.
**iOS:** needs a Mac. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`, then in Xcode create an iOS App project named `iosApp`,
replace its Swift files with `iosApp/iosApp/*.swift`, link the `Shared` framework (or use the official KMP wizard's Xcode project),
and merge `iosApp/Info.plist.additions.xml` into Info.plist.
**Instagram Stories:** replace `YOUR_FACEBOOK_APP_ID` (MainActivity.kt, IosPlatformModule.kt) with your Facebook App ID.

## Honest status – read this
- **Not compiled.** The build sandbox could not reach Google/Maven repositories and has no Android SDK or Xcode, so none of this has been built or run.
  Expect small fixes (imports, API signature drift), most likely in `IosPlatform.kt` (PHPicker / pasteboard interop) and Room/KSP setup.
- **Versions:** verified when the guide was written: Compose Multiplatform 1.12.1, Koin 4.2.1, Kotlin 2.2.20 recommended.
  Everything marked `check` in `libs.versions.toml` (AGP, KSP, Room, Coil, lifecycle, datetime, activity) is a best guess; bump if sync fails (KSP must start with your Kotlin version).
- **If Gradle won't sync:** create a fresh project with the official Kotlin Multiplatform wizard (Android + iOS, Compose UI shared), then copy `shared/src/**` over its `composeApp` sources and add the dependencies from `shared/build.gradle.kts`.
- **Simplifications vs the guide:** plain back stack instead of Navigation 3 (swap per Module 7); share card captured at device density, not fixed 1080x1920 (Module 9.4); no location, no "save to Photos".
