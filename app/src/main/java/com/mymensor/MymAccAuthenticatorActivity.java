package com.mymensor;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * The MymAccAuthenticatorActivity activity.
 * Called by the MymAccAuthenticator and in charge of identifing the user.
 * It sends back to the MymAccAuthenticator the result.
 */

public class MymAccAuthenticatorActivity extends AccountAuthenticatorActivity {
    private static final String TAG = "MymAccAuthActvty";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_PASS = "USER_PASS";

    private AccountManager mAccountManager;
    private String mAuthTokenType;

    public RequestQueue mRequestQueue;

    SharedPreferences sharedPref;

    /*
    private final int REQ_SIGNUP = 1;
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mymaccauthenticator);

        sharedPref = this.getSharedPreferences("com.mymensor.app",Context.MODE_PRIVATE);

        mAccountManager = AccountManager.get(getBaseContext());

        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);

        Bundle response = getIntent().getExtras();

        Log.d(TAG, "accountName:"+accountName+" Response: "+ response.toString());

        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);

        if (mAuthTokenType == null)
            mAuthTokenType = Constants.AUTHTOKEN_TYPE_FULL_ACCESS;

        Log.d(TAG, "mAuthTokenType:"+mAuthTokenType);

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        findViewById(R.id.signUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.REGISTER_SERVER));
                startActivity(browserIntent);
            }
        });
    }

    public void submit() {

        String userName = ((TextView) findViewById(R.id.accountName)).getText().toString();
        String userPass = ((TextView) findViewById(R.id.accountPassword)).getText().toString();

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", userName);
        params.put("password", userPass);
        userPass = "";
        final String userNameInner = userName;
        final String userPassInner = userPass;
        final Bundle data = new Bundle();
        // Formulate the request and handle the response.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Constants.AUTH_SERVER, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            Log.d(TAG, "Respose mym_auth:"+ response.toString());
                            String authtoken = response.getString("token");
                            data.putString(AccountManager.KEY_ACCOUNT_NAME, userNameInner);
                            data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                            data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                            data.putString(AccountManager.KEY_PASSWORD, userPassInner);
                            final Intent res = new Intent();
                            res.putExtras(data);
                            finishLogin(res);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error MYM AUTH TOKEN: ", error.getMessage());
                data.putString(KEY_ERROR_MESSAGE, error.getMessage());
            }
        });

        // Add the request to the RequestQueue.
        mRequestQueue.add(jsonObjectRequest);
    }

    private void finishLogin(Intent intent) {
        Log.d(TAG,"finishLogin started");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.d(TAG, "finishLogin > addAccountExplicitly");
            Log.d(TAG, "Intent Extras:"+intent.getExtras().toString()+" mAuthTokenType: "+ mAuthTokenType);
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = mAuthTokenType;

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);
            //mAccountManager.setUserData(account, AccountManager.KEY_USERDATA, accountCognitoIdentityId);
        } else {
            Log.d(TAG, "finishLogin > setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.MYM_KEY, intent.getStringExtra(AccountManager.KEY_AUTHTOKEN));
        editor.putString(Constants.MYM_USER,intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
        editor.commit();
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);

        finish();
    }

}
