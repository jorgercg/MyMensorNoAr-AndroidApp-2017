package com.mymensor.cognitoclient;

import android.content.SharedPreferences;
import android.util.Log;

import java.net.URL;
import java.util.Map;

/**
 * This class is used to communicate with the sample Cognito developer
 * authentication application sample.
 */
public class AmazonCognitoSampleDeveloperAuthenticationClient {
    private static final String LOG_TAG = "AmzCgntSmplDvlpAuthClnt";

    /**
     * The endpoint for the sample Cognito developer authentication application.
     */
    private final URL endpoint;

    /**
     * The shared preferences where user key is stored.
     */
    private final SharedPreferences sharedPreferences;

    public AmazonCognitoSampleDeveloperAuthenticationClient(
            SharedPreferences sharedPreferences, URL endpoint) {
        this.endpoint = endpoint;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * Gets a token from the sample Cognito developer authentication
     * application. The registered key is used to secure the communication.
     */
    public Response getToken(Map<String, String> logins, String identityId) {
        String key = AmazonSharedPreferencesWrapper
                .getKeyForUser(this.sharedPreferences);

        Request getTokenRequest = new GetTokenRequest(this.endpoint);
        ResponseHandler handler = new GetTokenResponseHandler(key);

        GetTokenResponse getTokenResponse = (GetTokenResponse) this
                .processRequest(getTokenRequest, handler, key);

        // TODO: You can cache the open id token as you will have the control
        // over the duration of the token when it is issued. Caching can reduce
        // the communication required between the app and your backend
        return getTokenResponse;
    }

    /**
     * Using the given username and password, securily communictes the Key for
     * the user's account.
     */
    public Response login(String mymToken) {
        Response response = Response.SUCCESSFUL;
        LoginRequest loginRequest = new LoginRequest(this.endpoint);
        ResponseHandler handler = new LoginResponseHandler();
        response = this.processRequest(loginRequest, handler, mymToken);
        if (response.requestWasSuccessful()) {
            AmazonSharedPreferencesWrapper.registerUserKey(
                    this.sharedPreferences,((LoginResponse) response).getKey());
            AmazonSharedPreferencesWrapper.registerUserGroup(
                    this.sharedPreferences,((LoginResponse) response).getAppMymensorGroup());
            Log.d(LOG_TAG,"appMymensorGroup:"+((LoginResponse) response).getAppMymensorGroup());
        }
        return response;
    }

    /**
     * Process Request
     */
    protected Response processRequest(Request request, ResponseHandler handler, String key) {
        Response response = null;
        int retries = 2;
        do {
            response = CognitoSampleDeveloperAuthenticationService.sendRequest(
                    request, handler, key);
            if (response.requestWasSuccessful()) {
                return response;
            } else {
                Log.w(LOG_TAG,
                        "Request to Cognito Sample Developer Authentication Application failed with Code: ["
                                + response.getResponseCode() + "] Message: ["
                                + response.getResponseMessage() + "]");
            }
        } while (retries-- > 0);

        return response;
    }
}
