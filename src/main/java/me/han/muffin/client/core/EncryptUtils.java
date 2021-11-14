package me.han.muffin.client.core;

import me.han.muffin.client.utils.network.HttpRequestException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class EncryptUtils {
    private static final String ALGORITHM = "AES";

    public static String encrypt(String strToEncrypt, String secret) {
       // BASE64Encoder base64encoder = new BASE64Encoder();
        try {
            setKey(secret);
            byte[] bytes = strToEncrypt.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] doFinal = cipher.doFinal(bytes);
            return Base64.encodeBytes(doFinal);
        } catch (Exception e) {
            return "ERROR";
        }
    }

    public static String decrypt(String strToDecrypt, String secret) {
      //  BASE64Decoder base64decoder = new BASE64Decoder();
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytes = Base64.decode(strToDecrypt);
            byte[] doFinal = cipher.doFinal(bytes);
            return new String(doFinal, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private static SecretKeySpec secretKey;

    public static void setKey(String myKey) {
        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, ALGORITHM);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encode the given URL as an ASCII {@link String}
     * <p>
     * This method ensures the path and query segments of the URL are properly
     * encoded such as ' ' characters being encoded to '%20' or any UTF-8
     * characters that are non-ASCII. No encoding of URLs is done by default by
     * the {Connection} constructors and so if URL encoding is needed this
     * method should be called before calling the {Connection} constructor.
     *
     * @param url
     * @return encoded URL
     * @throws HttpRequestException
     */
    public static String encode(final CharSequence url) throws HttpRequestException {
        URL parsed;

        try {
            parsed = new URL(url.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }

        String host = parsed.getHost();
        int port = parsed.getPort();

        if (port != -1)
            host += ':' + port;

        try {
            String encoded = new URI(parsed.getProtocol(), host, parsed.getPath(),
                    parsed.getQuery(), null).toASCIIString();
            int paramsStart = encoded.indexOf('?');
            if (paramsStart > 0 && paramsStart + 1 < encoded.length())
                encoded = encoded.substring(0, paramsStart + 1)
                        + encoded.substring(paramsStart + 1).replace("+", "%2B");

            return encoded;
        } catch (URISyntaxException e) {
            IOException io = new IOException("Parsing URI failed", e);
            throw new HttpRequestException(io);
        }
    }


}