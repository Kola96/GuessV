# 部署方案设计

> 本文档定义 GuessV 的构建与部署方案。
> 核心原则：**开发分离，部署合一，单 Jar 极简交付**。
> 返回 [AGENTS.md](../../AGENTS.md)

---

## 一、方案总览

```
┌──────────────── 开发环境（分离）────────────────┐
│                                                 │
│  Vite Dev Server :5173 ──proxy /api──► Spring Boot :8080 │
│  （前端热更新）                     （后端热重启） │
│                                                 │
└─────────────────────────────────────────────────┘

┌──────────────── 生产环境（合一）────────────────┐
│                                                 │
│  java -jar guessv.jar  （唯一进程，监听 8080）   │
│  ├── /api/**        → Spring Controller         │
│  ├── /admin/**      → 静态资源（运营后台）       │
│  └── /**            → 静态资源（前端 SPA）       │
│                                                 │
│  SQLite：jar 同目录 data/guessv.db（零安装）     │
└─────────────────────────────────────────────────┘
```

| 维度 | 方案 |
|------|------|
| 交付物 | 单个 `guessv.jar` |
| 运行命令 | `java -jar guessv.jar` |
| 外部依赖 | JDK 21（唯一要求） |
| 数据库 | SQLite（初期零安装）→ MySQL（后期） |
| 进程管理 | systemd（开机自启 + 崩溃重启） |
| Nginx | **不需要**（可选 Caddy 处理 HTTPS） |
| Docker | **不需要**（可选） |

---

## 二、工作原理

### 2.1 构建时：前端产物打进 Jar

```
frontend/npm run build
    ↓ 产出
frontend/dist/
    ├── index.html
    └── assets/（带 hash 的 js/css）
    ↓ 拷贝
backend/src/main/resources/static/
    ↓ mvn package
guessv.jar（内含前端静态资源 + 后端代码）
```

### 2.2 运行时：请求分发规则

| 请求路径 | 处理方 | 说明 |
|----------|--------|------|
| `/api/**` | Spring Controller | REST API |
| `/assets/**` | 静态资源（长缓存） | 带内容 hash，永久缓存 |
| `/admin`、`/game` 等前端路由 | fallback 到 `index.html` | SPA 客户端路由 |
| `/` | `index.html` | 入口页面 |

---

## 三、开发环境配置

### 3.1 Vite Proxy（frontend/vite.config.ts）

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 3.2 开发启动命令

```bash
# 终端 1：后端（热重启）
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2：前端（热更新）
cd frontend && npm run dev
```

前端访问 `http://localhost:5173`，API 请求自动代理到后端。

---

## 四、构建流程

### 4.1 一键构建脚本

**scripts/build.sh**（Linux/macOS）：

```bash
#!/bin/bash
set -e

# 1. 构建前端
cd frontend
npm ci
npm run build

# 2. 拷贝前端产物到后端静态目录
rm -rf ../backend/src/main/resources/static/*
cp -r dist/* ../backend/src/main/resources/static/

# 3. 打包后端（产出单个 jar）
cd ../backend
./mvnw clean package -DskipTests

echo "构建完成：backend/target/guessv.jar"
```

**scripts/build.ps1**（Windows）：

```powershell
# 1. 构建前端
Set-Location frontend
npm ci
npm run build

# 2. 拷贝前端产物
Remove-Item -Recurse -Force ../backend/src/main/resources/static/* -ErrorAction SilentlyContinue
Copy-Item -Recurse dist/* ../backend/src/main/resources/static/

# 3. 打包后端
Set-Location ../backend
./mvnw clean package "-DskipTests"

Write-Host "构建完成：backend/target/guessv.jar"
```

### 4.2 注意事项

| 事项 | 说明 |
|------|------|
| `static/` 加入 `.gitignore` | 构建产物不提交 Git |
| Vite `base` 配置 | 使用默认 `/` 即可，无需子路径 |
| 资源 hash | Vite 默认产出带 hash 的文件名，天然支持长缓存 |

---

## 五、Spring Boot 配置

### 5.1 SPA 路由 Fallback

前端路由（React Router 的 `/game`、`/admin` 等）在刷新页面时，服务端必须返回 `index.html`：

```java
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) {
                        Resource requested = location.createRelative(path);
                        // 存在的静态资源直接返回；不存在的非 API 路径 fallback 到 index.html
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (path.startsWith("api/")) {
                            return null;  // API 路径走 Controller，不 fallback
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
```

### 5.2 缓存与压缩（application.yml）

```yaml
server:
  port: 8080
  compression:
    enabled: true                      # gzip 压缩
    mime-types: text/html,text/css,application/javascript,application/json

spring:
  web:
    resources:
      cache:
        cachecontrol:
          max-age: 365d                # 带 hash 的 assets 长缓存
  mvc:
    cache:
      cachecontrol:
        max-age: 0                     # index.html 不缓存（通过单独配置）
```

> 实现细节：`/assets/**` 长缓存，`index.html` 不缓存，确保发版后用户立即拿到新入口。

---

## 六、VPS 部署

### 6.1 服务器要求

| 项目 | 最低配置 |
|------|----------|
| CPU/内存 | 1 核 1G 足够 |
| 磁盘 | 10G |
| 系统 | 任意 Linux |
| 依赖 | **仅 JDK 21** |

### 6.2 目录结构

```
/opt/guessv/
├── guessv.jar          # 应用（唯一交付物）
├── data/
│   └── guessv.db       # SQLite 数据库（自动创建）
├── logs/
│   └── guessv.log      # 应用日志
└── application-prod.yml # 生产配置（可选，也可打包进 jar）
```

### 6.3 systemd 服务（/etc/systemd/system/guessv.service）

```ini
[Unit]
Description=GuessV Application
After=network.target

[Service]
Type=simple
User=guessv
WorkingDirectory=/opt/guessv
ExecStart=java -jar guessv.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
# 启用服务
sudo systemctl enable guessv
sudo systemctl start guessv
```

### 6.4 部署/升级流程

```bash
# 本地构建
./scripts/build.sh

# 上传（仅一个文件）
scp backend/target/guessv.jar user@vps:/opt/guessv/

# 重启
ssh user@vps "sudo systemctl restart guessv"
```

**整个部署 = 传一个 jar + 重启，完。**

---

## 七、数据库演进

### 7.1 初期：SQLite（零安装）

- 数据库文件 `data/guessv.db` 随应用自动创建
- 无需任何外部服务
- 备份 = 复制一个文件

```bash
# 备份
cp /opt/guessv/data/guessv.db /opt/guessv/data/guessv.db.bak.$(date +%F)
```

### 7.2 后期：切换 MySQL

访问量起来后：

```bash
# VPS 上安装 MySQL（或用 docker run 单容器）
sudo apt install mysql-server

# 修改配置后重启
sudo systemctl restart guessv
```

- 应用本身**不需要任何改动**，仅切换 `application-prod.yml` 数据源配置
- 详见 [数据库设计](./../database/001-schema-overview.md)

---

## 八、HTTPS（有域名时可选）

如需绑定域名 + HTTPS，推荐 **Caddy**（比 Nginx 更简）：

```
# /etc/caddy/Caddyfile（全部配置就这两行）
guessv.example.com
reverse_proxy localhost:8080
```

Caddy 自动申请并续期 Let's Encrypt 证书，无需任何额外操作。

> 没有域名、纯 IP 访问的场景：直接 `http://<IP>:8080` 即可，连 Caddy 都不用。

---

## 九、方案对比

| 方案 | 进程数 | 外部依赖 | 部署命令 | 结论 |
|------|--------|----------|----------|------|
| **单 Jar（本方案）** | 1 | JDK | scp + restart | ✅ 采用 |
| Nginx + Jar | 2 | JDK + Nginx | 多处配置 | 过度设计 |
| Docker Compose | 2+ | Docker | compose up | 可选，非必需 |
| 前后端分离部署 | 2+ | JDK + Node/Nginx | 两套发布 | 不符合需求 |

### 为什么不用 Docker？

| 考量 | 说明 |
|------|------|
| 单 Jar 已是自包含 | Docker 镜像一个 jar 没有额外收益 |
| VPS 资源有限 | 少一层更省内存 |
| 排障简单 | 直接看进程和日志文件 |

> 如果未来团队习惯 Docker 或需要多实例，可平滑补一个单服务 Dockerfile，不影响本方案。

---

*文档版本：v1.0 | 更新日期：2026-08-18*
