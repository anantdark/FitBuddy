package com.anant.fitbuddy.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.model.QueuedPhotoLog
import com.anant.fitbuddy.ui.viewmodel.MAX_PHOTO_LOG_IMAGES
import com.anant.fitbuddy.ui.viewmodel.PhotoDraftUiState
import java.text.DateFormat
import java.util.Date

/** Compose a photo log: chain photos, add a note, then send now or queue for later. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PhotoLogSheet(
    draft: PhotoDraftUiState,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSend: () -> Unit,
    onQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(
                    if (draft.editingQueueId != null) R.string.photo_log_edit_title else R.string.photo_log_title
                ),
                style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.photo_log_count, draft.photos.size, MAX_PHOTO_LOG_IMAGES),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(draft.photos) { index, bytes ->
                    Box {
                        PhotoThumb(bytes, Modifier.size(96.dp))
                        Surface(
                            onClick = { onRemovePhoto(index) },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.photo_log_remove),
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
            val canAdd = draft.photos.size < MAX_PHOTO_LOG_IMAGES
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onAddCamera, enabled = canAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.photo_log_add_camera))
                }
                OutlinedButton(onClick = onAddGallery, enabled = canAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.photo_log_add_gallery))
                }
            }
            OutlinedTextField(
                value = draft.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.photo_log_note_label)) },
                placeholder = { Text(stringResource(R.string.photo_log_note_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onQueue,
                    enabled = draft.photos.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Queue, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(
                            if (draft.editingQueueId != null) R.string.photo_log_save_changes else R.string.photo_log_queue
                        )
                    )
                }
                Button(
                    onClick = onSend,
                    enabled = draft.photos.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.photo_log_send)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Queued photo logs the user can run (one at a time) or delete. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PhotoQueueSheet(
    items: List<QueuedPhotoLog>,
    thumbnails: suspend (String) -> ByteArray?,
    onRun: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.photo_queue_title), style = MaterialTheme.typography.titleLarge)
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.photo_queue_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    QueuedRow(item, thumbnails, onRun, onEdit, onDelete)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QueuedRow(
    item: QueuedPhotoLog,
    thumbnails: suspend (String) -> ByteArray?,
    onRun: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val thumb = androidx.compose.runtime.produceState<ByteArray?>(null, item.id) {
        value = thumbnails(item.id)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bytes = thumb.value
            if (bytes != null) {
                PhotoThumb(bytes, Modifier.size(56.dp))
            } else {
                Box(Modifier.size(56.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.note.ifBlank { stringResource(R.string.photo_queue_no_note) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
                Text(
                    pluralStringResource(R.plurals.photo_queue_photos, item.photoFiles.size, item.photoFiles.size) +
                        " · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(item.createdAtEpochMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onEdit(item.id) }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.photo_queue_edit))
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.photo_queue_delete))
            }
            Button(onClick = { onRun(item.id) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.photo_queue_run))
            }
        }
    }
}

@Composable
private fun PhotoThumb(bytes: ByteArray, modifier: Modifier) {
    val bitmap = remember(bytes) {
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(modifier.clip(RoundedCornerShape(12.dp)))
    }
}
