package com.wayfarer.platform

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.wayfarer.domain.*
import kotlinx.cinterop.*
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

// GUIDE: Module 8 (iOS picker) and Module 9.3 (iOS share). UNVERIFIED on a Mac: compile in Xcode and fix import nits.

actual fun ImageBitmap.toPngBytes(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes

/** The view controller currently on screen - iOS's version of "current Activity". Guide: Module 8.2 */
fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPhotoPickerLauncher(maxItems: Int, onPicked: (List<PickedMedia>) -> Unit): PhotoPickerLauncher {
    val latest = rememberUpdatedState(onPicked)
    // The delegate must be held strongly or iOS frees it before the user picks. Guide: Module 8.3 pitfall.
    val delegateHolder = remember { arrayOfNulls<Any>(1) }
    return remember {
        object : PhotoPickerLauncher {
            override fun launch() {
                val config = PHPickerConfiguration().apply {
                    selectionLimit = maxItems.toLong(); filter = PHPickerFilter.imagesFilter
                }
                val picker = PHPickerViewController(config)
                val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                        picker.dismissViewControllerAnimated(true, null)
                        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                        val out = mutableListOf<PickedMedia>()
                        var remaining = results.size
                        if (remaining == 0) return
                        results.forEach { r ->
                            // The temp file is deleted when this callback returns -> copy it NOW.
                            r.itemProvider.loadFileRepresentationForTypeIdentifier("public.image") { url, _ ->
                                if (url != null) {
                                    val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
                                    val dest = "$docs/photos/${NSUUID().UUIDString}.jpg"
                                    NSFileManager.defaultManager.createDirectoryAtPath("$docs/photos", true, null, null)
                                    NSFileManager.defaultManager.copyItemAtURL(url, NSURL.fileURLWithPath(dest), null)
                                    out += PickedMedia(dest, "image/jpeg")
                                }
                                remaining--
                                if (remaining == 0) dispatch_main_queue_run { latest.value(out.toList()) }
                            }
                        }
                    }
                }
                delegateHolder[0] = delegate
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, true, null)
            }
        }
    }
}

private fun dispatch_main_queue_run(block: () -> Unit) {
    platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) { block() }
}

@OptIn(ExperimentalForeignApi::class)
class IosFileStore : FileStore {
    override fun writeCache(name: String, bytes: ByteArray): String {
        val dir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
        val path = "$dir/$name"
        bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()).writeToFile(path, true) }
        return path
    }
}

@OptIn(ExperimentalForeignApi::class)
class IosShareService(private val config: ShareConfig) : ShareService {

    override suspend fun share(request: ShareRequest): Outcome<ShareOutcome, DomainError> {
        val image = request.imagePath?.let { UIImage.imageWithContentsOfFile(it) }
        if (request.target == ShareTarget.InstagramStories && image != null) {
            val url = NSURL.URLWithString("instagram-stories://share?source_application=${config.facebookAppId}")!!
            if (UIApplication.sharedApplication.canOpenURL(url)) { // needs LSApplicationQueriesSchemes = instagram-stories
                val png = UIImagePNGRepresentation(image)!!
                UIPasteboard.generalPasteboard.setItems(
                    listOf(mapOf("com.instagram.sharedSticker.backgroundImage" to png)),
                    mapOf(UIPasteboardOptionExpirationDate to NSDate.dateWithTimeIntervalSinceNow(300.0)),
                )
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
                return Outcome.Ok(ShareOutcome.Shared)
            }
        }
        val vc = topViewController() ?: return Outcome.Err(DomainError.Platform("No screen to share from."))
        val items = listOfNotNull(request.text, image)
        return suspendCancellableCoroutine { cont ->
            val sheet = UIActivityViewController(items, null)
            // iPad crashes without an anchor for the popover. Guide: Module 9.3 pitfall.
            sheet.popoverPresentationController?.sourceView = vc.view
            sheet.completionWithItemsHandler = { _, completed, _, _ ->
                val o = when {
                    !completed -> ShareOutcome.Cancelled
                    request.target == ShareTarget.InstagramStories -> ShareOutcome.UsedFallbackSheet
                    else -> ShareOutcome.Shared
                }
                if (cont.isActive) cont.resume(Outcome.Ok(o))
            }
            vc.presentViewController(sheet, true, null)
        }
    }
}
