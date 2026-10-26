package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.R
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Photo
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    onSetAsCover: () -> Unit,
    onSetAsChapterCover: (() -> Unit)? = null,
    onShare: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    var showChapterCoverDialog by remember { mutableStateOf(false) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = androidStringResource(R.string.chapter_set_work_cover),
                    icon = MaterialSymbols.Rounded.Photo,
                    onClick = { showSetCoverDialog = true },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_copy_to_clipboard),
                    icon = MaterialSymbols.Rounded.ContentCopy,
                    onClick = {
                        onShare(true)
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_share),
                    icon = MaterialSymbols.Rounded.Share,
                    onClick = {
                        onShare(false)
                        onDismissRequest()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_save),
                    icon = MaterialSymbols.Rounded.Save,
                    onClick = {
                        onSave()
                        onDismissRequest()
                    },
                )
            }
            if (onSetAsChapterCover != null) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { showChapterCoverDialog = true },
                ) { Text(androidStringResource(R.string.chapter_set_cover)) }
            }
        }
    }

    if (showChapterCoverDialog && onSetAsChapterCover != null) {
        AlertDialog(
            onDismissRequest = { showChapterCoverDialog = false },
            text = { Text(androidStringResource(R.string.chapter_cover_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onSetAsChapterCover()
                    showChapterCoverDialog = false
                }) { Text(stringResource(MR.strings.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showChapterCoverDialog = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                onSetAsCover()
                showSetCoverDialog = false
            },
            onDismiss = { showSetCoverDialog = false },
        )
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
