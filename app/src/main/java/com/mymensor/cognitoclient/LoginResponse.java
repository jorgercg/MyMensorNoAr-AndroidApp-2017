package com.mymensor.cognitoclient;

/**
 * This class is used to store the response of the Login call of sample Cognito
 * developer authentication.
 */
public class LoginResponse extends Response {
    private final String key;
    private final String appMymensorGroup;

    public LoginResponse(final int responseCode, final String responseMessage) {
        super(responseCode, responseMessage);
        this.key = null;
        this.appMymensorGroup = null;
    }

    public LoginResponse(final String key, final String appMymensorGroup) {
        super(200, null);
        this.key = key;
        this.appMymensorGroup = appMymensorGroup;
    }

    public String getKey() {
        return this.key;
    }

    public String getAppMymensorGroup() {
        return this.appMymensorGroup;
    }
}
