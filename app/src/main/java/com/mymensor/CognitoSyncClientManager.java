package com.mymensor;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSAbstractCognitoIdentityProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.util.HashMap;
import java.util.Map;

public class CognitoSyncClientManager {

    private static final String TAG = "CognitoSyncClientMnager";

    /**
     * Enter here the Identity Pool associated with your app and the AWS
     * region where it belongs. Get this information from the AWS console.
     */

    private static final String IDENTITY_POOL_ID = Constants.COGNITO_POOL_ID;
    private static final Regions REGION = Constants.COGNITO_POOL_ID_REGION;

    //private static CognitoSyncManager syncClient;
    private static AmazonS3Client syncClient;
    protected static CognitoCachingCredentialsProvider credentialsProvider = null;
    protected static AWSAbstractCognitoIdentityProvider developerIdentityProvider;

    /**
     * Set this flag to true for using developer authenticated identities
     * Make sure you configured it in DeveloperAuthenticationProvider.java.
     */
    private static boolean useDeveloperAuthenticatedIdentities = true;


    /**
     * Initializes the Cognito Identity and Sync clients. This must be called before getInstance().
     *
     * @param context a context of the app
     */
    public static void init(Context context) {

        if (syncClient != null) return;

        if (useDeveloperAuthenticatedIdentities) {
            developerIdentityProvider = new DeveloperAuthenticationProvider(
                    null, IDENTITY_POOL_ID, context, Regions.US_EAST_1);
            credentialsProvider = new CognitoCachingCredentialsProvider(context, developerIdentityProvider,
                    REGION);
            Log.i(TAG, "Using developer authenticated identities");
        } else {
            credentialsProvider = new CognitoCachingCredentialsProvider(context, IDENTITY_POOL_ID,
                    REGION);
            Log.i(TAG, "Developer authenticated identities is not configured");
        }

        //syncClient = new CognitoSyncManager(context, REGION, credentialsProvider);
        syncClient = new AmazonS3Client(credentialsProvider);
    }

    /**
     * Sets the login so that you can use authorized identity. This requires a
     * network request, so you should call it in a background thread.
     *
     * @param providerName the name of the external identity provider
     * @param token openId token
     */
    public static void addLogins(String providerName, String token) {
        if (syncClient == null) {
            throw new IllegalStateException("CognitoSyncClientManager not initialized yet");
        }

        Map<String, String> logins = credentialsProvider.getLogins();
        if (logins == null) {
            logins = new HashMap<String, String>();
        }
        logins.put(providerName, token);
        credentialsProvider.setLogins(logins);
    }

    /**
     * Gets the singleton instance of the CognitoClient. init() must be called
     * prior to this.
     *
     * @return an instance of CognitoClient
     *
     * public static CognitoSyncManager getInstance() {
     *
     */
    public static AmazonS3Client getInstance() {
        if (syncClient == null) {
            throw new IllegalStateException("CognitoSyncClientManager not initialized yet");
        }
        return syncClient;
    }

    /**
     * Returns a credentials provider object
     *
     * @return
     */
    public static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }
}
