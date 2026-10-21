package com.epicmems.stride.ui.components

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Surface1
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.ui.theme.profileColor
import com.epicmems.stride.util.Format

/** Edit the name and accent colour shown in the app header. */
@Composable
fun ProfileDialog(
    name: String,
    colorIndex: Int,
    onCommit: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localName by remember { mutableStateOf(name) }
    var localColor by remember { mutableIntStateOf(colorIndex) }

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
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(profileColor(localColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Format.initials(localName),
                        color = OnPastel,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.size(16.dp))
                Text(
                    text = "This is how Stride greets you.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
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
                    onCommit(localName.trim().ifBlank { "Stride" }, localColor)
                    onDismiss()
                }
            )
        }
    }
}
