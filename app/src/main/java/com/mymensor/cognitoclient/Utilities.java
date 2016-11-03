package com.mymensor.cognitoclient;

import android.util.Log;

/**
 * Utility class for communicating with sample Cognito developer authentication.
 */
public class Utilities {

    public static String extractElement(String json, String element) {
        boolean hasElement = (json.indexOf(element) != -1);
        if (hasElement) {
            Log.i("help", json);
            int elementIndex = json.indexOf(element) + element.length() + 1;
            int startIndex = json.indexOf("\"", elementIndex);
            int endIndex = json.indexOf("\"", startIndex + 1);
            Log.i("help", json.substring(startIndex + 1, endIndex));
            return json.substring(startIndex + 1, endIndex);
        }

        return null;
    }

}
