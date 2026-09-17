package com.example.simpledocumentscanner

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpledocumentscanner.data.ScanDatabase
import com.example.simpledocumentscanner.data.ScanDocumentEntity
import com.example.simpledocumentscanner.data.ScanPageEntity
import com.example.simpledocumentscanner.data.ScanRepository
import com.example.simpledocumentscanner.export.DocumentExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ScanUiState(
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val document: ScanDocumentEntity? = null,
    val pages: List<ScanPageEntity> = emptyList(),
    val message: String? = null,
    val lastExportUri: Uri? = null,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ScanDatabase.getInstance(application)
    private val repository = ScanRepository(application, database)
    private val exporter = DocumentExporter(application)
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var activeDocumentId: String? = null
    private var documentJob: Job? = null

    init {
        viewModelScope.launch {
            observeDocument(repository.latestOrNewDocument())
        }
    }

    fun newDocument() {
        if (_uiState.value.isWorking || _uiState.value.pages.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.createDocument() }
                .onSuccess(::observeDocument)
                .onFailure(::showError)
        }
    }

    fun importScannedPages(uris: List<Uri>) {
        val documentId = activeDocumentId ?: return
        if (uris.isEmpty()) return
        launchWorking {
            repository.importPages(documentId, uris)
            "已加入 ${uris.size} 頁掃描"
        }
    }

    fun movePage(pageId: String, offset: Int) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            runCatching { repository.movePage(documentId, pageId, offset) }
                .onFailure(::showError)
        }
    }

    fun deletePage(page: ScanPageEntity) {
        val documentId = activeDocumentId ?: return
        viewModelScope.launch {
            runCatching { repository.deletePage(documentId, page) }
                .onFailure(::showError)
        }
    }

    fun exportJpegs() {
        val state = _uiState.value
        val document = state.document ?: return
        if (state.pages.isEmpty()) return
        launchWorking {
            exporter.exportJpegs(document, state.pages).also { result ->
                _uiState.value = _uiState.value.copy(lastExportUri = result.uri)
            }.description
        }
    }

    fun exportPdf() {
        val state = _uiState.value
        val document = state.document ?: return
        if (state.pages.isEmpty()) return
        launchWorking {
            exporter.exportPdf(document, state.pages).also { result ->
                _uiState.value = _uiState.value.copy(lastExportUri = result.uri)
            }.description
        }
    }

    fun reportError(error: Throwable) = showError(error)

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun observeDocument(documentId: String) {
        activeDocumentId = documentId
        documentJob?.cancel()
        documentJob = viewModelScope.launch {
            combine(
                repository.observeDocument(documentId),
                repository.observePages(documentId),
            ) { document, pages -> document to pages }
                .collect { (document, pages) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        document = document,
                        pages = pages,
                        lastExportUri = null,
                    )
                }
        }
    }

    private fun launchWorking(block: suspend () -> String) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { block() }
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(isWorking = false, message = message)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    showError(error)
                }
        }
    }

    private fun showError(error: Throwable) {
        _uiState.value = _uiState.value.copy(
            message = error.localizedMessage?.takeIf(String::isNotBlank) ?: "操作失敗，請再試一次",
        )
    }
}
