# 簡單文件掃描

「簡單文件掃描」是一款以 Kotlin 與 Jetpack Compose 開發的 Android 文件掃描 App。它使用 ML Kit 文件掃描器擷取及裁切文件，支援調整頁面順序、儲存 JPEG 圖片，以及依目前頁序建立多頁 PDF。

> GitHub Releases 目前提供的是 **Prerelease 測試版本**，僅供測試與功能體驗，並非正式發行版本。

## 主要功能

- 使用 ML Kit 掃描文件或從相簿匯入圖片。
- 單份文件最多可包含 50 頁。
- 顯示掃描縮圖與頁碼。
- 使用左右按鈕調整頁面順序。
- 支援刪除頁面及繼續加入掃描內容。
- 使用 Room 保存目前文件、圖片路徑與頁面順序。
- 將 JPEG 匯出至 `Download/簡單文件掃描`。
- 將多頁 PDF 匯出至 `Download/簡單文件掃描`。
- 開啟或分享最近匯出的檔案。
- 提供六種預設主題色彩、完整色相自訂色彩及跟隨系統／淺色／深色顯示模式。
- 系統狀態列及手機底部導覽列會配合深色或淺色介面。
- 支援 Android Adaptive Icon 及 Android 13 單色主題圖示。

## 操作方式

1. 點選「開始掃描」。
2. 使用相機拍攝文件，或從相簿匯入既有圖片。
3. 完成掃描後，使用頁面下方的按鈕調整順序或刪除頁面。
4. 選擇「儲存圖片」或「建立 PDF」。
5. 匯出完成後，可直接開啟或透過 Android 分享介面傳送檔案。

使用「繼續掃描」時，掃描器會依目前頁數限制可加入的頁面數；單份文件達到 50 頁後無法繼續掃描。

畫面右上角的齒輪圖示可開啟完整設定頁，旁邊的退出圖示可將 App 移至背景。主題色彩與顯示模式會立即生效並保存；設定頁的「功能說明」可查看完整操作方式、儲存位置及裝置需求。

## 系統需求

- Android 10（API 29）以上。
- 裝置需具備 Google Play 服務。
- ML Kit 文件掃描器要求裝置至少具有約 1.7 GB RAM。
- 第一次啟動掃描功能時，Google Play 服務可能需要連線下載掃描元件。

## Prerelease 測試版

可從 [GitHub Releases](https://github.com/mark216tw/android-doc-scan-01/releases) 下載 Prerelease APK。

安裝前請注意：

- APK 啟用 R8 壓縮並使用 Android Debug 金鑰簽署，不適合正式發布或長期散布。
- 此版本僅供測試，可能存在尚未發現的問題。
- Android 可能顯示未知來源或 Play Protect 提醒。
- 未來正式版可能無法直接覆蓋安裝此 Prerelease 版本。
- 安裝前請自行評估風險，重要文件請保留其他備份。

## 從原始碼建置

需要以下開發環境：

- Java 17
- Android SDK Platform 35
- Android SDK Build Tools 35

Windows PowerShell：

```powershell
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease --no-daemon
```

macOS 或 Linux：

```bash
./gradlew testPrereleaseUnitTest lintPrerelease assemblePrerelease --no-daemon
```

建置完成後，Prerelease APK 位於：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

Prerelease 使用 `1.0.0-prerelease` 版本名稱、R8 壓縮及 Debug 金鑰簽署。設定頁顯示的 Build 編碼由建置日期、時間及 Version Code 組成。

## 技術架構

- Kotlin 2.0
- Android Gradle Plugin 8.7
- Jetpack Compose 與 Material 3
- ML Kit Document Scanner
- Room
- Kotlin Coroutines 與 Flow
- Coil
- Android `PdfDocument`
- MediaStore

掃描後的 JPEG 會先複製到 App 私有目錄，Room 負責保存頁面資料及排序。建立 PDF 時，App 會依目前頁面順序逐頁輸出，因此不直接使用 ML Kit 產生的 PDF。

## 隱私

App 不會主動將掃描文件上傳到開發者伺服器，也不要求廣泛儲存空間權限。草稿保存在 App 私有目錄，且已停用 Android 系統備份。

完整內容請參閱[隱私說明](docs/PRIVACY.md)。

## 專案文件

- [文件索引](docs/README.md)
- [版本變更紀錄](docs/CHANGELOG.md)
- [介面與功能優化建議](docs/IMPROVEMENT_RECOMMENDATIONS.md)
- [優化待辦事項](docs/IMPROVEMENT_TODO.md)

## 已知限制

- 尚未加入拖曳排序、旋轉、重新裁切、濾鏡與文件歷史列表。
- 尚未加入 OCR 或可搜尋 PDF。
- ML Kit 目前的公開 Android API 無法指定掃描器預設使用手動拍攝模式；使用者仍可在 Google 提供的掃描介面中切換可用模式。
- 套件名稱仍為開發用的 `com.example.simpledocumentscanner`，正式上架前必須更換。
- Prerelease 測試版仍需在更多實體裝置上驗證相機、匯出及分享流程。

## 授權

本專案採用 [MIT License](LICENSE) 授權。
