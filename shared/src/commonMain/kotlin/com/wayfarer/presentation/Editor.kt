package com.wayfarer.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayfarer.domain.*
import com.wayfarer.platform.PickedMedia
import com.wayfarer.platform.rememberPhotoPickerLauncher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// GUIDE: Module 6 "The editor screen": events in, state out, one-off effects on a Channel.

data class EditorState(
    val title: String = "", val body: String = "", val place: String = "",
    val photos: List<String> = emptyList(), val saving: Boolean = false, val error: String? = null,
)
sealed interface EditorEvent {
    data class TitleChanged(val v: String) : EditorEvent
    data class BodyChanged(val v: String) : EditorEvent
    data class PlaceChanged(val v: String) : EditorEvent
    data class PhotosPicked(val items: List<PickedMedia>) : EditorEvent
    data class PhotoRemoved(val index: Int) : EditorEvent
    data object SaveClicked : EditorEvent
}
sealed interface EditorEffect { data object Saved : EditorEffect }

class EditorViewModel(
    private val createEntry: CreateEntryUseCase,
    private val updateEntry: UpdateEntryUseCase,
    private val repository: EntryRepository,
) : ViewModel() {
    private var editingId: JournalEntryId? = null

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Called whenever the editor route opens: blank for a new entry, prefilled when editing. */
    fun start(id: JournalEntryId?) {
        editingId = id
        _state.value = EditorState()
        if (id != null) viewModelScope.launch {
            repository.get(id)?.let { e ->
                _state.value = EditorState(title = e.title, body = e.body, place = e.place?.name.orEmpty(), photos = e.photoPaths)
            }
        }
    }

    fun onEvent(e: EditorEvent) = when (e) {
        is EditorEvent.TitleChanged -> _state.update { it.copy(title = e.v, error = null) }
        is EditorEvent.BodyChanged -> _state.update { it.copy(body = e.v, error = null) }
        is EditorEvent.PlaceChanged -> _state.update { it.copy(place = e.v) }
        is EditorEvent.PhotosPicked -> _state.update { it.copy(photos = it.photos + e.items.map { m -> m.path }) }
        is EditorEvent.PhotoRemoved -> _state.update { it.copy(photos = it.photos.filterIndexed { i, _ -> i != e.index }) }
        EditorEvent.SaveClicked -> save()
    }

    private fun save() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(saving = true) }
        val id = editingId
        val result = if (id == null) createEntry(DefaultTripId, s.title, s.body, s.place, s.photos)
        else updateEntry(id, s.title, s.body, s.place, s.photos)
        when (val r = result) {
            is Outcome.Ok -> _effects.send(EditorEffect.Saved)
            is Outcome.Err -> _state.update { it.copy(saving = false, error = r.error.userMessage()) }
        }
    }
}

fun DomainError.userMessage(): String = when (this) {
    is DomainError.Validation -> message
    DomainError.NotFound -> "That entry no longer exists."
    is DomainError.Storage -> "Could not save: $message"
    is DomainError.Platform -> message
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(state: EditorState, editing: Boolean, onEvent: (EditorEvent) -> Unit, onBack: () -> Unit) {
    // The picker is a composable (needs the Activity / view controller). Guide: Module 8.3.
    val picker = rememberPhotoPickerLauncher(maxItems = 5) { onEvent(EditorEvent.PhotosPicked(it)) }
    val focus = LocalFocusManager.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = WfColors.Teal, unfocusedBorderColor = WfColors.Outline,
        focusedContainerColor = WfColors.Surface, unfocusedContainerColor = WfColors.Surface,
    )
    Scaffold(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { focus.clearFocus() } },
        containerColor = WfColors.Ground,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit entry" else "New entry", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back", color = WfColors.Teal, fontWeight = FontWeight.SemiBold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WfColors.Ground),
            )
        },
        bottomBar = {
            Button(
                onClick = { onEvent(EditorEvent.SaveClicked) }, enabled = !state.saving, shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(20.dp).heightIn(min = 54.dp),
            ) { Text("Save", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        },
    ) { p ->
        Column(Modifier.padding(p).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(state.title, { onEvent(EditorEvent.TitleChanged(it)) }, label = { Text("Title") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }), singleLine = true, colors = fieldColors, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.place, { onEvent(EditorEvent.PlaceChanged(it)) }, label = { Text("Place") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }), singleLine = true, colors = fieldColors, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.body, { onEvent(EditorEvent.BodyChanged(it)) }, label = { Text("What happened?") }, minLines = 5, colors = fieldColors, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            if (state.photos.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.photos.forEachIndexed { i, path -> PhotoThumb(path) { onEvent(EditorEvent.PhotoRemoved(i)) } }
                }
            }
            OutlinedButton(
                onClick = picker::launch, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.5.dp, WfColors.Teal),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(if (state.photos.isEmpty()) "Add photos" else "${state.photos.size} photo(s) - add more", color = WfColors.Teal, fontWeight = FontWeight.SemiBold) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** 80dp thumbnail with a circled-cross badge (44dp touch target) at the top right. */
@Composable
private fun PhotoThumb(path: String, onRemove: () -> Unit) {
    Box(Modifier.size(88.dp)) {
        AsyncImage(
            model = "file://$path", contentDescription = "Attached photo", contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.BottomStart).size(80.dp).clip(MaterialTheme.shapes.small).background(WfColors.PhotoPlaceholder),
        )
        Box(
            Modifier.align(Alignment.TopEnd).size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Remove photo", onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(24.dp)) {
                drawCircle(Color.White)
                drawCircle(WfColors.Ink, radius = size.minDimension / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                val c = Offset(size.width / 2, size.height / 2); val d = 4.5.dp.toPx()
                drawLine(WfColors.Ink, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), 2.dp.toPx(), StrokeCap.Round)
                drawLine(WfColors.Ink, Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
