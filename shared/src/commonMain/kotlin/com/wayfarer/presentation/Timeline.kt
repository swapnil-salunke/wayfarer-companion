package com.wayfarer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.wayfarer.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// GUIDE: Module 5 "Presentation: state, events, effects" (UDF = state flows down, events flow up).

data class TimelineState(val loading: Boolean = true, val entries: List<JournalEntry> = emptyList())

class TimelineViewModel(
    observeTimeline: ObserveTimelineUseCase,
    private val deleteEntry: DeleteEntryUseCase,
) : ViewModel() {
    val state: StateFlow<TimelineState> = observeTimeline(DefaultTripId)
        .map { TimelineState(loading = false, entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineState())

    fun onDelete(id: JournalEntryId) { viewModelScope.launch { deleteEntry(id) } }
}

@Composable
fun TimelineScreen(state: TimelineState, onAdd: () -> Unit, onOpenShare: (JournalEntryId) -> Unit, onEdit: (JournalEntryId) -> Unit, onDelete: (JournalEntryId) -> Unit) {
    Scaffold(
        containerColor = WfColors.Ground,
        topBar = {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)) {
                Text("Wayfarer", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp))
                if (state.entries.isNotEmpty())
                    Text(if (state.entries.size == 1) "1 entry" else "${state.entries.size} entries", color = WfColors.InkSoft, fontSize = 14.sp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = WfColors.Amber, contentColor = WfColors.Ink, shape = RoundedCornerShape(20.dp)) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = WfColors.Teal)
                state.entries.isEmpty() -> EmptyTimeline(onAdd, Modifier.align(Alignment.Center))
                else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(state.entries, key = { it.id.value }) { e -> EntryCard(e, { onOpenShare(e.id) }, { onEdit(e.id) }, { onDelete(e.id) }) }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimeline(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("No entries yet", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
        Text("Tap + to log your first stop. Everything stays on your phone.", color = WfColors.InkSoft, fontSize = 16.sp)
        Button(onClick = onAdd, shape = MaterialTheme.shapes.medium, modifier = Modifier.heightIn(min = 48.dp)) { Text("Log first stop") }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun stamp(millis: Long): String {
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${d.day} $month - ${d.hour.toString().padStart(2, '0')}:${d.minute.toString().padStart(2, '0')}"
}

@Composable
private fun EntryCard(e: JournalEntry, onShare: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onShare),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = WfColors.Surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stamp(e.createdAtMillis).uppercase(), color = WfColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            if (e.title.isNotBlank()) Text(e.title, style = MaterialTheme.typography.titleMedium)
            e.place?.let { Text(it.name, color = WfColors.InkSoft, fontSize = 13.sp) }
            if (e.body.isNotBlank()) Text(e.body, maxLines = 3, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp))
            if (e.photoPaths.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    e.photoPaths.take(2).forEach {
                        AsyncImage(model = "file://$it", contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).height(84.dp).clip(MaterialTheme.shapes.small).background(WfColors.PhotoPlaceholder))
                    }
                    if (e.photoPaths.size > 2) Box(Modifier.width(56.dp).height(84.dp).clip(MaterialTheme.shapes.small).background(Color(0xFFE1E8E4)), contentAlignment = Alignment.Center) {
                        Text("+${e.photoPaths.size - 2}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row {
                TextButton(onClick = onShare) { Text("Share", color = WfColors.Teal, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onEdit) { Text("Edit", color = WfColors.Teal, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onDelete) { Text("Delete", color = WfColors.Danger, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
