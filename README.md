# 🍝 West Food Restaurant

一個提供使用者登入、商品瀏覽與管理功能的餐廳網站，  
具備登入功能、角色權限與後台管理概念。


## 首頁畫面

<img src="./src/assets/screenshot/homeScreenShot.png" width="600" />


## 功能介紹

- 使用者註冊 / 登入
- 密碼加密儲存（bcrypt）
- Navbar 依登入狀態切換
- 商品分類瀏覽
- 顧客評論
- 管理者後台
    - 使用者管理
    - 資料管理
    - 數據監測


## 使用技術

### Frontend
- Vue 3
- Pinia
- Vue Router
- Axios

### Backend
- Java 17
- Spring Boot 3 (Web, MongoDB, OAuth2 Client)
- MongoDB
- bcrypt

### Other
- Docker

## 專案說明

- 本專案使用 `concurrently` 同時啟動前端與 Java 後端
- 後端使用 `bcrypt` 對使用者密碼進行雜湊處理，不以明文方式儲存


## 專案啟動方式

### 1. Clone 專案
```bash
git clone https://github.com/Clare0808/west-food-restaurant.git
```

### 2. 安裝套件

```bash
npm install
cd backend && mvn test
```

### 3. 環境需求

- Node.js（僅前端）
- npm
- JDK 17
- Maven 3.9+
- MongoDB

### 4. 環境變數設定

本機執行 `npm run start` / `npm run backend` 會從 `backend/.env` 載入環境變數（已存在的系統環境變數優先）；直接執行 `mvn spring-boot:run` 或部署到 Render 時則須設定系統環境變數：

```env
PORT=3000
MONGO_URL=mongodb://127.0.0.1:27017/west_food
JWT_SECRET=yourSecretKey
GOOGLE_CLIENT_ID=yourGoogleClientId
GOOGLE_CLIENT_SECRET=yourGoogleClientSecret
GOOGLE_CALLBACK_URL=http://localhost:3000/api/auth-callback
FRONTEND_URL=http://localhost:8080
MENU_DATA_DIR=../public
MENU_SEED_DIR=../public
```

### 5. 啟動專案

```bash
npm run start
```

從專案根目錄執行此指令會使用本機的 MongoDB (`127.0.0.1:27017`)；請先啟動 MongoDB。`npm run start` 透過本機啟動器呼叫 Maven，需安裝 JDK 17 / Maven 3.9+（若本機先前已將工具下載至暫存目錄，也可直接使用該工具鏈）。

若要分開啟動 (選擇性) :

#### 前端

```bash
npm run serve
```

#### 後端
```bash
cd backend
mvn spring-boot:run
```

### 本地 Docker Compose（整合版）

在專案根目錄設定環境變數 `JWT_SECRET`（PowerShell：`$env:JWT_SECRET="請使用夠長的隨機密鑰"`），啟動 Docker Desktop，執行：

```bash
docker compose up --build
```

開啟 `http://localhost:8080`；`http://localhost:8080/data/dishData.json` 可檢查菜單 API。Compose 使用同一個前後端映像和本地 MongoDB，資料分別保存在 `menu-data`、`mongo-data` volumes；停止用 `docker compose down`（不要加 `-v`，否則會刪除資料）。前端修改後重新執行 `docker compose up --build`；若要即時更新，可沿用上面的 `npm run start` 開發模式（需自行啟動本機 MongoDB）。Docker 模式與開發模式都使用 8080，請勿同時佔用該埠。

### Render（單一 Docker Web Service）

1. 準備外部 MongoDB（例如 MongoDB Atlas），建立資料庫使用者並允許 Render 的來源 IP 連線。Render 上的 `MONGO_URL` 必須是可連線的完整 URI，**不可**使用 `mongo` 或 `localhost`。資料庫不包含在 Render Docker 映像中。
2. 將此專案（包含未提交的新檔案）推送至 Git 儲存庫。在 Render 選 **New → Blueprint**，連接此 repo；根目錄使用專案根目錄，`render.yaml` 會建立一個 Docker Web Service。或手動建立 Docker Web Service，Dockerfile 路徑設為 `./dockerfile`、context 設為 `.`、磁碟掛載到 `/app/storage`（至少 1 GB）。磁碟需要支援 persistent disk 的付費方案。
3. Blueprint 提示時填寫 `MONGO_URL`；`JWT_SECRET` 由 Blueprint 自動產生（手動建立時請自行設定）。待部署完成後，以 Render 提供的 `https://<服務名稱>.onrender.com` 開啟首頁、`/menu` 和 `/data/dishData.json` 驗證。Vue 網頁、API、圖片全部由同一個網址提供；Render 會自動提供 `PORT`。
4. 如需 Google 登入，於 Render 環境變數新增 `GOOGLE_CLIENT_ID`、`GOOGLE_CLIENT_SECRET`、`GOOGLE_CALLBACK_URL=https://<服務名稱>.onrender.com/api/auth-callback`；Google Cloud Console 的 redirect URI 設為相同網址。若不使用 Google 登入，可不設定，但登入按鈕不會正常完成 OAuth。單一網域不需要 `FRONTEND_URL`；本地 Compose 會自動設為 `http://localhost:8080`。

菜單 JSON 和後台上傳圖片寫入 `/app/storage`（Render 磁碟），初始內容從 `/app/seed` 讀取。磁碟不會保存到 Git，本地和 Render 的修改不會自動同步。無持久化磁碟重新部署會遺失修改；若不能使用付費磁碟，需先改為外部檔案儲存。**公開部署前請先補上後端管理 API 的認證／授權**（目前允許未登入者存取管理 API），並更改 `Startup.java` 的預設管理員密碼 `admin123`；目前設定不適合公開正式環境。
