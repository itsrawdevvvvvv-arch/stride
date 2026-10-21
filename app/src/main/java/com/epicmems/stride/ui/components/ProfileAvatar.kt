package com.epicmems.stride.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.profileColor
import com.epicmems.stride.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes the saved avatar off the main thread. Null when there is no photo. */
@Composable
fun rememberProfileBitmap(path: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    return bitmap
}

/**
 * Circular avatar: the user's photo when they've set one, otherwise their initials on their
 * chosen accent colour.
 */
@Composable
fun ProfileAvatar(
    name: String,
    colorIndex: Int,
    photoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null
) {
    val bitmap = rememberProfileBitmap(photoPath)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(profileColor(colorIndex)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = Format.initials(name),
                color = OnPastel,
                style = textStyle ?: MaterialTheme.typography.titleMedium
            )
        }
    }
}
