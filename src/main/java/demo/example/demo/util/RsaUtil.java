package demo.example.demo.util;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaUtil {

    public static String encryptPassword(String plainPassword, String base64PublicKey) {
        try {
            // 1) Clean the key: remove \n and \r
            String cleanKey = base64PublicKey
                    .replace("\n", "")
                    .replace("\r", "")
                    .trim();

            // 2) Decode
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password with RSA", e);
        }
    }
}
