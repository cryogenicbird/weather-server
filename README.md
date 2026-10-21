# weather-server 天气后端

[bubu 课表 App](https://github.com/cryogenicbird/tt-timetable) 的自建后端：代理和风天气 API + 用户账号系统，已部署腾讯云轻量（公网可访问）。

## 接口

| 接口 | 说明 | 鉴权 |
|---|---|---|
| `POST /register` | 注册（BCrypt 哈希入库，用户名唯一） | 无 |
| `POST /login` | 登录，返回一年期 JWT（Ed25519 签名） | 无 |
| `GET /weather/hourly/{lat}/{lon}` | 240 小时逐小时预报（响应裁剪，仅下发 App 所需字段） | Bearer token |

接口文档（Swagger UI）：`http://124.223.162.5:8080/swagger-ui/index.html`

## 技术栈

- Java 21 / Spring Boot 4.1.1 / MyBatis / MySQL
- 认证：Ed25519 JWT 签发与验签（私钥不进入代码仓库，CI 现场生成一次性测试密钥）
- 拦截器：鉴权（未登录 401）+ 限流（固定窗口 60 次/分，超限 429；限流在最外层，登录接口也受保护防爆破）
- 全局异常处理：统一错误契约（4001 参数错 / 4002 JSON 坏 / 4003 未登录 / 4004 限流 / 5000 内部错 / 5001 上游失败）
- 接口文档：springdoc-openapi 3.1.0

## 测试与 CI

- 8 个接口用例（MockMvc + Mockito）：注册/登录/鉴权/限流全链路，BCrypt 与 JWT 真实运行，不依赖数据库与外部 API
- GitHub Actions：每次 push 自动执行 `mvnw test`（见仓库 Actions 标签页）

## 本地运行

```bash
./mvnw spring-boot:run        # 后端（8080）
# 数据库：MySQL（建表 SQL 见下方），本地开发可用 SSH 隧道连服务器库：
# ssh -L 3306:localhost:3306 ubuntu@<服务器IP>
```

建库建表：

```sql
CREATE DATABASE timetable_app CHARACTER SET utf8mb4;
CREATE TABLE timetable_app.users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 部署

腾讯云轻量 + systemd 守护（开机自启、崩溃自动拉起）。发版三步：

```bash
./mvnw package
scp target/weather-server-0.0.1-SNAPSHOT.jar ubuntu@<服务器IP>:/home/ubuntu/
sudo systemctl restart weather-server   # 服务器上执行
```
