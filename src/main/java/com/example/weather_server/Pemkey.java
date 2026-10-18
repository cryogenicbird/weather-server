package com.example.weather_server;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class Pemkey{
    private Pemkey() {}
    public static final PrivateKey loadPrivateKey() throws Exception {
        try (InputStream in = UserJwt.class.getClassLoader()
                .getResourceAsStream("ed25519-private.pem")) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String body = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            return KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
        }
    }
    public static final String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Url(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

}