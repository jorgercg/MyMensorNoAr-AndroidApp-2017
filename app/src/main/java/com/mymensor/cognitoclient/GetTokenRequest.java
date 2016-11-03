package com.mymensor.cognitoclient;

import java.net.URL;

/**
 * A class used to construct the GetToken request to the sample Cognito
 * developer authentication application.
 */
public class GetTokenRequest extends Request {

    private final URL endpoint;

    public GetTokenRequest(final URL endpoint) {
        this.endpoint = endpoint;
    }

    /*
     * (non-Javadoc)
     * @see com.amazonaws.cognito.sync.devauth.client.Request#buildRequestUrl()
     * Constructs the request url for GetToken call to sample Cognito developer
     * authentication. The signature is a calculated on the concatenation of
     * timestamp and the contents of the logins mao.
     */
    @Override
    public String buildRequestUrl() {
        String url = this.endpoint.toString();

        StringBuilder builder = new StringBuilder(url);
        if (!url.endsWith("/")) {
            builder.append("/");
        }

        return builder.toString();
    }

}
