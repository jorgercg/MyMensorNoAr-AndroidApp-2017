package com.mymensor.cognitoclient;

/**
 * This class is used to store the response of the GetToken call of the sample
 * Cognito developer authentication.
 */
public class GetTokenResponse extends Response {
    private final String identityId;
    private final String identityPoolId;
    private final String token;

    public GetTokenResponse(final int responseCode, final String responseMessage) {
        super(responseCode, responseMessage);
        this.identityId = null;
        this.identityPoolId = null;
        this.token = null;
    }

    public GetTokenResponse(final String identityId,
                            final String identityPoolId, final String token) {
        super(200, null);
        this.identityId = identityId;
        this.identityPoolId = identityPoolId;
        this.token = token;
    }

    public String getIdentityId() {
        return this.identityId;
    }

    public String getIdentityPoolId() {
        return this.identityPoolId;
    }

    public String getToken() {
        return this.token;
    }
}
