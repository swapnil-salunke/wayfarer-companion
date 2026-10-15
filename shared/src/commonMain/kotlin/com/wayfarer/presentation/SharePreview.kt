package com.wayfarer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import com.wayfarer.domain.*
import com.wayfarer.platform.FileStore
import com.wayfarer.platform.toPngBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// GUIDE: Module 9 "Sharing": draw a card -> save as PNG -> hand it to ShareService.
// KNOWN SIMPLIFICATION: the guide captures at a fixed Density(3f) to get exactly 1080x1920. Here the card is
// captured at the device's density (works everywhere, size varies). Upgrading is an exercise in Module 9.4.

@Composable
fun SharePreviewScreen(entryId: JournalEntryId, onBack: () -> Unit) {
    val repo = koinInject<EntryRepository>()
    val shareEntry = koinInject<ShareEntryUseCase>()
    val files = koinInject<FileStore>()
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var entry by remember { mutableStateOf<JournalEntry?>(null) }
    var pagerState by remember { mutableStateOf<PagerState?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(entryId) { entry = repo.get(entryId) }

    Column(Modifier.fillMaxSize().background(WfColors.Ground).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back", color = WfColors.Teal, fontWeight = FontWeight.SemiBold) }
            Text("Share card", style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            entry?.let { e ->
                Box(Modifier.fillMaxHeight().aspectRatio(9f / 16f).clip(RoundedCornerShape(20.dp)).drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }) {
                    val pager = rememberPagerState(pageCount = { maxOf(1, e.photoPaths.size) })
                    pagerState = pager
                    ShareCard(e, pager)
                }
            }
        }
        if (entry?.photoPaths.orEmpty().size > 1) PageDots(pagerState?.currentPage ?: 0, entry?.photoPaths.orEmpty().size)
        Text(message ?: "Share opens the system sheet. Instagram posts to Stories.", color = WfColors.InkSoft, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { scope.launch { message = doShare(layer.toImageBitmap().toPngBytes(), files, shareEntry, entryId, ShareTarget.SystemSheet) } },
                shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text("Share", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = { scope.launch { message = doShare(layer.toImageBitmap().toPngBytes(), files, shareEntry, entryId, ShareTarget.InstagramStories) } },
                shape = MaterialTheme.shapes.large, border = BorderStroke(1.5.dp, WfColors.Ink), modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) { Text("Instagram", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WfColors.Ink) }
        }
    }
}

private suspend fun doShare(png: ByteArray, files: FileStore, uc: ShareEntryUseCase, id: JournalEntryId, target: ShareTarget): String {
    val path = files.writeCache("share-${id.value}.png", png)
    return when (val r = uc(id, path, target)) {
        is Outcome.Ok -> when (r.value) {
            ShareOutcome.Shared -> "Shared!"
            ShareOutcome.Cancelled -> "Cancelled"
            ShareOutcome.UsedFallbackSheet -> "Instagram not installed - used the share sheet"
        }
        is Outcome.Err -> r.error.userMessage()
    }
}

/** Only the photos page; the text overlay is drawn on top and stays fixed. */
@Composable
private fun ShareCard(e: JournalEntry, pager: PagerState) {
    Box(Modifier.fillMaxSize().background(WfColors.PhotoPlaceholder)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), overscrollEffect = null) { page ->
            e.photoPaths.getOrNull(page)?.let {
                AsyncImage("file://$it", null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0x99000000)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.title.ifBlank { "Untitled stop" }, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            e.place?.let { Text(it.name, color = Color.White, fontSize = 14.sp) }
            Text("WAYFARER", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(Modifier.size(if (i == current) 10.dp else 8.dp).clip(CircleShape).background(if (i == current) WfColors.Ink else WfColors.Outline))
        }
    }
}
