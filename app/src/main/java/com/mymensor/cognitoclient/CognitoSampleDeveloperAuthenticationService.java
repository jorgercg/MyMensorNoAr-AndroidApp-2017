package com.mymensor.cognitoclient;

import android.util.Log;

import com.mymensor.Constants;
import com.mymensor.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility class for communicating with sample Cognito developer
 * authentication application.
 */
public class CognitoSampleDeveloperAuthenticationService {
    private static final String LOG_TAG = "CogntSmplDvlprAuthSrv";
    private static final String ERROR = "Internal Server Error";
    public static int isApprovedByCognitoState = 0;
    public static int qtyClientsExceededState = 0;

    /**
     * A function to send request to the sample Cognito developer authentication
     * application
     *
     * @param request
     * @param reponseHandler
     * @return
     */
    public static Response sendRequest(Request request,
                                       ResponseHandler reponseHandler, String mymToken) {
        int responseCode = 0;
        String responseBody = null;
        String requestUrl = null;

        try {
            requestUrl = request.buildRequestUrl();

            Log.i(LOG_TAG, "Sending Request:[" + requestUrl + "]:Token=["+mymToken+"]:CST=["+Constants.CLIENT_SOFTWARE_TYPE+"]:ID=["+MainActivity.mymClientGUID+"]");

            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestProperty("Authorization","Token " + mymToken);
            connection.setRequestProperty("From", Constants.CLIENT_SOFTWARE_TYPE);
            connection.setRequestProperty("Warning", MainActivity.mymClientGUID);
            Log.i(LOG_TAG, "mymClientGUID : [" + MainActivity.mymClientGUID + "]");
            responseCode = connection.getResponseCode();
            responseBody = CognitoSampleDeveloperAuthenticationService
                    .getResponse(connection);
            if (responseCode==200) {
                isApprovedByCognitoState =1;
            } else {
                if (responseCode==432) {
                    responseBody="Trial Expired";
                    isApprovedByCognitoState =3;
                } else {
                    if (responseCode==434) {
                        responseBody="Subscription Expired";
                        isApprovedByCognitoState =4;
                    } else {
                        isApprovedByCognitoState =2;
                    }
                }
            }
            if (responseCode==433) {
                responseBody="Quantity of concurrent clients limit exceeded";
                qtyClientsExceededState = 1;
            } else {
                qtyClientsExceededState = 2;
            }
            Log.i(LOG_TAG, "ResponseCode : [" + responseCode + "]");
            Log.i(LOG_TAG, "ResponseBody : [" + responseBody + "]");

            return reponseHandler.handleResponse(responseCode, responseBody);
        } catch (IOException exception) {
            Log.w(LOG_TAG, exception);
            if (exception.getMessage().equals(
                    "Received authentication challenge is null")) {
                return reponseHandler.handleResponse(401,
                        "Unauthorized token request");
            } else {
                return reponseHandler.handleResponse(404,
                        "Unable to reach resource at [" + requestUrl + "]");
            }
        } catch (Exception exception) {
            Log.w(LOG_TAG, exception);
            return reponseHandler.handleResponse(responseCode, responseBody);
        }
    }

    /**
     * Function to get the response from sample Cognito developer authentication
     * application.
     *
     * @param connection
     * @return
     */
    protected static String getResponse(HttpURLConnection connection) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        InputStream inputStream = null;
        try {
            baos = new ByteArrayOutputStream(1024);
            int length = 0;
            byte[] buffer = new byte[1024];

            if (connection.getResponseCode() == 200) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }

            return baos.toString();
        } catch (Exception exception) {
            Log.w(LOG_TAG, exception);
            return ERROR;
        } finally {
            try {
                baos.close();
            } catch (Exception exception) {
                Log.w(LOG_TAG, exception);
            }
        }
    }
}
