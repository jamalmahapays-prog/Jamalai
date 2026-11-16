package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.AppStrings
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletPrimary
import java.io.File
import java.io.FileOutputStream

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(listOf(VioletPrimary.copy(alpha = 0.7f), CyanSecondary.copy(alpha = 0.7f)))
    } else {
        Brush.linearGradient(listOf(BorderSubtle, BorderSubtle))
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        tonalElevation = 2.dp,
        modifier = modifier.border(1.dp, borderBrush, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AiOutputCard(
    title: String,
    text: String,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    context: Context
) {
    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        accentBorder = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(VioletPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = CyanSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanSecondary
                )
            }
            Row {
                IconButton(onClick = { copyTextToClipboard(context, text, strings.copied) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = strings.copy,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { sharePlainText(context, text, title) }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    isHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isHealthy) EmeraldSuccess else RoseError
    Surface(
        shape = RoundedCornerShape(50),
        color = dotColor.copy(alpha = 0.14f),
        modifier = modifier.border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = dotColor
            )
        }
    }
}

fun copyTextToClipboard(context: Context, text: String, toastMessage: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("MyAI Pro", text))
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}

fun sharePlainText(context: Context, text: String, subject: String = "MyAI Pro") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

fun shareBitmapImage(context: Context, bitmap: Bitmap, title: String = "MyAI Pro Image") {
    runCatching {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, "myai_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, toastSuccess: String): Boolean {
    return runCatching {
        val filename = "MyAIPro_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyAIPro")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        Toast.makeText(context, toastSuccess, Toast.LENGTH_SHORT).show()
        true
    }.getOrDefault(false)
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
