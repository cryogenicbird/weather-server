package com.example.weather_server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** 用户登录 token：一年有效期（MVP 选长 token 换体验，升级路线是 refresh 轮换） */
public class UserJwt {

    public static String sign(String username) {
        try {
            PrivateKey key = Pemkey.loadPrivateKey();
            long now = System.currentTimeMillis() / 1000;
            String header = "{\"alg\":\"EdDSA\"}";
            String payload = String.format(
                "{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                username, now - 30, now + 365L * 24 * 3600);  // 一年

            String h = Pemkey.base64Url(header);
            String p = Pemkey.base64Url(payload);
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(key);
            signer.update((h + "." + p).getBytes(StandardCharsets.UTF_8));
            return h + "." + p + "." + Pemkey.base64Url(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("用户 token 签名失败", e);
        }
    }
}