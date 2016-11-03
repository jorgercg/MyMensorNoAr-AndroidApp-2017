package com.mymensor.cognitoclient;

import java.net.URL;

/**
 * This class is used to construct the Login request for communication with
 * sample Cognito developer authentication.
 */
public class LoginRequest extends Request {

    private final URL endpoint;
    // private final String uid;
    // private final String username;
    // private final String password;
    // private final String appName;

    //  private final String decryptionKey;

    public LoginRequest(final URL endpoint) //, final String appName,
    //final String uid, final String username, final String password) {
    {
        this.endpoint = endpoint;
        //    this.appName = appName;
        //    this.uid = uid;
        //    this.username = username;
        //    this.password = password;

        //  this.decryptionKey = this.computeDecryptionKey();
    }
    /*
        public String getDecryptionKey() {
            return this.decryptionKey;
        }
    */
    /*
     * (non-Javadoc)
     * @see com.amazonaws.cognito.sync.devauth.client.Request#buildRequestUrl()
     */
    @Override
    public String buildRequestUrl() {
        String url = this.endpoint.toString();

        StringBuilder builder = new StringBuilder(url);
        if (!url.endsWith("/")) {
            builder.append("/");
        }
        /*
        String timestamp = Utilities.getTimestamp();
        String signature = Utilities
                .getSignature(timestamp, this.decryptionKey);

        builder.append("login/");
        builder.append("?uid=" + HttpUtils.urlEncode(this.uid, false));
        builder.append("&username=" + HttpUtils.urlEncode(this.username, false));
        builder.append("&timestamp=" + HttpUtils.urlEncode(timestamp, false));
        builder.append("&signature=" + HttpUtils.urlEncode(signature, false));
*/
        return builder.toString();
    }

    /**
     * This function computes the decryption key

     protected String computeDecryptionKey() {
     try {
     String salt = this.username + this.appName + this.endpoint.getHost();
     return Utilities.getSignature(salt, this.password);
     } catch (Exception exception) {
     return null;
     }
     }*/
}
