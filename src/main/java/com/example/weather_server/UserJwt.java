package com.example.weather_server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import tools.jackson.databind.ObjectMapper;

/** 用户登录 token：一年有效期（MVP 选长 token 换体验，升级路线是 refresh 轮换） */
public final class UserJwt {

    private UserJwt() {}

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
    /** 验签 + 过期检查：签名对且未过期才算有效 */
    public static boolean verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
        // ① 验签：签名对不上 = 伪造/篡改
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(Pemkey.loadPublicKey());
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                return false;
            }
        // ② 过期检查：一年后 token 自动作废
            String payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = new ObjectMapper().readTree(payloadJson).get("exp").asLong();
            return exp > System.currentTimeMillis() / 1000;
    } 
        catch (Exception e) {
            return false;   // 任何异常都视为无效 token
    }
}
}