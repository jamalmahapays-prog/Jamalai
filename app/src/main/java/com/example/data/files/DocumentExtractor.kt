package com.example.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class ExtractedDocument(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val extractedText: String,
    val pdfBytes: ByteArray?,
    val bitmap: Bitmap?
)

object DocumentExtractor {
    private const val MAX_FILE_BYTES = 20L * 1024L * 1024L // 20 MB

    suspend fun extractFromUri(context: Context, uri: Uri): Result<ExtractedDocument> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                var fileName = "document"
                var fileSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                if (fileSize > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("File exceeds 20 MB limit.")
                }

                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unable to read selected file.")

                if (bytes.size.toLong() > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("File exceeds 20 MB limit.")
                }
                val actualSize = if (fileSize > 0) fileSize else bytes.size.toLong()

                when {
                    mimeType.startsWith("image/") -> {
                        val bmp = decodeScaledBitmap(bytes)
                            ?: throw IllegalStateException("Could not decode image.")
                        ExtractedDocument(
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = actualSize,
                            extractedText = "",
                            pdfBytes = null,
                            bitmap = bmp
                        )
                    }
                    mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                        ExtractedDocument(
                            fileName = fileName,
                            mimeType = "application/pdf",
                            sizeBytes = actualSize,
                            extractedText = "",
                            pdfBytes = bytes,
                            bitmap = null
                        )
                    }
                    fileName.endsWith(".docx", ignoreCase = true) ||
                        mimeType.contains("wordprocessingml", ignoreCase = true) -> {
                        val text = extractDocxText(bytes)
                        ExtractedDocument(
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = actualSize,
                            extractedText = text,
                            pdfBytes = null,
                            bitmap = null
                        )
                    }
                    else -> {
                        val text = bytes.toString(Charsets.UTF_8).take(15000)
                        ExtractedDocument(
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = actualSize,
                            extractedText = text,
                            pdfBytes = null,
                            bitmap = null
                        )
                    }
                }
            }
        }

    suspend fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    decodeScaledBitmap(stream.readBytes())
                }
            }.getOrNull()
        }

    private fun decodeScaledBitmap(bytes: ByteArray, maxDim: Int = 1280): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun extractDocxText(bytes: ByteArray): String {
        val sb = StringBuilder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "p" && sb.isNotEmpty()) {
                                    sb.append('\n')
                                }
                            }
                            XmlPullParser.TEXT -> {
                                sb.append(parser.text)
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        return sb.toString().trim().take(15000)
    }
}
