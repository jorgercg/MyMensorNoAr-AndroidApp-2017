package com.mymensor;

import android.util.Log;

import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MymCrypt {

    private static final String TAG = "MymCrypt";

    public static String encrypt(byte[] raw, String stringClear) throws Exception {
        byte[] clear = stringClear.getBytes(Charset.forName("ISO-8859-1"));
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return new String(encrypted,Charset.forName("ISO-8859-1"));
    }

    public static String decrypt(byte[] raw, String stringEncrypted) throws Exception {
        byte[] encrypted = stringEncrypted.getBytes(Charset.forName("ISO-8859-1"));
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        String stringDecrypted = new String(decrypted,Charset.forName("ISO-8859-1"));
        return stringDecrypted;
    }

    public static byte[] getRawKey (byte[] keyStart){
        byte[] rawKey = new byte[0];
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(keyStart);
            kgen.init(256, sr);
            SecretKey skey = kgen.generateKey();
            rawKey = skey.getEncoded();
        } catch (Exception e){
            Log.d(TAG, "raw Key creation error:"+e.toString());
        }
        if (rawKey != null) {
            return rawKey;
        } else {
            return null;
        }
    }


}
