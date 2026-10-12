package com.wayfarer.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.wayfarer.domain.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference

// GUIDE: Module 8 (picker, activity access) and Module 9.3 (Android share).

@Composable
actual fun rememberPhotoPickerLauncher(maxItems: Int, onPicked: (List<PickedMedia>) -> Unit): PhotoPickerLauncher {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        // Picker URIs are temporary -> copy into app storage right away. Guide: Module 8.3 "copy the file".
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val picked = uris.mapIndexed { i, uri ->
            val out = File(dir, "${System.currentTimeMillis()}-$i.jpg")
            context.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
            PickedMedia(out.absolutePath, context.contentResolver.getType(uri) ?: "image/jpeg")
        }
        if (picked.isNotEmpty()) latest.value(picked)
    }
    return remember(launcher) {
        object : PhotoPickerLauncher {
            override fun launch() = launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

actual fun ImageBitmap.toPngBytes(): ByteArray = ByteArrayOutputStream().also {
    asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
}.toByteArray()

class AndroidFileStore(private val context: Context) : FileStore {
    override fun writeCache(name: String, bytes: ByteArray): String {
        val dir = File(context.cacheDir, "share").apply { mkdirs() } // must match res/xml/file_paths.xml
        return File(dir, name).also { it.writeBytes(bytes) }.absolutePath
    }
}

/** Remembers the foreground Activity so services (not composables) can start intents. Guide: Module 8.2 */
class CurrentActivityProvider(app: Application) {
    private var ref = WeakReference<Activity>(null)
    init {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { ref = WeakReference(a) }
            override fun onActivityCreated(a: Activity, s: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, o: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
    /** Koin builds this lazily, after the first onResume has fired, so the host Activity seeds it. */
    fun seed(a: Activity) { ref = WeakReference(a) }
    fun current(): Activity? = ref.get()
}

class AndroidShareService(
    private val context: Context,
    private val activities: CurrentActivityProvider,
    private val config: ShareConfig,
) : ShareService {

    override suspend fun share(request: ShareRequest): Outcome<ShareOutcome, DomainError> {
        val activity = activities.current() ?: return Outcome.Err(DomainError.Platform("No screen to share from."))
        val uri = request.imagePath?.let { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it)) }
        if (request.target == ShareTarget.InstagramStories && uri != null) {
            val ig = Intent("com.instagram.share.ADD_TO_STORY").apply {
                putExtra("source_application", config.facebookAppId)
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Needs <queries> for com.instagram.android in the manifest, otherwise this is always null.
            if (ig.resolveActivity(activity.packageManager) != null) {
                activity.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(ig)
                return Outcome.Ok(ShareOutcome.Shared)
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, request.text)
            if (uri != null) { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            else type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(send, null))
        // Android does not tell us whether the user finished sharing, so "Shared" means "sheet shown".
        return Outcome.Ok(if (request.target == ShareTarget.InstagramStories) ShareOutcome.UsedFallbackSheet else ShareOutcome.Shared)
    }
}
