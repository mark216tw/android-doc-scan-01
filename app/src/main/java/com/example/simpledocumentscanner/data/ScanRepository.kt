package com.example.simpledocumentscanner.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

const val MAX_DOCUMENT_PAGES = 50

class ScanRepository(
    private val context: Context,
    private val database: ScanDatabase,
) {
    private val dao = database.scanDao()

    suspend fun latestOrNewDocument(): String = withContext(Dispatchers.IO) {
        dao.latestDocument()?.id ?: createDocumentInternal()
    }

    suspend fun createDocument(): String = withContext(Dispatchers.IO) {
        createDocumentInternal()
    }

    fun observeDocument(documentId: String): Flow<ScanDocumentEntity?> =
        dao.observeDocument(documentId)

    fun observePages(documentId: String): Flow<List<ScanPageEntity>> =
        dao.observePages(documentId)

    suspend fun importPages(documentId: String, sourceUris: List<Uri>) = withContext(Dispatchers.IO) {
        if (sourceUris.isEmpty()) return@withContext

        val currentPageCount = dao.pages(documentId).size
        require(currentPageCount + sourceUris.size <= MAX_DOCUMENT_PAGES) {
            "單份文件最多只能包含 $MAX_DOCUMENT_PAGES 頁"
        }

        val documentDirectory = File(context.filesDir, "scans/$documentId").apply { mkdirs() }
        var nextPosition = dao.maximumPosition(documentId) + 1
        val pages = sourceUris.map { uri ->
            val pageId = UUID.randomUUID().toString()
            val destination = File(documentDirectory, "$pageId.jpg")
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "無法讀取掃描圖片" }
                    destination.outputStream().use(input::copyTo)
                }
            } catch (error: Exception) {
                destination.delete()
                throw error
            }

            ScanPageEntity(
                id = pageId,
                documentId = documentId,
                imagePath = destination.absolutePath,
                position = nextPosition++,
            )
        }

        database.withTransaction {
            dao.insertPages(pages)
            dao.touchDocument(documentId, System.currentTimeMillis())
        }
    }

    suspend fun movePage(documentId: String, pageId: String, offset: Int) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val pages = dao.pages(documentId)
                val fromIndex = pages.indexOfFirst { it.id == pageId }
                val reordered = pages.moved(fromIndex, fromIndex + offset)
                if (reordered === pages) return@withTransaction

                reordered.forEachIndexed { index, page ->
                    dao.updatePosition(page.id, index)
                }
                dao.touchDocument(documentId, System.currentTimeMillis())
            }
        }

    suspend fun deletePage(documentId: String, page: ScanPageEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.deletePage(page.id)
            dao.pages(documentId).forEachIndexed { index, remainingPage ->
                dao.updatePosition(remainingPage.id, index)
            }
            dao.touchDocument(documentId, System.currentTimeMillis())
        }
        File(page.imagePath).delete()
    }

    private suspend fun createDocumentInternal(): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val title = "掃描_${LocalDateTime.now().format(TITLE_FORMAT)}"
        dao.insertDocument(
            ScanDocumentEntity(
                id = id,
                title = title,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    private companion object {
        val TITLE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
    }
}
