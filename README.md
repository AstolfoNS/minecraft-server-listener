# Minecraft Listener Service

> 一个用于 **远程安全控制 Minecraft 服务进程** 的 Spring Boot 服务
> 支持 **HTTP API（HMAC 鉴权）** + **WebSocket 实时日志推送**

---

## 项目简介

`minecraft-listener` 是一个运行在服务器上的 Spring Boot 服务，核心能力包括：

- 启动 / 停止 Minecraft Server
- 向 Minecraft 控制台发送命令
- 实时推送 Minecraft 日志（WebSocket）
- 使用 **HMAC-SHA256** 对 HTTP 接口进行安全校验
- 支持部署在 Linux 服务器上，通过脚本管理生命周期

该服务通常作为 **“服务端控制器”**，由其他客户端服务（如 QQ Bot、管理后台等）远程调用。

---

## 整体架构

```graph
┌──────────────────────┐
│  Client SpringBoot   │  ← OkHttp + HMAC
│  (QQ Bot / Panel)    │
└──────────┬───────────┘
           │ HTTP API
           ▼
┌──────────────────────┐
│ minecraft-listener   │
│ Spring Boot :8081    │
│  - HMAC 校验         │
│  - Process 管理      │
│  - WebSocket 日志    │
└──────────┬───────────┘
           │ stdin/stdout
           ▼
┌──────────────────────┐
│   Minecraft Server   │
│   (java -jar ...)    │
└──────────────────────┘
```

---

## 功能列表

### HTTP API（需 HMAC 鉴权）

| 接口                   | 方法 | 说明           |
| ---------------------- | ---- | -------------- |
| `/api/minecraft/start` | POST | 启动 MC 服务器 |
| `/api/minecraft/stop`  | POST | 停止 MC 服务器 |
| `/api/minecraft/cmd`   | POST | 发送控制台命令 |

### WebSocket（无需鉴权）

| 地址                    | 说明             |
| ----------------------- | ---------------- |
| `/api/minecraft/ws/log` | 实时推送 MC 日志 |

---

## 安全机制（HMAC-SHA256）

所有 HTTP 请求必须携带以下请求头：

| Header    | 说明                 |
| --------- | -------------------- |
| `X-TS`    | 秒级时间戳           |
| `X-NONCE` | 随机字符串（防重放） |
| `X-SIGN`  | HMAC-SHA256 签名     |

### Canonical String 格式

```text
METHOD \n PATH \n TIMESTAMP \n NONCE
```

### 特性

- 时间窗口校验（默认 ±60 秒）
- nonce 防重放（内存缓存）
- 常量时间字符串比较（防时序攻击）
- WebSocket 明确 **不做鉴权**

---

## 项目结构

### Java 包结构

```dir
com.timeleafing.minecraft
├── controller        # HTTP API
├── service           # MC 进程控制
├── security          # HMAC Filter
├── websocket         # 日志 WS 推送
├── config            # WebSocket / Spring 配置
└── MinecraftApplication.java
```

---

## 服务器部署结构

```dir
/www/
├── minecraft/
│   └── /xxx    # minecraft 服务端
└── listener/
    ├── logs/
    │   └── listener_2026-01-28_10-22-17.log
    ├── script/
    │   ├── start.sh
    │   └── stop.sh
    ├── secret/
    │   └── hmac_secret
    ├── server/
    │   ├── config/
    │   │   └── application.yaml
    │   └── minecraft-0.0.1-SNAPSHOT.jar
    └── listener.pid
```

---

## application.yaml 示例

```yaml
spring:
  application:
    name: minecraft
server:
  port: 8081
  servlet:
    context-path: /api/minecraft
security:
  hmac-secret: ${HMAC_SECRET}
  max-skew-seconds: 60
  header-ts: X-TS
  header-nonce: X-NONCE
  header-sign: X-SIGN
minecraft:
  work-dir: /www/minecraft/VanillaEra:CulinaryJourney2.5.1–Server
  run-script: ./run.sh
```

---

## HMAC 密钥管理

### 密钥文件

```dir
/www/listener/secret/hmac_secret
```

内容示例（一行）：

```txt
a-very-long-random-secret-string
```

### 权限建议

```bash
chmod 600 /www/listener/secret/hmac_secret
```

---

## 启动服务

```bash
cd /www/listener/script
./start.sh
```

启动后会：

- 自动读取 `hmac_secret`
- 注入为环境变量 `HMAC_SECRET`
- 写入 PID 文件
- 日志输出到 `/logs`

---

## 停止服务

```bash
./stop.sh
```

支持优雅停止（SIGTERM）。

---

## 使用 Postman 测试

### 计算签名

```head
METHOD = POST
PATH   = /api/minecraft/start
TS     = 当前秒级时间戳
NONCE  = 随机字符串
```

Canonical String：

```api
POST
/api/minecraft/start
1700000000
abcdef123456
```

签名算法：HmacSHA256 → Base64

---

### Postman Headers 示例

| Key     | Value        |
| ------- | ------------ |
| X-TS    | 1700000000   |
| X-NONCE | abcdef123456 |
| X-SIGN  | xxxBase64xxx |

---

## WebSocket 连接示例

```bash
ws://<server-ip>:8081/api/minecraft/ws/log
```

说明：

- 不需要任何 Header
- 用于实时日志监听
- 可被客户端服务 / 运维工具 / 浏览器使用
