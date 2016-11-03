package com.mymensor;

/**
 * A class used to encapsulate user's credentials for showing the
 * developer authenticated feature of Amazon Cognito
 */
public class LoginCredentials {
    private String mymtoken;

    public LoginCredentials(String mymtoken) {
        this.mymtoken = mymtoken;
    }

    public String getMyMToken() {
        return mymtoken;
    }

    public void setMyMToken(String mymtoken) {
        this.mymtoken = mymtoken;
    }
}
