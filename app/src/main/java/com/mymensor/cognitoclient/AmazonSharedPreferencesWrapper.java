package com.mymensor.cognitoclient;

import android.content.SharedPreferences;

/**
 * This utility class is used to store content in Android's Shared Preferences.
 * For maximum security the preferences should be private.
 */
public class AmazonSharedPreferencesWrapper {

    private static final String MYM_USER_KEY = "MYM_USER_KEY";
    private static final String MYM_USER_GROUP = "MYM_USER_GROUP";

    /**
     * Set all of the Shared Preferences used by the sample Cognito developer
     * authentication application to null. This function is useful if the user
     * needs/wants to log out to clear any user specific information.
     */
    public static void wipe(SharedPreferences sharedPreferences) {
        AmazonSharedPreferencesWrapper.storeValueInSharedPreferences(
                sharedPreferences, MYM_USER_KEY, null);
    }

    /**
     * Stores the UID and Key that were registered in the Shared Preferences.
     * The UID and Key and used to encrypt/decrypt the Token that is returned
     * from the sample Cognito developer authentication application.
     */
    public static void registerUserKey(SharedPreferences sharedPreferences, String key) {
        AmazonSharedPreferencesWrapper.storeValueInSharedPreferences(
                sharedPreferences, MYM_USER_KEY, key);
    }

    /**
     * Returns the current Key stored in Shared Preferences.
     */
    public static String getKeyForUser(SharedPreferences sharedPreferences) {
        return AmazonSharedPreferencesWrapper.getValueFromSharedPreferences(
                sharedPreferences, MYM_USER_KEY);
    }

    public static void registerUserGroup(SharedPreferences sharedPreferences, String group) {
        AmazonSharedPreferencesWrapper.storeValueInSharedPreferences(
                sharedPreferences, MYM_USER_GROUP, group);
    }

    public static String getGroupForUser(SharedPreferences sharedPreferences) {
        return AmazonSharedPreferencesWrapper.getValueFromSharedPreferences(
                sharedPreferences, MYM_USER_GROUP);
    }

    protected static void storeValueInSharedPreferences(
            SharedPreferences sharedPreferences, String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    protected static String getValueFromSharedPreferences(
            SharedPreferences sharedPreferences, String key) {
        return sharedPreferences.getString(key, null);
    }
}
