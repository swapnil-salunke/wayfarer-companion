---
name: add-photos-ios-sim
description: Add photos to a Wayfarer journal entry in the iOS Simulator (new or edit), the way the user does it. Use when asked to add/attach photos, reproduce the photo flow, or test photo thumbnails, the share-card pager, or photo removal.
---

# Add photos to an entry in the iOS Simulator

Derived from the user's screen recording `recordings/add-images.mp4` (iPhone 17 Pro, iOS 27.1).
Use the `mcp__Claude_Code_iOS_Simulator__control` tool. Coordinates are device points (402x874);
a screenshot shown at 920px wide maps to points by dividing by 2.29.

## Preconditions
- App built and running: `./run-ios.sh` (bundle id `com.wayfarer.app.ios`). Wait ~8s after launch; the
  first screen is blank/white until Compose loads. The UI lags, so `sleep 3` before screenshots.
- The simulator photo library already holds sample photos (flowers, waterfalls).

## Flow
1. **Open the editor**
   - New entry: tap the amber + FAB (358, 797).
   - Existing entry: tap **Edit** on its card (~129, 438).
2. **Tap "Add photos"** (outlined teal button). With no photos it sits at ~y=485; with a row of
   thumbnails it moves down to ~y=590. Label reads "N photo(s) - add more" once photos exist.
3. **iOS picker sheet opens** ("Select up to 5 photos."). Tap tiles to tick them (blue check appears
   bottom-right of the tile). Grid is 3 columns: x ~ 67 / 201 / 335, first row y ~ 430, second ~ 515.
   Max 5 photos per pick (`rememberPhotoPickerLauncher(maxItems = 5)`).
4. **Confirm**: tap the blue check at top right (~362, 143). Sheet closes.
5. **Result**: an 88dp thumbnail row appears above the add button, each with a circled-cross at its top
   right (tap to remove before saving, ~44dp target), and the button reads "N photo(s) - add more".
6. **Save** (bottom button, ~200, 793) returns to the timeline. Verify with a screenshot.

## Gotchas
- `text` input goes to the start of a focused field; not relevant for photos.
- Tapping empty space clears keyboard focus (editor has tap-outside handling).
- **Teal issue (two different causes, check which one you see):**
  1. *Blank teal squares/cards where photos should be.* Teal `#2E5B63` is `WfColors.PhotoPlaceholder`,
     the background drawn behind every `AsyncImage` (timeline strip, edit thumbnails, share card). It shows
     whenever Coil can't load `file://<path>`. Seen on the entry saved at 22:37 in an earlier session; after
     re-picking photos and saving, the same entry rendered correctly. Picked photos are copied to the app's
     `Documents` dir (`IosPlatform.kt` ~line 53), so the earlier failure was NOT a temp-dir cleanup; the
     cause is not confirmed (suspect stale absolute paths saved by an earlier build/container). Diagnose by
     comparing the entry's stored paths with `xcrun simctl get_app_container booted com.wayfarer.app.ios data`.
     Workaround: Edit the entry, re-add photos, remove the dead ones with the circled-cross, Save.
  2. *Teal flash when dragging past the first/last photo on the share card.* The pager's iOS bounce
     revealed the card's teal background. Fixed by `overscrollEffect = null` on the `HorizontalPager` in
     `SharePreview.kt`. The simulator tool can't screenshot mid-drag, so Claude could not see it; the user confirmed the fix works (no drag past the last image).
  No new recording of the issue was available when this note was written (only `recordings/add-images.mp4`);
  if the user records it again, extract frames (AVAssetImageGenerator script, no ffmpeg on this Mac) and update this note.
- Share card page (tap **Share** ~68, 438): swipe the card horizontally to page through photos; dots below.
- To record a run: `xcrun simctl io booted recordVideo --codec h264 --force recordings/<name>.mp4 &`,
  stop with `pkill -INT -f "simctl io booted recordVideo"`.
