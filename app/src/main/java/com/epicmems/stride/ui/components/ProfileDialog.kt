package com.epicmems.stride.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Surface1
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.ui.theme.profileColor
import com.epicmems.stride.util.ProfileImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Edit the profile: photo, name and accent colour. */
@Composable
fun ProfileDialog(
    name: String,
    colorIndex: Int,
    photoPath: String?,
    onCommit: (name: String, colorIndex: Int, photoPath: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var localName by remember { mutableStateOf(name) }
    var localColor by remember { mutableIntStateOf(colorIndex) }
    var localPhoto by remember { mutableStateOf(photoPath) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            saving = true
            scope.launch {
                val saved = withContext(Dispatchers.IO) { ProfileImageStore.save(context, uri) }
                if (saved != null) localPhoto = saved
                saving = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Surface1)
                .padding(22.dp)
        ) {
            Text("Your profile", color = TextPrimary, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = localName,
                    colorIndex = localColor,
                    photoPath = localPhoto,
                    size = 76.dp,
                    textStyle = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (localPhoto == null) "No photo yet" else "Looking good",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (saving) "Saving photo…" else "Pick a photo, or just use your initials.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GhostButton(
                text = if (localPhoto == null) "Choose a photo" else "Change photo",
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            if (localPhoto != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Remove photo",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .clickable { localPhoto = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = localName,
                onValueChange = { if (it.length <= 24) localName = it },
                label = { Text("Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = Surface3,
                    focusedLabelColor = Lime,
                    unfocusedLabelColor = TextMuted,
                    cursorColor = Lime
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Text("Accent", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (0..3).forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(profileColor(index))
                            .clickable { localColor = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == localColor) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = OnPastel,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            PrimaryButton(
                text = "Save",
                onClick = {
                    onCommit(localName.trim().ifBlank { "Stride" }, localColor, localPhoto)
                    onDismiss()
                }
            )
        }
    }
}
