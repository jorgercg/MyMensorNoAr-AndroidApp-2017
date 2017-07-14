package com.mymensor;

import android.content.SharedPreferences;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;



public class MymCrypt {

    private static final String TAG = "MymCrypt";

    private static final int IV_SIZE = 16;
    private static final int KEY_SIZE = 32;

    private static byte[] encryptOrDecrypt(
            byte[] data, SecretKey key, byte[] iv, boolean isEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(isEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key,
                    new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("This is unconceivable!", e);
        }
    }

    public static byte[] encryptData(byte[] data, byte[] iv, SecretKey key) {
        return encryptOrDecrypt(data, key, iv, true);
    }

    public static byte[] decryptData(byte[] data, byte[] iv, SecretKey key) {
        return encryptOrDecrypt(data, key, iv, false);
    }

    public static byte[] retrieveIv(SharedPreferences sharedPref) {
        byte[] iv = new byte[IV_SIZE];
        // Create a random salt if encrypting for the first time, and save it for future use.
        String stringIv = sharedPref.getString(Constants.MYM_CLIENT_IV,"NOTSET");
        if (stringIv.equals("NOTSET")) {
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(iv);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Constants.MYM_CLIENT_IV, new String(iv,Charset.forName("ISO-8859-1")));
            editor.commit();
        } else {
            iv = stringIv.getBytes(Charset.forName("ISO-8859-1"));
        }
        return iv;
    }


    private static byte[] retrieveSalt(SharedPreferences sharedPref) {
        // Salt must be at least the same size as the key.
        byte[] salt = new byte[KEY_SIZE];
        // Create a random salt if encrypting for the first time, and save it for future use.
        String stringSalt = sharedPref.getString(Constants.MYM_CLIENT_SALT,"NOTSET");
        if (stringSalt.equals("NOTSET")) {
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(salt);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Constants.MYM_CLIENT_SALT, new String(salt,Charset.forName("ISO-8859-1")));
            editor.commit();
        } else {
            salt = stringSalt.getBytes(Charset.forName("ISO-8859-1"));
        }
        return salt;
    }

    public static SecretKey getSecretKeySecurely(String password, SharedPreferences sharedPref) {
        // Use this to derive the key from the password:
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), retrieveSalt(sharedPref),
                100 /* iterationCount */, KEY_SIZE * 8 /* key size in bits */);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("MymCrypt Exception!!!!", e);
        }
    }

}