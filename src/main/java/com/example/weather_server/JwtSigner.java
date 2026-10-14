package com.example.weather_server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class JwtSigner {

    private static final String KID = "C4WMKXTD42";  // 凭据ID
    private static final String ISS = "Q11437F0FE";   // 开发者ID
    private static final String SUB = "3C2ERDUKBF";   // 项目ID

    /** 签一个 24 小时有效的 JWT */
    public static String sign(){
        try{PrivateKey key = loadPrivateKey();
        long now = System.currentTimeMillis() / 1000;

        String header = String.format("{\"alg\":\"EdDSA\",\"kid\":\"%s\"}", KID);
        String payload = String.format(
            "{\"iss\":\"%s\",\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
            ISS, SUB, now - 30, now + 86300);

        String h = base64Url(header);
        String p = base64Url(payload);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(key);
        signer.update((h + "." + p).getBytes(StandardCharsets.UTF_8));
        String s = base64Url(signer.sign());

        return h + "." + p + "." + s;
    }catch (Exception e) {
        throw new IllegalStateException("JWT 签名失败", e);
    }
    }

    /** 从 resources 里读 PEM 文件，翻译成 Java 认识的私钥对象 */
    private static PrivateKey loadPrivateKey() throws Exception {
        try (InputStream in = JwtSigner.class.getClassLoader()
                .getResourceAsStream("ed25519-private.pem")) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // 剥掉 PEM 的信封行（BEGIN/END），只留 Base64 主体
            String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(body);
            return KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        }
    }

    private static String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}
