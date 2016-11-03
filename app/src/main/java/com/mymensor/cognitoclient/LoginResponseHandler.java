package com.mymensor.cognitoclient;

/**
 * This class is used to parse the response of the Login request of the sample
 * Cognito developer authentication and convert it into LoginResponse object
 */
public class LoginResponseHandler extends ResponseHandler {
    /*
    private final String decryptionKey;

    public LoginResponseHandler(final String decryptionKey) {
        this.decryptionKey = decryptionKey;
    }
    */
    public Response handleResponse(int responseCode, String responseBody) {
        if (responseCode == 200) {
            try {
                String json = responseBody;
                return new LoginResponse(Utilities.extractElement(json, "key"));
            } catch (Exception exception) {
                return new LoginResponse(500, exception.getMessage());
            }
        } else {
            return new LoginResponse(responseCode, responseBody);
        }
    }
}
