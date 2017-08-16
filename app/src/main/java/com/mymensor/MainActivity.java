package com.mymensor;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.mymensor.cognitoclient.AmazonSharedPreferencesWrapper;
import com.mymensor.cognitoclient.AwsUtil;

import javax.crypto.SecretKey;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MYMMainActivity";

    static {
        System.loadLibrary("MyMensor");
    }

    public native String getSecretKeyFromJNI();

    public SecretKey aesKey;

    private static long back_pressed;

    private static final String STATE_DIALOG = "state_dialog";
    private static final String STATE_INVALIDATE = "state_invalidate";

    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL;
    }
    private static final String LAST_APP_VERSION = Constants.CURR_APP_VERSION;
    private static AppStart appStart = null;
    private String appStartState;
    public static String mymClientGUID;
    private String mymClientGUIDStored;

    private AccountManager mAccountManager;
    private AlertDialog mAlertDialog;
    private boolean mInvalidate;
    private boolean noUserLogged = true;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    // A List of all transfers
    private List<TransferObserver> observers;

    SharedPreferences sharedPref;

    LinearLayout mainLinearLayout;
    ImageView appLogo;
    Button logInOut;
    TextView userLogged;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            Toast.makeText(getBaseContext(), getString(R.string.sdk_lower_then_kitkat), Toast.LENGTH_LONG).show();
        }

        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(getBaseContext(), getString(R.string.no_camera), Toast.LENGTH_LONG).show();
        }

        permissionsRequest();

        mAccountManager = AccountManager.get(this);

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.GET_ACCOUNTS)!= PackageManager.PERMISSION_GRANTED);

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            try {
                Log.d(TAG, "onCreate: sharedPref: " + entry.getKey() + "=[" + entry.getValue().toString() +"]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mymClientGUIDStored = sharedPref.getString(Constants.MYM_CLIENT_GUID,"NOTSET");

        Log.d(TAG,"mymClientGUIDStored= ["+mymClientGUIDStored+"]");

        aesKey = MymCrypt.getSecretKeySecurely(getSecretKeyFromJNI(), sharedPref);

        if (mymClientGUIDStored.equals("NOTSET")){
            final String androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            mymClientGUID = androidId;
            mymClientGUIDStored = new String(MymCrypt.encryptData(mymClientGUID.getBytes(Charset.forName("ISO-8859-1")),
                    MymCrypt.retrieveIv(sharedPref),
                    aesKey),Charset.forName("ISO-8859-1"));
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Constants.MYM_CLIENT_GUID, mymClientGUIDStored);
            editor.commit();
        } else {
            mymClientGUID = new String(MymCrypt.decryptData(mymClientGUIDStored.getBytes(Charset.forName("ISO-8859-1")),
                    MymCrypt.retrieveIv(sharedPref),
                    aesKey),Charset.forName("ISO-8859-1"));
        }



        /*
         * Initializes the sync client. This must be call before you can use it.
         */
        CognitoSyncClientManager.init(getApplicationContext());

        setContentView(R.layout.activity_main);
        mainLinearLayout = (LinearLayout)findViewById(R.id.MainActivityLinearLayout);
        appLogo = (ImageView) findViewById(R.id.mainactivity_logo);
        userLogged = (TextView) findViewById(R.id.userlogstate_message);
        logInOut = (Button) findViewById(R.id.buttonlog);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        initData();

        switch (checkAppStart(this,sharedPref)) {
            case NORMAL:
                // We don't want to get on the user's nerves
                appStartState = "normal";
                break;
            case FIRST_TIME_VERSION:
                // TODO show what's new
                appStartState = "firstthisversion";
                break;
            case FIRST_TIME:
                // TODO show a tutorial
                appStartState = "firstever";
                break;
            default:
                break;
        }


        if (availableAccounts.length == 0){
            Log.d(TAG, "availableAccounts[] = " + "nada!!!!" + " Qty= 0");
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Constants.MYM_LAST_USER, null);
            editor.putString(AmazonSharedPreferencesWrapper.MYM_USER_GROUP, null);
            editor.commit();
        } else {
            Log.d(TAG, "availableAccounts[] = " + availableAccounts[0] + " Qty="+availableAccounts.length);
            if (!sharedPref.getString(Constants.MYM_LAST_USER, "").equals(availableAccounts[0].name)) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(Constants.MYM_LAST_USER, null);
                editor.putString(AmazonSharedPreferencesWrapper.MYM_USER_GROUP, null);
                editor.commit();
            };
        }

        if (availableAccounts.length == 0) {
            userLogged.setText(R.string.userstate_loggedout);
            logInOut.setVisibility(View.VISIBLE);
            noUserLogged = true;
        } else {
            logInOut.setVisibility(View.GONE);
            if (availableAccounts.length == 1){
                userLogged.setText(getText(R.string.userstate_loggedin)+" "+availableAccounts[0].name);
                noUserLogged = false;
                getExistingAccountAuthToken(availableAccounts[0], Constants.AUTHTOKEN_TYPE_FULL_ACCESS);
            }
        }

        boolean showDialog = false;
        boolean invalidate = false;

        if (savedInstanceState != null) {
            showDialog = savedInstanceState.getBoolean(STATE_DIALOG);
            invalidate = savedInstanceState.getBoolean(STATE_INVALIDATE);
            if (showDialog) {
                showAccountPicker(Constants.AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
            }

        }

        Log.d(TAG, "showDialog = " + showDialog + " invalidate="+invalidate);

        if (availableAccounts.length > 1){
            showAccountPicker(Constants.AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
        }

        if (!noUserLogged) {
            do {
                //Nothing....
            } while (sharedPref.getString(Constants.MYM_USER,"").equals(""));
        }

        if (!noUserLogged){
            Log.d(TAG, "OnCreate - Calling the imagecapactivity, with user="+sharedPref.getString(Constants.MYM_USER,""));
            Intent launch_intent = new Intent(getApplicationContext(),LoaderActivity.class);
            launch_intent.putExtra("activitytobecalled", "imagecapactivity");
            launch_intent.putExtra("account", sharedPref.getString(Constants.MYM_USER,""));
            launch_intent.putExtra("deviceid", mymClientGUID);
            launch_intent.putExtra("appstartstate", appStartState);
            startActivity(launch_intent);
            finish();
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A native method that is implemented by the project native library,
     * which is packaged with this application.

    public static native String stringFromJNI();
     */

    /**
     * Gets all relevant transfers from the Transfer Service
     */
    private void initData() {
        // Use TransferUtility to get all upload transfers.
        observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);
        for (TransferObserver observer : observers) {

            // Sets listeners to in progress transfers
            if (TransferState.WAITING.equals(observer.getState())
                    || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                    || TransferState.IN_PROGRESS.equals(observer.getState())) {
                transferUtility.resume(observer.getId());
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            outState.putBoolean(STATE_DIALOG, true);
            outState.putBoolean(STATE_INVALIDATE, mInvalidate);
        }
    }


    private void showAccountPicker(final String authTokenType, final boolean invalidate) {
        mInvalidate = invalidate;

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.GET_ACCOUNTS)!= PackageManager.PERMISSION_GRANTED);

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        if (availableAccounts.length == 0) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show();
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            mAlertDialog = new AlertDialog.Builder(this).setTitle("Pick Account").setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, name), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (invalidate)
                        invalidateAuthToken(availableAccounts[which], authTokenType);
                    else
                        getExistingAccountAuthToken(availableAccounts[which], authTokenType);
                    userLogged.setText(getText(R.string.userstate_loggedin)+" "+availableAccounts[which].name);
                    noUserLogged = false;
                    Log.d(TAG, "Account Picker - Calling the imagecapactivity, with user="+sharedPref.getString(Constants.MYM_USER,""));
                    Intent launch_intent = new Intent(getApplicationContext(),LoaderActivity.class);
                    launch_intent.putExtra("activitytobecalled", "imagecapactivity");
                    launch_intent.putExtra("account", sharedPref.getString(Constants.MYM_USER,""));
                    launch_intent.putExtra("deviceid", mymClientGUID);
                    launch_intent.putExtra("appstartstate", appStartState);
                    startActivity(launch_intent);
                    finish();
                }
            }).create();
            mAlertDialog.show();
        }
    }


    private void invalidateAuthToken(final Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null,null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();
                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    mAccountManager.invalidateAuthToken(account.type, authtoken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /* Method to get existing mym_authToken from Account manager and fetch Cognito Credentials from Cognito */
    private void getExistingAccountAuthToken(Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();
                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    final String userName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Constants.MYM_USER ,userName);
                    editor.putString(Constants.MYM_KEY,authtoken);
                    editor.commit();
                    Log.d(TAG, "GetToken Bundle is " + bnd);
                    logInOut.setVisibility(View.GONE);
                    Log.d(TAG, "Token is " + authtoken);
                    getCognitoIdAndToken(authtoken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void getCognitoIdAndToken(String authToken){
        // Clear the existing credentials
        CognitoSyncClientManager.credentialsProvider
                .clearCredentials();
        // Initiate user authentication against the
        // developer backend in this case the Cognito
        // developer authentication application.
        ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                .getIdentityProvider()).login(authToken, MainActivity.this);
    }


    private void addNewAccount(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    Log.d(TAG, "AddNewAccount Bundle is " + bnd.toString());
                    logInOut.setVisibility(View.GONE);
                    userLogged.setText(getText(R.string.userstate_loggedin)+" "+ bnd.getString("authAccount"));

                    Map<String, ?> keys = sharedPref.getAll();

                    for (Map.Entry<String, ?> entry : keys.entrySet()) {
                        try {
                            Log.d(TAG, "BEFORE: map values: " + entry.getKey() + ": " + entry.getValue().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(AmazonSharedPreferencesWrapper.MYM_USER_KEY, "");
                    editor.putString(AmazonSharedPreferencesWrapper.MYM_USER_GROUP, null);
                    editor.commit();

                    Map<String, ?> keysafter = sharedPref.getAll();

                    for (Map.Entry<String, ?> entry : keysafter.entrySet()) {
                        Log.d(TAG, "AFTER: map values: " + entry.getKey() + ": " + entry.getValue().toString());
                    }

                    String mymtoken = sharedPref.getString(Constants.MYM_KEY," ");
                    Log.d(TAG, "AddNewAccount Token is " + mymtoken);
                    getCognitoIdAndToken(mymtoken);
                    Log.d(TAG, "addNewAccount - Calling the imagecapactivity Capture Activity, with user="+sharedPref.getString(Constants.MYM_USER,""));
                    Intent launch_intent = new Intent(getApplicationContext(),LoaderActivity.class);
                    launch_intent.putExtra("activitytobecalled", "imagecapactivity");
                    launch_intent.putExtra("account", sharedPref.getString(Constants.MYM_USER,""));
                    launch_intent.putExtra("deviceid", mymClientGUID);
                    launch_intent.putExtra("appstartstate", appStartState);
                    startActivity(launch_intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }


    @Override
    public void onBackPressed()
    {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Snackbar.make(mainLinearLayout,getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();

        back_pressed = System.currentTimeMillis();
    }


    public void onButtonClick(View v) {
        if (v.getId() == R.id.buttonlog) {
            Log.d(TAG, "Calling method to add a new account");
            addNewAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE_FULL_ACCESS);
        }
    }


    public AppStart checkAppStart(Context context, SharedPreferences sharedPreferences) {
        PackageInfo pInfo;

        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            int lastVersionCode = sharedPreferences.getInt(LAST_APP_VERSION, -1);
            int currentVersionCode = pInfo.versionCode;
            Log.d(TAG,"checkAppStart: version from sharedPreferences="+lastVersionCode);
            Log.d(TAG,"checkAppStart: version from PackageInfo="+currentVersionCode);
            appStart = checkAppStart(currentVersionCode, lastVersionCode);
            // Update version in preferences
            sharedPreferences.edit().putInt(LAST_APP_VERSION, currentVersionCode).commit();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(MainActivity.TAG,
                    "Unable to determine current app version from package manager. Defensively assuming normal app start.");
        }
        return appStart;
    }


    public AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
        if (lastVersionCode == -1) {
            return AppStart.FIRST_TIME;
        } else if (lastVersionCode < currentVersionCode) {
            return AppStart.FIRST_TIME_VERSION;
        } else if (lastVersionCode > currentVersionCode) {
            Log.w(TAG, "AppStart: Current version code (" + currentVersionCode
                    + ") is less then the one recognized on last startup ("
                    + lastVersionCode
                    + "). Defensively assuming normal app start.");
            return AppStart.NORMAL;
        } else {
            return AppStart.NORMAL;
        }
    }

    // Requests app permissions
    public void permissionsRequest() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.INTERNET)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.LOCATION_HARDWARE)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.CAPTURE_VIDEO_OUTPUT)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.GET_ACCOUNTS)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET,
                                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                                Manifest.permission.CAMERA,
                                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                                Manifest.permission.LOCATION_HARDWARE,
                                                                Manifest.permission.CAPTURE_VIDEO_OUTPUT,
                                                                Manifest.permission.RECORD_AUDIO,
                                                                Manifest.permission.GET_ACCOUNTS},111);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 111: {
                if (grantResults.length > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED ) {
                    Log.d(TAG,"onRequestPermissionsResult: Permissions OK");
                } else {
                    permissionsNotSelected();
                }
            }
        }
    }

    private void permissionsNotSelected() {
        AlertDialog.Builder builder = new AlertDialog.Builder (this);
        builder.setTitle(getString(R.string.permrequired));
        builder.setMessage(getString(R.string.permmessage));
        builder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener () {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                System.exit(1);
            }
        });
    }

}
