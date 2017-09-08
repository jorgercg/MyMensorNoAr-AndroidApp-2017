package com.mymensor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.s3.AmazonS3;
import com.mymensor.cognitoclient.AmazonSharedPreferencesWrapper;
import com.mymensor.cognitoclient.CognitoSampleDeveloperAuthenticationService;

public class LoaderActivity extends Activity {
    private static final String TAG = "LoaderActvty";

    private String activityToBeCalled = null;
    private String mymensorAccount = null;
    private String mymensorUserGroup = null;
    private String origMymAcc;
    private String deviceId;

    private boolean finishApp = false;
    public boolean searchForServerEnded = false;
    public boolean isConnectedToServer = false;


    private boolean clockSetSuccess = false;
    private static long back_pressed;
    private int dciNumber = 1;

    private BackgroundLoader backgroundLoader;

    private long sntpReference;
    private long sntpTime;

    ImageView mymensorLogoTxt;
    ImageView mymensorLogoCircles;
    LinearLayout logoLinearLayout;

    Animation rotationCircles;

    SharedPreferences sharedPref;

    private String appStartState;

    private AmazonS3 s3Amazon;

    SharedPreferences amazonSharedPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityToBeCalled = getIntent().getExtras().get("activitytobecalled").toString();

        appStartState = getIntent().getExtras().get("appstartstate").toString();
        Log.d(TAG, "OnCreate: appStartState: " + appStartState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        s3Amazon = CognitoSyncClientManager.getInstance();

        // Creating AsyncTask
        backgroundLoader = new BackgroundLoader();

        Log.d(TAG, "onCreate(): calling checkConnectionToServer().");

        setContentView(R.layout.activity_loader);
        logoLinearLayout = (LinearLayout) findViewById(R.id.MyMensorLogoLinearLayout1);
        logoLinearLayout.setVisibility(View.VISIBLE);

        mymensorLogoTxt = (ImageView) findViewById(R.id.mymensor_logo_txt);
        mymensorLogoTxt.setVisibility(View.VISIBLE);

        mymensorLogoCircles = (ImageView) findViewById(R.id.mymensor_rot_logo);
        mymensorLogoCircles.setVisibility(View.VISIBLE);

        rotationCircles = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mymensorLogoCircles.startAnimation(rotationCircles);


    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): CALLED");
        startUpLoader();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume CALLED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy(): CALLED");
        backgroundLoader.cancel(true);
        Log.d(TAG, "onDestroy(): cancelled backgroundLoader = " + backgroundLoader.getStatus());
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Snackbar.make(logoLinearLayout, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
        back_pressed = System.currentTimeMillis();
    }


    private void startUpLoader() {
        // Retrieving SeaMensor Account information,
        mymensorAccount = getIntent().getExtras().get("account").toString();
        deviceId = getIntent().getExtras().get("deviceid").toString();

        Log.d(TAG, "startUpLoader: MyMensor Account from Mobile App: " + mymensorAccount);

        if (mymensorAccount.equalsIgnoreCase("")) {
            Snackbar.make(logoLinearLayout.getRootView(), getText(R.string.nomymensoraccount), Snackbar.LENGTH_LONG)
                    .setAction(getText(R.string.ok), null).show();
            Log.d(TAG, "Closing the app");
            finish();
            return;
        }

        amazonSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Log.d(TAG, "startUpLoader: Before COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
        Log.d(TAG, "startUpLoader: Before COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState);

        Long loopStart = System.currentTimeMillis();

        do {
            //nada!!!!
        }
        while (((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) || (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0)) && ((System.currentTimeMillis() - loopStart) < 3000));

        Log.d(TAG, "startUpLoader: After COG response: isApprovedByCognitoState=" + CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState);
        Log.d(TAG, "startUpLoader: After COG response: qtyClientsExceededState=" + CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState);

        if ((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 1) && (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2)) {
            Log.d(TAG, "startUpLoader: finishing");
            Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_mob_client_qty_exceeded), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();
            finish();
            return;
        } else {
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 2) ) {
                Log.d(TAG, "startUpLoader: finishing");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
                finish();
                return;
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 3) ) {
                Log.d(TAG, "startUpLoader: TRIAL EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_trial_exp_continuing_with_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
            if ((CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 4) ) {
                Log.d(TAG, "startUpLoader: SUB EXP continuing without server connection");
                Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.error_sub_exp_continuing_with_no_server_connection), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
        }

        // || ((CognitoSampleDeveloperAuthenticationService.qtyClientsExceededState == 0) && (CognitoSampleDeveloperAuthenticationService.isApprovedByCognitoState == 0))


        do {
            mymensorUserGroup = AmazonSharedPreferencesWrapper.getGroupForUser(amazonSharedPref);
        } while (mymensorUserGroup == null);

        Log.d(TAG, "startUpLoader: MYM_USR_GROUP: " + mymensorUserGroup);

        if (mymensorUserGroup.equalsIgnoreCase("mymARmobileapp")) {
            origMymAcc = mymensorAccount;
            mymensorAccount = mymensorAccount.substring(7, mymensorAccount.length());
        } else {
            origMymAcc = mymensorAccount;
        }


        Log.d(TAG, "startUpLoader: END");

        backgroundLoader.execute();

    }


    public class BackgroundLoader extends AsyncTask<Void, String, Void> {

        int qtyVps = 0;

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "backgroundLoader: onPreExecute()");
            clockSetSuccess = false;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            TextView message = (TextView) findViewById(R.id.bottom_message);
            message.setText(progress[0]);

        }

        @Override
        protected Void doInBackground(Void... params) {
            long now = 0;
            Long loopStart = System.currentTimeMillis();
            Log.d(TAG, "backgroundLoader: Calling SNTP");
            SntpClient sntpClient = new SntpClient();
            do {
                if (isCancelled()) {
                    Log.i("AsyncTask", "backgroundLoader: cancelled");
                    break;
                }
                if (sntpClient.requestTime("pool.ntp.org", 5000)) {
                    sntpTime = sntpClient.getNtpTime();
                    sntpReference = sntpClient.getNtpTimeReference();
                    now = sntpTime + SystemClock.elapsedRealtime() - sntpReference;
                    Log.i("SNTP", "SNTP Present Time =" + now);
                    Log.i("SNTP", "System Present Time =" + System.currentTimeMillis());
                    clockSetSuccess = true;
                }
                if (now != 0)
                    Log.d(TAG, "backgroundLoader: ntp:now=" + now);

            } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 5000));
            Log.d(TAG, "backgroundLoader: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);
            if (clockSetSuccess) {
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(clockSetSuccess, sntpTime, sntpReference));
            } else {
                sntpTime = 0;
                sntpReference = 0;
            }

            // Loading App Assets
            try {
                Log.d(TAG, "loadFinalDefinitions: backgroundLoader:####### LOADING: LOCAL ASSETS");
                MymUtils.extractAllAssets(getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "loadFinalDefinitions: backgroundLoader extractAllAssets failed:" + e.toString());
                publishProgress(getString(R.string.checkcfgfiles));
                finishApp = true;
                finish();
            }

            if (!finishApp) {
                publishProgress(getString(R.string.load_assets_finished));
            } else {
                publishProgress(getString(R.string.checkcfgfiles));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "onPostExecute CALLED: finishApp=" + finishApp);
            super.onPostExecute(result);
            if (finishApp) {
                MymUtils.showToastMessage(getApplicationContext(), getString(R.string.checkcfgfiles_online));
                finish();
            } else {
                callingActivities();
            }

        }
    }

    public void callingActivities() {

        Log.d(TAG, "callingActivities");
        Log.d(TAG, "callingActivities:####### LOADING: onPostExecute: callingARVewactivity: isTimeCertified=" + clockSetSuccess);
        Log.d(TAG, "callingActivities:####### LOADING: onPostExecute: callingARVewactivity: activityToBeCalled=" + activityToBeCalled);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.MYM_LAST_USER, origMymAcc);
        editor.commit();
        if (activityToBeCalled.equalsIgnoreCase("imagecapactivity")) {
            try {
                Intent intent = new Intent(getApplicationContext(), ImageCapActivity.class);
                intent.putExtra("mymensoraccount", mymensorAccount);
                intent.putExtra("origmymacc", origMymAcc);
                intent.putExtra("deviceid", deviceId);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", Constants.maxQtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("isTimeCertified", clockSetSuccess);
                startActivity(intent);
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            } finally {
                finish();
            }
        }

    }

}
