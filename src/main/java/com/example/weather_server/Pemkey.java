package com.example.weather_server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 密钥加载：优先读【环境变量指定的文件路径】（生产：服务器 secrets 目录，chmod 600，不进 jar）；
 * 没配路径时回退 classpath（本地开发/CI 现场生成的测试密钥）。
 */
public final class Pemkey {
    private Pemkey() {}

    public static PrivateKey loadPrivateKey() throws Exception {
        String pem = readPem("JWT_PRIVATE_KEY_PATH", "ed25519-private.pem");
        String body = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        return KeyFactory.getInstance("Ed25519")
            .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
    }

    public static PublicKey loadPublicKey() throws Exception {
        String pem = readPem("JWT_PUBLIC_KEY_PATH", "ed25519-public.pem");
        String body = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        return KeyFactory.getInstance("Ed25519")
            .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(body)));
    }

    /** 环境变量有路径 → 读文件；否则读 classpath */
    private static String readPem(String envName, String classpathName) throws Exception {
        String path = System.getenv(envName);
        if (path != null && !path.isBlank()) {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        }
        try (InputStream in = Pemkey.class.getClassLoader()
                .getResourceAsStream(classpathName)) {
            if (in == null) {
                throw new IllegalStateException(
                    "找不到密钥：" + classpathName + "（可设置环境变量 " + envName + " 指向文件路径）");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Url(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
