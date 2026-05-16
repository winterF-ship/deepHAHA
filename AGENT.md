农村# DeepHaha 论坛 — 开发日志

## 项目概览
- **项目路径**: D:\DSV4pro
- **后端**: Spring Boot 3.3.5 + Spring Data JPA + MySQL 8.0（端口 9000）
- **前端**: Vue 3 + Vite + Element Plus + Axios + Pinia（端口 5173，代理 → 9000）
- **数据库**: MySQL → `deephaha` 库
- **认证**: JWT + BCrypt 密码加密
- **外部 Maven**: `D:\spring boot\apache-maven-3.9.13`
- **IDEA 内置 Maven**: `D:\java\IntelliJ IDEA 2024.1.1\plugins\maven\lib\maven3`（不可用）

---

## 2026-05-14 — 项目初始化 & 功能搭建

### 后端（backend/）
- [x] Spring Boot 项目骨架 + pom.xml（Maven）
- [x] 4 个 JPA 实体：User、Category、Post、Reply
- [x] 8 个 DTO：Result（统一返回格式）、LoginRequest、RegisterRequest、PostRequest、ReplyRequest、UserDTO、PostDTO、ReplyDTO、PageResult
- [x] 4 个 JPA Repository
- [x] 3 个 Service：UserService（BCrypt 密码加密 + JWT 签发）、PostService（发帖/列表/详情+Entity→DTO）、ReplyService
- [x] 4 个 Controller：AuthController、CategoryController、PostController、ReplyController、UserController
- [x] JWT 工具类 + JwtInterceptor 拦截器
- [x] 全局异常处理 GlobalExceptionHandler
- [x] CORS 配置 + Spring Security 放行
- [x] data.sql 初始化 4 个板块：技术讨论、资源共享、闲聊灌水、公告反馈

### 前端（frontend/）
- [x] Vue 3 + Vite 项目搭建
- [x] Vue Router 路由配置 + 全局守卫（未登录跳登录页）
- [x] Pinia 状态管理（token + userInfo 持久化 localStorage）
- [x] Axios 封装：自动带 Bearer Token、401 拦截跳转登录
- [x] 6 个页面：HomeView、CategoryView、PostDetailView、PostCreateView、LoginView、RegisterView
- [x] NavBar 组件：渐变色用户头像 + hover 下拉菜单（个人信息/我的帖子/退出登录）
- [x] 首页 & 板块页醒目发帖按钮
- [x] Element Plus UI 组件集成

### Bug 修复记录
1. **Maven settings.xml 语法错误** — 第 167 行多余 `-->` 导致 XML 解析失败 → 已删除
2. **中文乱码** — Spring 默认用 GBK 读取 data.sql，中文损坏 → 加 `spring.sql.init.encoding=utf-8`
3. **Hibernate 懒加载序列化异常** — Category 实体 `posts` 字段懒加载，Jackson 序列化报错 → 加 `@JsonIgnore`
4. **前端 XSS 风险** — PostDetailView 用 `v-html` 渲染帖子内容 → 改为 `{{ }}` 纯文本
5. **导航栏布局** — 用户区域未靠右 → 调整 flex 布局，`margin-left: auto` 推到最右
6. **IDEA NoClassDefFoundError: SpringApplication** — IDEA 使用内置 Maven 导致依赖解析失败 → 切换为外部 Maven `D:\spring boot\apache-maven-3.9.13`（详见下方 IDEA 配置）
7. **注册/登录 403 Forbidden** — CORS 写死 `localhost:5173`，Vite 端口被占用自动跳到 5176 导致 Origin 不匹配 → 改为 `allowedOriginPatterns("http://localhost:*")`（SecurityConfig + WebMvcConfig 两处都要改）
8. **Axios 拦截器双重报错** — 业务错误（HTTP 200 但 code≠200）在 success 拦截器 `Promise.reject` 后又触发 error 拦截器，弹出两条错误 → 加 `err._handled = true` 标记，error 拦截器检测到直接跳过

### API 路由表
| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | /api/auth/register | 注册 | ❌ |
| POST | /api/auth/login | 登录 | ❌ |
| GET | /api/categories | 板块列表 | ❌ |
| GET | /api/posts | 帖子列表（分页） | ❌ |
| GET | /api/posts/{id} | 帖子详情 | ❌ |
| POST | /api/posts | 发帖 | ✅ |
| POST | /api/posts/{postId}/replies | 回复 | ✅ |
| GET | /api/user/me | 当前用户 | ✅ |

### 启动方式
```bash
# 后端（端口 9000）
cd D:\DSV4pro\backend && mvn spring-boot:run

# 前端（端口 5173）
cd D:\DSV4pro\frontend && npm install && npm run dev
```

浏览器访问 `http://localhost:5173`

---

## IDEA Maven 配置（重要）

IDEA 内置 Maven 会导致 `NoClassDefFoundError: SpringApplication`，必须切换为外部 Maven。

### 配置步骤
1. IDEA 中右键 `backend/pom.xml` → **添加为 Maven 项目**
2. `文件` → `设置` → `构建、执行、部署` → `构建工具` → `Maven`
3. **Maven 主路径**: `D:\spring boot\apache-maven-3.9.13`（不要用"已捆绑 Maven"）
4. **用户设置文件**: `D:\spring boot\apache-maven-3.9.13\conf\settings.xml`
5. **Maven Runner** → JRE: JDK 17（如 `D:\jdk`）
6. Maven 面板 → **Reload All Maven Projects**

### 验证方法
运行时启动命令中应出现：
```
-Dmaven.home=D:\spring boot\apache-maven-3.9.13
```
如果仍出现内置 Maven 路径，说明配置未生效。

### 排查优先级
端口、MySQL、JWT、Spring Security、pom.xml、application.properties 均已验证正确。**不要再优先修改这些**。先检查 IDEA Maven 配置。
```bash
# 命令行验证（总是可靠的）
cd /d D:\DSV4pro\backend
mvn clean compile
mvn spring-boot:run
```

---

## CORS 配置（重要）

CORS **必须**用 `allowedOriginPatterns("http://localhost:*")`，不能用写死的端口号。

### 原因
Vite 默认在端口被占用时自动跳到下一个端口（如 5173→5176）。如果 CORS 写死 5173，跳端口后 Origin 不匹配，Spring Security 返回 403。前端 `vite.config.js` 可加 `strictPort: true` 阻止自动跳端口。

### 涉及文件
- `SecurityConfig.java` — `CorsConfigurationSource` Bean
- `WebMvcConfig.java` — `addCorsMappings` 方法

两处都要用 `allowedOriginPatterns`，不能用 `allowedOrigins`。

---

## 端口占用排查记录（重要）

### 固定端口
- 后端端口：`9000`
- 前端端口：`5173`
- 前端 `vite.config.js` 应保持：
  - `server.port = 5173`
  - `server.strictPort = true`
  - `/api` 代理到 `http://localhost:9000`
  - `/uploads` 代理到 `http://localhost:9000`

### 常见现象
- IDEA 控制台显示 `Web server failed to start. Port 9000 was already in use.`
- IDEA 当前运行窗口显示“进程已结束，退出代码为 1”时，代表本次启动失败了，不代表占用端口的旧进程在这个窗口里。
- 真正占用端口的通常是旧的 `java.exe` 后端进程，可能残留在后台。
- Vite 如果没有 `strictPort: true`，`5173` 被占用时会自动跳到 `5174/5175/5176`，导致请求地址和 CORS/代理判断混乱。

### Windows 查占用命令
```powershell
netstat -ano | Select-String ':9000'
netstat -ano | Select-String ':5173|:5174|:5175|:5176'
```

看 `LISTENING` 行最后一列 PID，例如：
```text
TCP    0.0.0.0:9000    0.0.0.0:0    LISTENING    33564
```
这里占用 `9000` 的进程就是 PID `33564`。

### 结束占用进程
优先用任务管理器：
1. 打开任务管理器
2. 进入“详细信息”
3. 如果没有 PID 列，右键表头勾选 `PID`
4. 找到对应 PID 的 `java.exe` 或 `node.exe`
5. 右键“结束任务”

命令行方式：
```powershell
Stop-Process -Id 33564 -Force
taskkill /PID 33564 /F
```

如果命令行返回 `Access is denied`，说明当前权限无法结束该进程。改用任务管理器结束，或用管理员 PowerShell 执行 `taskkill`。

### 判断是否已经释放
再次执行：
```powershell
netstat -ano | Select-String ':9000'
```

如果没有 `LISTENING` 行，说明端口已经释放。`TIME_WAIT` 通常不是问题，真正阻塞启动的是 `LISTENING`。

### 启动成功标志
后端成功启动时应看到：
```text
Tomcat started on port 9000
Started ForumApplication
```

如果仍然看到：
```text
Port 9000 was already in use
```
说明还有旧进程占用 `9000`，继续按 PID 排查。

---

## 头像显示/上传排查记录（重要）

### 头像链路
头像功能不是只改上传接口，必须同时保证四段链路一致：
1. 后端上传文件保存到 `backend/uploads/avatars/`
2. 后端返回头像 URL，例如 `/uploads/avatars/avatar_xxx.png`
3. 前端 Vite 代理 `/uploads` 到 `http://localhost:9000`
4. 前端所有显示用户头像的位置都使用同一个头像字段

### Vite 代理要求
`frontend/vite.config.js` 必须同时代理：
```js
proxy: {
  '/api': {
    target: 'http://localhost:9000',
    changeOrigin: true
  },
  '/uploads': {
    target: 'http://localhost:9000',
    changeOrigin: true
  }
}
```

如果只代理 `/api`，上传接口会成功，但浏览器访问 `/uploads/avatars/...` 时会请求前端 dev server，导致图片破图。

### 用户状态同步
头像上传成功后，前端不能只改当前页面预览，必须同步 Pinia/localStorage：
```js
const res = await uploadAvatar(file)
avatarPreview.value = res.data.avatar
store.updateUser(res.data)
```

否则个人中心可能变了，但导航栏右上角仍然显示旧头像。

### DTO 字段要求
帖子列表、帖子详情、回复列表要显示作者头像，后端 DTO 必须返回作者头像字段：
- `PostDTO.authorAvatar`
- `ReplyDTO.authorAvatar`

后端转换 DTO 时必须设置：
```java
dto.setAuthorAvatar(post.getAuthor().getAvatar());
rd.setAuthorAvatar(reply.getAuthor().getAvatar());
```

否则详情页或列表页只能拿到 `authorName`，前端会回退显示用户名首字。

### 前端显示要求
所有作者头像 UI 都应优先显示图片，没有头像才显示首字：
```vue
<span class="author-avatar">
  <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
  <span v-else>{{ post.authorName?.charAt(0) }}</span>
</span>
```

已处理过的位置：
- `NavBar.vue`：当前登录用户头像，使用 `store.userInfo.avatar`
- `ProfileView.vue`：个人中心头像，上传后同步 `store.updateUser`
- `PostDetailView.vue`：帖子作者头像、回复作者头像
- `HomeView.vue`：首页最新帖子作者头像
- `CategoryView.vue`：分类页帖子作者头像

### 常见现象和原因
- 个人中心破图：多半是 `/uploads` 没代理，或后端静态资源映射目录不一致。
- 右上角没变：上传后没有 `store.updateUser(res.data)`，localStorage 里还是旧用户信息。
- 详情页变了但首页没变：详情页用了 `authorAvatar`，首页/分类页还在写死 `authorName.charAt(0)`。
- 上传成功但旧帖子作者头像没变：后端 `PostDTO` 没带 `authorAvatar`，或后端没重启仍在跑旧代码。
