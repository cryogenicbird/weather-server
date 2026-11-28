# 部署说明（双实例 + nginx）

```
客户端 → nginx :8080 ──轮询──→ 后端 #1 :8082
                      └────────→ 后端 #2 :8081
                                   ↓
                            MySQL / Redis（共享状态）
```

## 关键文件

| 文件 | 服务器位置 |
|---|---|
| `nginx.conf` | `/etc/nginx/sites-available/default` |
| `weather-server.service` | `/etc/systemd/system/weather-server.service`（实例 #1，8082） |
| `weather-server-2.service` | `/etc/systemd/system/weather-server-2.service`（实例 #2，8081） |

## 应用侧唯一要求

`application.properties` 中：

```properties
server.forward-headers-strategy=native
```

作用：还原 X-Forwarded-For 的真实客户端 IP，保证限流按 IP 生效。

## 发版流程

```bash
./mvnw package
scp target/weather-server-0.0.1-SNAPSHOT.jar ubuntu@<server>:/home/ubuntu/
sudo systemctl restart weather-server weather-server-2
```

## 验收

1. 分流可观测：`curl -si http://<server>:8080/weather/hourly/29.36/106.18 | grep -i x-upstream`，多次请求应在 8081/8082 间轮换
2. 限流共享：连打 61 次，第 61 次返回 429（若第 121 次才 429，说明计数未共享）
3. 真实 IP：`redis-cli keys 'rl:*'` 中应是客户端公网 IP，而不是 127.0.0.1
4. 内存：`free -h` 稳定、swap 不持续增长、`dmesg -T | grep -i "killed process"` 无 OOM

## 回退

```bash
sudo systemctl disable --now weather-server-2      # 停第二实例
# nginx.conf 的 upstream 只保留 8082 → 重载
sudo systemctl reload nginx
```
