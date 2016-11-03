package com.mymensor;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import com.mymensor.cognitoclient.Response;

/**
 * A class which performs the task of authentication the user. For the sample it
 * validates a set of username and possword against the sample Cognito developer
 * authentication application
 */
public class DeveloperAuthenticationTask extends
        AsyncTask<LoginCredentials, Void, Void> {

    private final Context context;

    public DeveloperAuthenticationTask(Context context) {
        this.context = context;
    }

    private static final String TAG = "DvlprAuthTask";
    // The user name or the developer user identifier you will pass to the
    // Amazon Cognito in the GetOpenIdTokenForDeveloperIdentity API
    private String mymToken;

    private boolean isSuccessful;


    @Override
    protected Void doInBackground(LoginCredentials... params) {

        Response response = DeveloperAuthenticationProvider
                .getDevAuthClientInstance()
                .login(params[0].getMyMToken());
        isSuccessful = response.requestWasSuccessful();
        mymToken = params[0].getMyMToken();

        if (isSuccessful) {
            CognitoSyncClientManager
                    .addLogins(
                            ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                                    .getIdentityProvider()).getProviderName(),
                            mymToken);
            // Always remember to call refresh after updating the logins map
            ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                    .getIdentityProvider()).refresh();
        }
        return null;
    }
    @Override
    protected void onPostExecute(Void result) {
        if (isSuccessful) {
            Log.d("DvlpAuthTask"," Login OK ");
            //new AlertDialog.Builder(context).setTitle("Login OK").setMessage("Success!!").show();
        }
        if (!isSuccessful) {
            Log.d("DvlpAuthTask"," Login error ");
            //new AlertDialog.Builder(context).setTitle("Login error").setMessage("Credentials not accepted!!").show();
        }
    }
}