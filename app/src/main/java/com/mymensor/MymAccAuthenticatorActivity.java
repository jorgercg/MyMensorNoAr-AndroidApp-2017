package com.mymensor;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    public final static int ARG_ACCOUNT_REQUEST_CODE = 1234;

    private AccountManager mAccountManager;
    private String mAuthTokenType;

    public RequestQueue mRequestQueue;

    SharedPreferences sharedPref;

    public ProgressDialog pdWaitingForNewtwork;

    /*
    private final int REQ_SIGNUP = 1;
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mymaccauthenticator);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mAccountManager = AccountManager.get(getBaseContext());

        mRequestQueue = Volley.newRequestQueue(getApplicationContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);

        Bundle response = getIntent().getExtras();

        Log.d(TAG, "accountName:" + accountName + " Response: " + response.toString());

        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);

        if (mAuthTokenType == null)
            mAuthTokenType = Constants.AUTHTOKEN_TYPE_FULL_ACCESS;

        Log.d(TAG, "mAuthTokenType:" + mAuthTokenType);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
            ;

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        pdWaitingForNewtwork = new ProgressDialog(MymAccAuthenticatorActivity.this);

        if (availableAccounts.length == 0) {
            Log.d(TAG, "availableAccounts[] = " + "nada!!!!" + " Qty= 0");
            findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submit();
                }
            });
            findViewById(R.id.signUp).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);

                    //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.REGISTER_SERVER));
                    //startActivity(browserIntent);

                    startActivityForResult(intent, ARG_ACCOUNT_REQUEST_CODE);

                }
            });
        } else {
            Log.d(TAG, "availableAccounts[] = " + availableAccounts[0] + " Qty=" + availableAccounts.length);
            Log.d(TAG, "One user is already logged in, this app accepts only one user per device!");
            showAlertOnlyOneUserPerDevice();
        }
    }


    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        if (requestCode == ARG_ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
            final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            // Change the view to make space for the email and new buttons
            ((TextView) findViewById(R.id.accountEmail)).setText(accountName);
            findViewById(R.id.accountEmail).setVisibility(View.VISIBLE);
            findViewById(R.id.submit).setVisibility(View.GONE);
            findViewById(R.id.signUp).setVisibility(View.GONE);
            findViewById(R.id.termslinearlayout).setVisibility(View.VISIBLE);
            findViewById(R.id.register).setVisibility(View.VISIBLE);
            findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signUpWithAppMymensor(accountName);
                }
            });

        }
    }

    public void onClickFirstLink(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMSOFSERV_URI));
        startActivity(browserIntent);
    }

    public void onClickSecondLink(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_URI));
        startActivity(browserIntent);
    }


    public void signUpWithAppMymensor(String userEmail) {

        if (((CheckBox) findViewById(R.id.terms_check)).isChecked()) {
            pdWaitingForNewtwork.setTitle(getString(R.string.creatinguser));
            pdWaitingForNewtwork.setMessage(getString(R.string.waitingforserver));
            pdWaitingForNewtwork.setCancelable(false);
            pdWaitingForNewtwork.show();

            String userName = ((TextView) findViewById(R.id.accountName)).getText().toString();
            final String userPass = ((TextView) findViewById(R.id.accountPassword)).getText().toString();

            Map<String, String> params = new HashMap<String, String>();
            params.put("email", userEmail);
            params.put("username", userName);
            params.put("password", userPass);
            // Formulate the request and handle the response.
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Constants.REGISTER_SERVER, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                VolleyLog.v("signUpWithAppMymensor: Response to SignUp:%n %s", response.toString());
                                Log.d(TAG, "signUpWithAppMymensor: Response SignUp:" + response.toString());
                                submitAfterRegistration(response.getString("username"), userPass);


                            } catch (JSONException e) {
                                e.printStackTrace();

                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String body = "nada!!!!";
                    //get status code here
                    String statusCode = String.valueOf(error.networkResponse.statusCode);
                    //get response body and parse with appropriate encoding
                    if (error.networkResponse.data != null) {
                        try {
                            body = new String(error.networkResponse.data, "UTF-8");
                            Log.d(TAG, "signUpWithAppMymensor: Original body: [" + body + "]");
                            body = body.replace("{", "");
                            body = body.replace("}", "");
                            body = body.replace("[", "");
                            body = body.replace("]", "");
                            body = body.replace("\"", "");
                            body = body.replace(":", "\n\n");
                            body = body.replace(",", "\n");
                            body = body.substring(0, 1).toUpperCase() + body.substring(1);
                        } catch (java.io.UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    //do stuff with the body...
                    Log.d(TAG, "signUpWithAppMymensor: Error status code: [" + statusCode + "]");
                    Log.d(TAG, "signUpWithAppMymensor: Error response body: [" + body + "]");
                    VolleyLog.e("signUpWithAppMymensor: Error Registering with App MyM: ", error.getMessage());
                    pdWaitingForNewtwork.dismiss();
                    showAlertRegistration(body);
                }
            }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Accept-Language", Locale.getDefault().getLanguage()+"-"+Locale.getDefault().getCountry());
                    return headers;
                }
            };

            // Add the request to the RequestQueue.

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            mRequestQueue.add(jsonObjectRequest);
        } else {
            showAlertRegistration(getString(R.string.checknotchecked));
        }
    }

    public void submitAfterRegistration(String userName, String userPass) {

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
                            VolleyLog.v("SUBMIT AFTER REGISTRATION: Response:%n %s", response.toString(4));
                            Log.d(TAG, "SUBMIT AFTER REGISTRATION: Response mym_auth:" + response.toString());
                            String authtoken = response.getString("token");
                            data.putString(AccountManager.KEY_ACCOUNT_NAME, userNameInner);
                            data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                            data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                            data.putString(AccountManager.KEY_PASSWORD, userPassInner);
                            final Intent res = new Intent();
                            res.putExtras(data);
                            pdWaitingForNewtwork.dismiss();
                            finishLogin(res);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("SUBMIT AFTER REGISTRATION:Error MYM AUTH TOKEN: ", error.getMessage());
                data.putString(KEY_ERROR_MESSAGE, error.getMessage());
                pdWaitingForNewtwork.dismiss();
                showAlert();
            }
        });

        // Add the request to the RequestQueue.
        mRequestQueue.add(jsonObjectRequest);
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
                            VolleyLog.v("SUBMIT: Response:%n %s", response.toString(4));
                            Log.d(TAG, "SUBMIT: Response mym_auth:" + response.toString());
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
                VolleyLog.e("SUBMIT: Error MYM AUTH TOKEN: ", error.getMessage());
                data.putString(KEY_ERROR_MESSAGE, error.getMessage());
                showAlert();
            }
        });

        // Add the request to the RequestQueue.
        mRequestQueue.add(jsonObjectRequest);
    }

    private void finishLogin(Intent intent) {
        Log.d(TAG, "finishLogin started");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.d(TAG, "finishLogin > addAccountExplicitly");
            Log.d(TAG, "Intent Extras:" + intent.getExtras().toString() + " mAuthTokenType: " + mAuthTokenType);
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
        editor.putString(Constants.MYM_USER, intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
        editor.commit();
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);

        finish();
    }

    public void showAlertRegistration(String alertText) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getText(R.string.registrationfailed));
        alert.setMessage(alertText);

        alert.setNegativeButton(getText(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alert.show();
    }

    public void showAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getText(R.string.loginfailed));
        alert.setMessage(getText(R.string.pta));

        alert.setNegativeButton(getText(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alert.show();
    }

    public void showAlertOnlyOneUserPerDevice() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getText(R.string.cantadduser));
        alert.setMessage(getText(R.string.onlyoneperdev));

        alert.setNegativeButton(getText(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });

        alert.show();
    }

}
