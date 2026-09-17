package com.example.simpledocumentscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insertDocument(document: ScanDocumentEntity)

    @Insert
    suspend fun insertPages(pages: List<ScanPageEntity>)

    @Query("SELECT * FROM scan_documents ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestDocument(): ScanDocumentEntity?

    @Query("SELECT * FROM scan_documents WHERE id = :documentId")
    fun observeDocument(documentId: String): Flow<ScanDocumentEntity?>

    @Query("SELECT * FROM scan_pages WHERE documentId = :documentId ORDER BY position")
    fun observePages(documentId: String): Flow<List<ScanPageEntity>>

    @Query("SELECT * FROM scan_pages WHERE documentId = :documentId ORDER BY position")
    suspend fun pages(documentId: String): List<ScanPageEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM scan_pages WHERE documentId = :documentId")
    suspend fun maximumPosition(documentId: String): Int

    @Query("UPDATE scan_pages SET position = :position WHERE id = :pageId")
    suspend fun updatePosition(pageId: String, position: Int)

    @Query("UPDATE scan_documents SET updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun touchDocument(documentId: String, updatedAt: Long)

    @Query("DELETE FROM scan_pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String)
}
