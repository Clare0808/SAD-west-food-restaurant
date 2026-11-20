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
git clone https://github.com/Clare0808/SAD-west-food-restaurant.git
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

### Render 免費部署（單一 Docker Web Service）

1. 建立外部 MongoDB 免費資料庫（例如 MongoDB Atlas M0），設定資料庫使用者與網路存取規則，取得可由 Render 連線的完整 `mongodb+srv://...` URI。密碼中的特殊字元須做 URL 編碼；`MONGO_URL` **不可**填 `mongo` 或 `localhost`。不要把密碼或連線字串提交到 Git。
2. 在 Render 選 **New → Web Service**（不是 Static Site），連接 `Clare0808/SAD-west-food-restaurant` 的 `main` 分支。Language 選 **Docker**、Instance Type 選 **Free**、Root Directory 留空；Dockerfile Path 設為 `./dockerfile`（檔名小寫），如有 Build Context Directory 欄位則設為 `.`。不要新增 Persistent Disk。Health Check Path 可設為 `/data/dishData.json`。
3. 在服務的 Environment 中加入 `MONGO_URL`（Atlas URI）和 `JWT_SECRET`（自行產生的長隨機字串）。`PORT` 由 Render 提供，`MENU_DATA_DIR`、`MENU_SEED_DIR` 已在 Dockerfile 設定；**不要**直接複製上方本機環境變數範例。確認計費畫面顯示 **Free** 後建立服務。
4. 也可以使用 **New → Blueprint**：目前 `render.yaml` 已明確設為 `plan: free` 且不包含付費磁碟；建立時填入 `MONGO_URL`，`JWT_SECRET` 會自動產生。若 Render 預覽仍顯示付費，請先核對連接的是此 repo 的最新 `main`，或改用上方的手動 Web Service 流程；不要確認任何付費項目。
5. 部署完成後開啟 `https://<服務名稱>.onrender.com`、`/menu` 和 `/data/dishData.json` 檢查；若啟動失敗請看 Render 的 Logs。Vue 網頁、API、圖片都由同一個網址提供。

如需 Google 登入，在 Render 加入 `GOOGLE_CLIENT_ID`、`GOOGLE_CLIENT_SECRET`、`GOOGLE_CALLBACK_URL=https://<服務名稱>.onrender.com/api/auth-callback`，並在 Google Cloud Console 設定相同的 redirect URI。單一網域不需 `FRONTEND_URL`；不用 Google 登入則可以不設定 OAuth 變數。

**免費方案限制：**Render Free 會在閒置後休眠、重啟與部署時重建容器，且無法掛載持久化磁碟。MongoDB 中的使用者、訂單等資料保存在外部資料庫；菜單 JSON 和上傳圖片仍寫在 `/app/storage`，**每次容器重啟或重新部署會遺失這些線上修改**，並回到映像內 `/app/seed` 的初始菜單／圖片。若需要永久保存線上新增菜單及圖片，須先改用外部持久化儲存；本地 Docker Compose 的 volumes 不受此限制。**公開部署前請先補上後端管理 API 的認證／授權**（目前允許未登入者存取管理 API），並更改 `Startup.java` 的預設管理員密碼 `admin123`；目前設定不適合公開正式環境。
