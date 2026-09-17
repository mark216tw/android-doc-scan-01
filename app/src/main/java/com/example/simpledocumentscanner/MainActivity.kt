package com.example.simpledocumentscanner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.simpledocumentscanner.data.MAX_DOCUMENT_PAGES
import com.example.simpledocumentscanner.data.ScanPageEntity
import com.example.simpledocumentscanner.ui.AppSettings
import com.example.simpledocumentscanner.ui.AppSettingsStore
import com.example.simpledocumentscanner.ui.DisplayMode
import com.example.simpledocumentscanner.ui.ScannerThemeColor
import com.example.simpledocumentscanner.ui.SimpleScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: ScanViewModel by viewModels()
    private val settingsStore by lazy { AppSettingsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var settings by remember { mutableStateOf(settingsStore.load()) }
            SimpleScannerTheme(
                themeColor = settings.themeColor,
                customThemeColor = settings.customThemeColor,
                displayMode = settings.displayMode,
            ) {
                ScannerRoute(
                    viewModel = viewModel,
                    settings = settings,
                    onSettingsChange = { updatedSettings ->
                        settings = updatedSettings
                        settingsStore.save(updatedSettings)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScannerRoute(
    viewModel: ScanViewModel,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val activity = requireNotNull(LocalActivity.current)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val remainingPageCount = (MAX_DOCUMENT_PAGES - state.pages.size).coerceAtLeast(0)
    val options = remember(remainingPageCount) {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(remainingPageCount.coerceAtLeast(1))
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember(options) { GmsDocumentScanning.getClient(options) }
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            viewModel.importScannedPages(scan?.pages?.map { it.imageUri }.orEmpty())
        }
    }
    val startScan: () -> Unit = {
        if (remainingPageCount == 0) {
            viewModel.reportError(
                IllegalStateException("單份文件最多只能包含 $MAX_DOCUMENT_PAGES 頁"),
            )
        } else {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { sender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
                .addOnFailureListener(viewModel::reportError)
        }
    }

    ScannerScreen(
        state = state,
        onScan = startScan,
        onNewDocument = viewModel::newDocument,
        onMovePage = viewModel::movePage,
        onDeletePage = viewModel::deletePage,
        onExportJpegs = viewModel::exportJpegs,
        onExportPdf = viewModel::exportPdf,
        onMessageShown = viewModel::clearMessage,
        onError = viewModel::reportError,
        settings = settings,
        onSettingsChange = onSettingsChange,
        onMoveToBackground = { activity.moveTaskToBack(true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerScreen(
    state: ScanUiState,
    onScan: () -> Unit,
    onNewDocument: () -> Unit,
    onMovePage: (String, Int) -> Unit,
    onDeletePage: (ScanPageEntity) -> Unit,
    onExportJpegs: () -> Unit,
    onExportPdf: () -> Unit,
    onMessageShown: () -> Unit,
    onError: (Throwable) -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onMoveToBackground: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showFeatureHelp by rememberSaveable { mutableStateOf(false) }
    var showNewDocumentConfirmation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    BackHandler(enabled = showSettings) {
        if (showFeatureHelp) {
            showFeatureHelp = false
        } else {
            showSettings = false
        }
    }

    if (showFeatureHelp) {
        FeatureHelpScreen(onBack = { showFeatureHelp = false })
        return
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onFeatureHelp = { showFeatureHelp = true },
            onBack = { showSettings = false },
        )
        return
    }

    if (showNewDocumentConfirmation) {
        AlertDialog(
            onDismissRequest = { showNewDocumentConfirmation = false },
            title = { Text("建立新文件？") },
            text = { Text("建立後會切換到新的空白文件，確定要繼續嗎？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewDocumentConfirmation = false
                        onNewDocument()
                    },
                ) {
                    Text("建立")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocumentConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("簡單文件掃描", fontWeight = FontWeight.Bold)
                        state.document?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showNewDocumentConfirmation = true },
                        enabled = state.pages.isNotEmpty() && !state.isWorking,
                    ) {
                        Text("新文件")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "開啟 App 設定",
                        )
                    }
                    IconButton(onClick = onMoveToBackground) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = "將 App 移至背景",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ScanHero(
                pageCount = state.pages.size,
                isWorking = state.isWorking,
                onScan = onScan,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                text = "掃描結果",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (state.pages.isEmpty()) {
                    "掃描後可在這裡檢查並調整頁面順序"
                } else {
                    "使用左右按鈕排序，PDF 將依此順序建立"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            if (state.pages.isEmpty()) {
                EmptyPages(modifier = Modifier.weight(1f))
            } else {
                PageStrip(
                    pages = state.pages,
                    onMovePage = onMovePage,
                    onDeletePage = onDeletePage,
                    modifier = Modifier.weight(1f),
                )
            }

            state.lastExportUri?.let { uri ->
                ExportActions(
                    onOpen = {
                        runCatching { openUri(context, uri) }.onFailure(onError)
                    },
                    onShare = {
                        runCatching { shareUri(context, uri) }.onFailure(onError)
                    },
                )
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onExportJpegs,
                    enabled = state.pages.isNotEmpty() && !state.isWorking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("儲存圖片")
                }
                Button(
                    onClick = onExportPdf,
                    enabled = state.pages.isNotEmpty() && !state.isWorking,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("建立 PDF")
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onFeatureHelp: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回上一頁",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "顯示模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DisplayMode.entries.forEachIndexed { index, mode ->
                    val label = when (mode) {
                        DisplayMode.SYSTEM -> "跟隨系統"
                        DisplayMode.LIGHT -> "淺色"
                        DisplayMode.DARK -> "深色"
                    }
                    SegmentedButton(
                        selected = settings.displayMode == mode,
                        onClick = { onSettingsChange(settings.copy(displayMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DisplayMode.entries.size,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = label, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "主題色彩",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ScannerThemeColor.entries.chunked(2).forEach { themeRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    themeRow.forEach { themeColor ->
                        SettingsChoice(
                            label = themeColor.displayName,
                            selected = settings.customThemeColor == null && settings.themeColor == themeColor,
                            swatch = themeColor.swatch,
                            onClick = {
                                onSettingsChange(
                                    settings.copy(
                                        themeColor = themeColor,
                                        customThemeColor = null,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            CustomColorPicker(
                settings = settings,
                onSettingsChange = onSettingsChange,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onFeatureHelp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("功能說明")
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "關於",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Build ${BuildConfig.BUILD_TIMESTAMP}.${BuildConfig.VERSION_CODE}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureHelpScreen(onBack: () -> Unit) {
    val sections = listOf(
        "開始掃描" to
            "在主畫面點選「開始掃描」，可使用相機拍攝文件或從相簿匯入圖片。完成 Google 提供的掃描流程後，頁面會加入目前文件。單份文件最多可包含 50 頁。",
        "繼續加入頁面" to
            "文件已有頁面時，主畫面按鈕會顯示「繼續掃描」。新頁面會加入現有頁面之後，再依需要調整順序；掃描器只會允許加入剩餘頁數，達到 50 頁後無法繼續掃描。",
        "檢查與整理頁面" to
            "掃描結果會顯示縮圖及頁碼。使用「向左」或「向右」調整頁序，JPEG 檔名頁碼及 PDF 頁序都會依目前順序產生。刪除頁面後目前無法復原，操作前請先確認。",
        "建立新文件" to
            "目前文件已有頁面時，可點選頂端的「新文件」。確認後會切換至新的空白文件；建立前建議先匯出需要保留的內容。",
        "儲存 JPEG 圖片" to
            "點選「儲存圖片」會將每一頁分別輸出為 JPEG，並儲存至 Download/簡單文件掃描。檔名包含文件名稱、匯出時間及頁碼。",
        "建立多頁 PDF" to
            "點選「建立 PDF」會依目前頁序建立一份多頁 PDF，並儲存至 Download/簡單文件掃描。",
        "開啟與分享" to
            "匯出完成後可使用「開啟」或「分享」。PDF 會操作最近建立的 PDF；多張 JPEG 匯出目前會操作最後一張圖片，所有圖片仍會保存在下載目錄。",
        "外觀設定" to
            "設定頁可選擇跟隨系統、淺色或深色模式，並可使用預設色彩或色相滑桿調整 App 主題色。設定會立即套用並保存。",
        "檔案與隱私" to
            "掃描草稿保存在 App 私有目錄，其他 App 無法直接存取，解除安裝時會一併移除。匯出的 JPEG 與 PDF 位於共享下載目錄，不會因解除安裝而自動刪除。",
        "裝置需求" to
            "掃描功能需要 Android 10 以上、Google Play 服務及約 1.7 GB 以上記憶體。第一次使用時，Google Play 服務可能需要連線下載掃描元件。",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能說明", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回設定",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sections.forEach { (title, description) ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CustomColorPicker(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val initialColor = settings.customThemeColor?.let { Color(it) } ?: settings.themeColor.swatch
    var hue by rememberSaveable(settings.customThemeColor, settings.themeColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(hsv[0])
    }
    val previewColor = Color.hsv(hue, 0.85f, 0.9f)
    val rainbow = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(18.dp),
            shape = RoundedCornerShape(6.dp),
            color = previewColor,
        ) {}
        Box(
            modifier = Modifier.weight(1f).height(58.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 10.dp)) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(rainbow),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }
            Slider(
                value = hue,
                onValueChange = { hue = it },
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(customThemeColor = previewColor.toArgb()))
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = previewColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SettingsChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatch: Color? = null,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            swatch?.let {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(it),
                )
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "已選取",
                )
            }
        }
    }
}

@Composable
private fun ScanHero(pageCount: Int, isWorking: Boolean, onScan: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pageCount == 0) {
                        "把紙本變成清晰文件"
                    } else {
                        "目前已有 $pageCount / $MAX_DOCUMENT_PAGES 頁"
                    },
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        pageCount >= MAX_DOCUMENT_PAGES -> "已達單份文件頁數上限"
                        pageCount == 0 -> "單份最多 $MAX_DOCUMENT_PAGES 頁，只在這支手機上處理"
                        else -> "還可加入 ${MAX_DOCUMENT_PAGES - pageCount} 頁"
                    },
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(16.dp))
            FilledTonalButton(
                onClick = onScan,
                enabled = !isWorking && pageCount < MAX_DOCUMENT_PAGES,
            ) {
                Text(
                    when {
                        pageCount >= MAX_DOCUMENT_PAGES -> "已達上限"
                        pageCount == 0 -> "開始掃描"
                        else -> "繼續掃描"
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyPages(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "尚無頁面",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PageStrip(
    pages: List<ScanPageEntity>,
    onMovePage: (String, Int) -> Unit,
    onDeletePage: (ScanPageEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.width(220.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box {
                        AsyncImage(
                            model = File(page.imagePath),
                            contentDescription = "第 ${index + 1} 頁掃描",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onMovePage(page.id, -1) },
                            enabled = index > 0,
                        ) { Text("向左") }
                        TextButton(onClick = { onDeletePage(page) }) { Text("刪除") }
                        TextButton(
                            onClick = { onMovePage(page.id, 1) },
                            enabled = index < pages.lastIndex,
                        ) { Text("向右") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportActions(onOpen: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("檔案已完成", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onOpen) { Text("開啟") }
        TextButton(onClick = onShare) { Text("分享") }
    }
}

private fun openUri(context: android.content.Context, uri: Uri) {
    val type = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("找不到可開啟此檔案的應用程式", error)
    }
}

private fun shareUri(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享掃描文件"))
}
