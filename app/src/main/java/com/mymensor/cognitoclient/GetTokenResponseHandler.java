package com.mymensor.cognitoclient;

import android.util.Log;

/**
 * This class is used to parse the response of the GetToken call of the sample
 * Cognito developer authentication and store it as a GetTokenResponse object.
 */
public class GetTokenResponseHandler extends ResponseHandler {

    private final String key;

    public GetTokenResponseHandler(final String key) {
        this.key = key;
    }

    public Response handleResponse(int responseCode, String responseBody) {
        if (responseCode == 200) {
            try {
                String json = responseBody;
                String identityId = Utilities
                        .extractElement(json, "IdentityId");
                String identityPoolId = Utilities.extractElement(json,
                        "identityPoolId");
                String token = Utilities.extractElement(json, "Token");
                Log.d("GetTokenResponseHandlr ",identityId+" "+identityPoolId+" "+token);
                return new GetTokenResponse(identityId, identityPoolId, token);
            } catch (Exception exception) {
                return new GetTokenResponse(500, exception.getMessage());
            }
        } else {
            return new GetTokenResponse(responseCode, responseBody);
        }
    }

}
