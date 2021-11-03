package com.ebay.dss.zds.common;

import com.ebay.dss.zds.serverconfig.RSACipherProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class RSACipher {

    public static final String CHARSET = "UTF8";
    public static final String CIPHER_ALG = "RSA/ECB/PKCS1Padding";
    public static final String KEY_ALG = "RSA";
    public static final String PROVIDER = "BC";
    private static final Logger logger = LogManager.getLogger();

    static {
        Security.addProvider(new BouncyCastleProvider());
        if (Security.getProvider("BC") == null) {
            logger.warn("The provider had to be added to the java.security.Security");
            Security.addProvider(new BouncyCastleProvider());
        }

    }

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RSACipher(RSACipherProperties properties) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALG, PROVIDER);
        this.privateKey = initPrivateKey(properties.getPrivateKey(), keyFactory);
        this.publicKey = initPublicKey(properties.getPublicKey(), keyFactory);
    }

    private static PrivateKey initPrivateKey(String privateKeyEncoded, KeyFactory keyFactory) throws InvalidKeySpecException {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);
        KeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    private static PublicKey initPublicKey(String publicKeyEncoded, KeyFactory keyFactory) throws InvalidKeySpecException {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyEncoded);
        KeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public String encrypt(String plainText) {
        String cipherText;
        try {
            Cipher encryptCipher = Cipher.getInstance(CIPHER_ALG, PROVIDER);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] messageBinary = plainText.getBytes(CHARSET);
            byte[] res = encryptCipher.doFinal(messageBinary);
            cipherText = Base64.getEncoder().encodeToString(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cipherText;
    }

    public String decrypt(String cipherText) {
        String plainText;
        try {
            Cipher decryptCipher = Cipher.getInstance(CIPHER_ALG, PROVIDER);
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] messageBinary = Base64.getDecoder().decode(cipherText);
            byte[] res = decryptCipher.doFinal(messageBinary);
            plainText = new String(res, CHARSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return plainText;
    }
}
