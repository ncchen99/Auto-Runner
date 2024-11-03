# AutoRunner

AutoRunner 是一個 Android 應用程式，旨在模擬 GPS 位置更新。這個應用程式允許用戶在地圖上添加地標，並以指定的速度模擬位置移動。

## 功能

- **地圖顯示**：使用 Google Maps 顯示當前位置和地標。
- **地標管理**：用戶可以在地圖上添加和刪除地標。
- **模擬位置更新**：根據用戶設置的速度模擬位置移動。
- **前景服務**：使用前景服務來持續模擬位置。
- **音效提示**：在到達地標時播放提示音。

## 安裝

1. 克隆此專案到本地：

   ```bash
   git clone https://github.com/ncchen99/autorunner.git
   ```

2. 使用 Android Studio 打開專案。

3. 確保您已經安裝了 Android SDK 和 Google Play Services。

4. 在模擬器或實體裝置上運行應用程式。

## 使用

1. 啟動應用程式後，允許應用程式訪問您的位置。

2. 在地圖上點擊以添加地標。

3. 點擊 "Start" 按鈕以開始模擬位置更新。

4. 點擊 "Stop" 按鈕以停止模擬並退出應用程式。

5. 您可以在地圖上長按地標以刪除它。

## 開發

### 小幫手
- **ChatGPT 4o**： 用於前期開發
- **Cursor 4o**： 用於後期開發

### 主要技術

- **Kotlin**：應用程式的主要開發語言。
- **Jetpack Compose**：用於構建 UI。
- **Google Maps SDK**：用於地圖顯示和位置管理。
- **FusedLocationProviderClient**：用於模擬位置更新。


### 目錄結構

- `MainActivity.kt`：應用程式的主要活動，負責地圖顯示和用戶交互。
- `MockLocationService.kt`：服務類，用於模擬位置更新。
- `res/`：資源文件夾，包含應用程式的圖示、佈局和字符串資源。

## 貢獻

歡迎貢獻！如果您有任何建議或改進，請提交 Pull Request 或創建 Issue。

## 授權

此專案使用 [MIT License](https://github.com/ncchen99/Auto-Runner/new/master) 授權。
