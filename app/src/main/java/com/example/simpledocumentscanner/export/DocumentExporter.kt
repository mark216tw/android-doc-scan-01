package com.example.simpledocumentscanner.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.simpledocumentscanner.data.ScanDocumentEntity
import com.example.simpledocumentscanner.data.ScanPageEntity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportResult(
    val description: String,
    val uri: Uri,
)

class DocumentExporter(private val context: Context) {
    suspend fun exportJpegs(
        document: ScanDocumentEntity,
        pages: List<ScanPageEntity>,
    ): ExportResult = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "沒有可匯出的頁面" }

        val title = safeTitle(document.title)
        val suffix = LocalDateTime.now().format(FILE_TIME_FORMAT)
        var lastUri: Uri? = null

        pages.forEachIndexed { index, page ->
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "${title}_${suffix}_${index + 1}.jpg")
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/簡單文件掃描",
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = checkNotNull(
                context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values,
                ),
            ) { "無法建立圖片檔案" }

            try {
                context.contentResolver.openOutputStream(uri).use { output ->
                    requireNotNull(output) { "無法寫入圖片檔案" }
                    File(page.imagePath).inputStream().use { input -> input.copyTo(output) }
                }
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null,
                )
                lastUri = uri
            } catch (error: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }

        ExportResult(
            description = "已儲存 ${pages.size} 張圖片至 Download/簡單文件掃描",
            uri = checkNotNull(lastUri),
        )
    }

    suspend fun exportPdf(
        document: ScanDocumentEntity,
        pages: List<ScanPageEntity>,
    ): ExportResult = withContext(Dispatchers.IO) {
        require(pages.isNotEmpty()) { "沒有可匯出的頁面" }

        val title = safeTitle(document.title)
        val suffix = LocalDateTime.now().format(FILE_TIME_FORMAT)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "${title}_$suffix.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/簡單文件掃描",
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        ) { "無法建立 PDF 檔案" }

        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val bitmap = decodeForPdf(page.imagePath)
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, index + 1).create()
                    val pdfPage = pdf.startPage(pageInfo)
                    pdfPage.canvas.drawColor(Color.WHITE)
                    val target = fitInside(
                        sourceWidth = bitmap.width,
                        sourceHeight = bitmap.height,
                        area = RectF(
                            PDF_MARGIN,
                            PDF_MARGIN,
                            PDF_WIDTH - PDF_MARGIN,
                            PDF_HEIGHT - PDF_MARGIN,
                        ),
                    )
                    pdfPage.canvas.drawBitmap(bitmap, null, target, IMAGE_PAINT)
                    pdf.finishPage(pdfPage)
                } finally {
                    bitmap.recycle()
                }
            }

            context.contentResolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "無法寫入 PDF 檔案" }
                pdf.writeTo(output)
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        } finally {
            pdf.close()
        }

        ExportResult(
            description = "已儲存 ${pages.size} 頁 PDF 至 Download/簡單文件掃描",
            uri = uri,
        )
    }

    private fun decodeForPdf(path: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "無法讀取掃描圖片" }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_IMAGE_EDGE) {
            sampleSize *= 2
        }
        return requireNotNull(
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ),
        ) { "無法解碼掃描圖片" }
    }

    private fun fitInside(sourceWidth: Int, sourceHeight: Int, area: RectF): RectF {
        val scale = minOf(area.width() / sourceWidth, area.height() / sourceHeight)
        val width = sourceWidth * scale
        val height = sourceHeight * scale
        val left = area.left + (area.width() - width) / 2f
        val top = area.top + (area.height() - height) / 2f
        return RectF(left, top, left + width, top + height)
    }

    private fun safeTitle(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "掃描文件" }

    private companion object {
        const val PDF_WIDTH = 595
        const val PDF_HEIGHT = 842
        const val PDF_MARGIN = 24f
        const val MAX_IMAGE_EDGE = 2400
        val IMAGE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val FILE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
