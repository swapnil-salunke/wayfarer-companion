package com.wayfarer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// GUIDE: Module 8 "Platform APIs" - two tools: (1) interface + DI (services), (2) expect/actual (tiny functions).

/** Result of the photo picker. `path` is a COPY inside the app's own storage (picker URIs are temporary). */
data class PickedMedia(val path: String, val mimeType: String)

interface PhotoPickerLauncher { fun launch() }

/** Pickers need an Activity / view controller, so they are Composables, not injected singletons. Guide: Module 8.3 */
@Composable
expect fun rememberPhotoPickerLauncher(maxItems: Int, onPicked: (List<PickedMedia>) -> Unit): PhotoPickerLauncher

/** Writes bytes to a cache file and returns its absolute path. */
interface FileStore { fun writeCache(name: String, bytes: ByteArray): String }

/** Tiny expect/actual: turning a Compose bitmap into PNG bytes differs per platform. Guide: Module 9.4 */
expect fun ImageBitmap.toPngBytes(): ByteArray

/** Keys that live outside the code (Guide: Module 9.3, Instagram needs a Facebook App ID). */
data class ShareConfig(val facebookAppId: String)
