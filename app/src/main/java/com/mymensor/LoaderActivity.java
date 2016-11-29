package com.mymensor;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.mymensor.cognitoclient.AwsUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LoaderActivity extends Activity
{
    private static final String TAG = "LoaderActvty";

    private String activityToBeCalled = null;
    private String mymensorAccount = null;
    private String descvpRemotePath;
    private String vpsRemotePath;
    private String vpsCheckedRemotePath;
    private String markervpRemotePath;

    private boolean clockSetSuccess = false;
    private static long back_pressed;
    private int  dciNumber = 1;;

    private BackgroundLoader backgroundLoader;

    private long sntpReference;
    private long sntpTime;

    ImageView seamensorLogo;
    LinearLayout logoLinearLayout;
    FloatingActionButton fab;

    SharedPreferences sharedPref;

    private String appStartState;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        activityToBeCalled = getIntent().getExtras().get("activitytobecalled").toString();

        // Retrieving SeaMensor Account information,
        mymensorAccount = getIntent().getExtras().get("account").toString();;
        Log.d(TAG,"OnCreate: MyMensor Account: "+mymensorAccount);

        appStartState = getIntent().getExtras().get("appstartstate").toString();
        Log.d(TAG,"OnCreate: appStartState: "+appStartState);

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        // Creating AsyncTask
        backgroundLoader = new BackgroundLoader();

        setContentView(R.layout.activity_loader);
        logoLinearLayout = (LinearLayout)findViewById(R.id.SeaMensorLogoLinearLayout1);
        logoLinearLayout.setVisibility(View.VISIBLE);

        fab = (FloatingActionButton) findViewById(R.id.fab);

        seamensorLogo = (ImageView) findViewById(R.id.seamensor_logo);
        seamensorLogo.setVisibility(View.VISIBLE);


        if (appStartState.equalsIgnoreCase("firstever")){
            firstTimeLoader();
        }

        if (mymensorAccount.equalsIgnoreCase("")){
            Snackbar.make(logoLinearLayout.getRootView(), getText(R.string.nomymensoraccount), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.ok), null).show();
                    Log.d(TAG, "Closing the app");
                    finish();
        }

        backgroundLoader.execute();

        final View.OnClickListener undoOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityToBeCalled = "imagecapactivity";
                Snackbar.make(view, getText(R.string.loadingimgcapactvty), Snackbar.LENGTH_LONG).show();
                Log.d(TAG, "Reverting the call back to imagecapactivity");
            }
        };

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Snackbar.make(view, getText(R.string.loadingcfgactvty), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.undo), undoOnClickListener).show();
                    Log.d(TAG, "Changing the call to configactivity");
                    activityToBeCalled = "configactivity";
            }
        });

    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG,"onStart(): CALLED");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG,"onDestroy(): CALLED");
        backgroundLoader.cancel(true);
        Log.d(TAG,"onDestroy(): cancelled backgroundLoader = " + backgroundLoader.getStatus());
    }

    @Override
    public void onBackPressed()
    {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Snackbar.make(logoLinearLayout,getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
        back_pressed = System.currentTimeMillis();
    }

    private void firstTimeLoader(){
        try {
            File vpsFile = new File(getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
            File vpsCheckedFile = new File(getApplicationContext().getFilesDir(),Constants.vpsCheckedConfigFileName);
            File descvpFile0 = new File(getApplicationContext().getFilesDir(), "descvp0.png");
            File descvpFile1 = new File(getApplicationContext().getFilesDir(), "descvp1.png");
            File markervpFile0 = new File(getApplicationContext().getFilesDir(), "markervp0.png");
            File markervpFile1 = new File(getApplicationContext().getFilesDir(), "markervp1.png");

            if (!vpsFile.exists()) {
                ConfigFileCreator.createVpsfile(getApplicationContext(), getApplicationContext().getFilesDir(), Constants.vpsConfigFileName);
            }
            if (!vpsCheckedFile.exists()) {
                ConfigFileCreator.createVpsCheckedFile(getApplicationContext(), getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
            }
            if (!descvpFile0.exists()) {
                ConfigFileCreator.createDescvpFile(getApplicationContext(), getApplicationContext().getFilesDir(), "descvp0.png");
            }
            if (!descvpFile1.exists()) {
                ConfigFileCreator.createDescvpFile(getApplicationContext(), getApplicationContext().getFilesDir(), "descvp1.png");
            }
            if (!markervpFile0.exists()) {
                ConfigFileCreator.createMarkervpFile(getApplicationContext(), getApplicationContext().getFilesDir(), "markervp0.png");
            }
            if (!markervpFile1.exists()) {
                ConfigFileCreator.createMarkervpFile(getApplicationContext(), getApplicationContext().getFilesDir(), "markervp1.png");
            }

            Log.d(TAG,"firstTimeLoader: Waiting for initial config files and image to be created");

            Boolean configFilesOK = false;

            do {
                File vpsFileCHK = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);
                File vpsCheckedFileCHK = new File(getApplicationContext().getFilesDir(),Constants.vpsCheckedConfigFileName);
                configFilesOK = ((vpsFileCHK.exists())&&(vpsCheckedFileCHK.exists()));
            } while (!configFilesOK);

            Log.d(TAG,"firstTimeLoader: initial config files CREATION DONE: configFilesOK="+configFilesOK);

            Boolean imageFilesOK = false;

            do {
                File descvpFileCHK0 = new File(getApplicationContext().getFilesDir(),"descvp0.png");
                File descvpFileCHK1 = new File(getApplicationContext().getFilesDir(),"descvp1.png");
                imageFilesOK = ((descvpFileCHK0.exists())&&(descvpFileCHK1.exists()));
            } while (!imageFilesOK);

            Log.d(TAG,"firstTimeLoader: initial image files CREATION DONE: imageFilesOK="+imageFilesOK);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "firstTimeLoader(): failed to create all config files, see stack trace "+e.toString());
            finish();
        }
    }



    public class BackgroundLoader extends AsyncTask<Void, String, Void> {

        int qtyVps=0;

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

            } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 10000));
            Log.d(TAG, "backgroundLoader: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);
            if (clockSetSuccess) {
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(clockSetSuccess, sntpTime, sntpReference));
            } else {
                sntpTime = 0;
                sntpReference = 0;
            }

            /*
            *********************************************************************************************************************
             */
            Log.d(TAG, "loadConfiguration(): Loading Definitions from Remote Storage and writing to local storage");


            descvpRemotePath = mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "dsc" + "/";
            markervpRemotePath = mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "mrk" + "/";
            vpsRemotePath = mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";
            vpsCheckedRemotePath = mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";

            try {

                File vpsFile = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);

                if (MymUtils.isNewFileAvailable(  s3Client,
                        Constants.vpsConfigFileName,
                        (vpsRemotePath + Constants.vpsConfigFileName),
                        Constants.BUCKET_NAME,
                        getApplicationContext())) {
                    Log.d(TAG,"vpsFile isNewFileAvailable= TRUE");
                    TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (vpsRemotePath + Constants.vpsConfigFileName), Constants.BUCKET_NAME, vpsFile);
                    observer.setTransferListener(new TransferListener() {

                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state.equals(TransferState.COMPLETED)) {
                                Log.d(TAG,"vpsFile TransferListener="+state.toString());
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            if (bytesTotal>0){
                                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            }

                            //Display percentage transfered to user
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "loadConfiguration() Observer: vpsFile loading failed:"+ ex.toString());
                            publishProgress(getString(R.string.checkcfgfiles));
                            finish();
                        }

                    });
                } else {
                    Log.d(TAG,"vpsFile isNewFileAvailable= FALSE");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadConfiguration(): vpsFile loading failed:"+e.toString());
                publishProgress(getString(R.string.checkcfgfiles));
                finish();
            }


            /*
            *********************************************************************************************************************
            */


            try {

                final File vpsCheckedFile = new File(getApplicationContext().getFilesDir(),Constants.vpsCheckedConfigFileName);

                if (MymUtils.isNewFileAvailable(  s3Client,
                        Constants.vpsCheckedConfigFileName,
                        (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName),
                        Constants.BUCKET_NAME,
                        getApplicationContext())) {
                    Log.d(TAG,"vpsCheckedFile isNewFileAvailable= TRUE");
                    TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile);
                    observer.setTransferListener(new TransferListener() {

                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state.equals(TransferState.COMPLETED)) {
                                Log.d(TAG,"vpsCheckedFile TransferListener="+state.toString());
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            if (bytesTotal>0){
                                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            }

                            //Display percentage transfered to user
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.e(TAG, "loadConfiguration() Observer: vpsCheckedFile loading failed:"+vpsCheckedFile.getPath()+ ex.toString());
                            publishProgress(getString(R.string.checkcfgfiles));
                            finish();
                        }

                    });
                } else {
                    Log.d(TAG,"vpsCheckedFile isNewFileAvailable= FALSE");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadConfiguration(): vpsCheckedFile loading failed:"+e.toString());
                publishProgress(getString(R.string.checkcfgfiles));
                finish();
            }

           /*
            *********************************************************************************************************************
            */

            Boolean configFilesOK = false;

            do {
                File vpsFileCHK = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);
                File vpsCheckedFileCHK = new File(getApplicationContext().getFilesDir(),Constants.vpsCheckedConfigFileName);
                configFilesOK = ((vpsFileCHK.exists())&&(vpsCheckedFileCHK.exists()));
            } while (!configFilesOK);

            Log.d(TAG,"Loading Config Files: configFilesOK="+configFilesOK);

            publishProgress(getString(R.string.still_loading_assets));

            /*
            *********************************************************************************************************************
            */

            long loopstart = System.currentTimeMillis();

            do{
                try
                {
                    try
                    {
                        Log.d(TAG,"loadQtyVpsFromVpsFile: File="+Constants.vpsConfigFileName);
                        InputStream fis = MymUtils.getLocalFile(Constants.vpsConfigFileName, getApplicationContext());
                        XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                        XmlPullParser myparser = xmlFactoryObject.newPullParser();
                        myparser.setInput(fis, null);
                        int eventType = myparser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT)
                        {
                            if(eventType == XmlPullParser.START_DOCUMENT)
                            {
                                //Log.d(TAG,"Start document");
                            }
                            else if(eventType == XmlPullParser.START_TAG)
                            {
                                //Log.d(TAG,"Start tag "+myparser.getName());
                                if(myparser.getName().equalsIgnoreCase("QtyVps"))
                                {
                                    eventType = myparser.next();
                                    qtyVps = Short.parseShort(myparser.getText());
                                }
                            }
                            else if(eventType == XmlPullParser.END_TAG)
                            {
                                //Log.d(TAG,"End tag "+myparser.getName());
                            }
                            else if(eventType == XmlPullParser.TEXT)
                            {
                                //Log.d(TAG,"Text "+myparser.getText());
                            }
                            eventType = myparser.next();
                        }
                        fis.close();
                    }
                    finally
                    {
                        Log.d(TAG,"loadConfiguration(): loadQtyVpsFromVpsFile QtyVps="+qtyVps);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.e(TAG, "loadConfiguration(): loadQtyVpsFromVpsFile loading failed, see stack trace");
                }
            } while ((!(qtyVps>0))&&((System.currentTimeMillis()-loopstart)<10000));


            /*
            *********************************************************************************************************************
            */

            try
            {
                // Loading Vp Location Description Images from Remote Storage and writing to local storage.

                for (int j = 0; j < (qtyVps); j++)
                {
                    Log.d(TAG,"loadFinalDefinitions:####### LOADING: VPDESCFILES CONTENTS j="+j);
                    File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (j) + ".png");
                    Log.d(TAG,"loadFinalDefinitions: vpLocationDescImageFilePath Dropbox: " + descvpRemotePath+ "descvp" + (j) + ".png");
                    if (MymUtils.isNewFileAvailable(  s3Client,
                            ("descvp" + (j) + ".png"),
                            (descvpRemotePath+ "descvp" + (j) + ".png"),
                            Constants.BUCKET_NAME,
                            getApplicationContext())) {
                        Log.d(TAG,"descvpFile loadFinalDefinitions: isNewFileAvailable= TRUE");
                        final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (descvpRemotePath+ "descvp" + (j) + ".png"), Constants.BUCKET_NAME, descvpFile);
                        observer.setTransferListener(new TransferListener() {

                            @Override
                            public void onStateChanged(int id, TransferState state) {
                                if (state.equals(TransferState.COMPLETED)) {
                                    Log.d(TAG,"loadFinalDefinitions: TransferListener Descvp="+observer.getKey()+" State="+state.toString());
                                }
                            }

                            @Override
                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                if (bytesTotal>0){
                                    int percentage = (int) (bytesCurrent / bytesTotal * 100);

                                }

                                //Display percentage transfered to user
                            }

                            @Override
                            public void onError(int id, Exception ex) {
                                Log.e(TAG, "loadFinalDefinitions: Descvp loading failed, see stack trace:"+observer.getKey());
                                publishProgress(getString(R.string.checkcfgfiles));
                                finish();
                            }

                        });
                    } else {
                        Log.d(TAG,"descvpFile loadFinalDefinitions: "+ "descvp" + (j) + ".jpg"+" isNewFileAvailable= FALSE");
                    }
                }
                publishProgress(getString(R.string.almost_finished_loading_assets));
                // Loading App Assets
                try
                {
                    Log.d(TAG,"loadFinalDefinitions: backgroundLoader:####### LOADING: LOCAL ASSETS");
                    MymUtils.extractAllAssets(getApplicationContext());
                }
                catch (Exception e)
                {
                    Log.e(TAG, "loadFinalDefinitions:AssetManager extractAllAssets failed:"+e.toString());
                    publishProgress(getString(R.string.checkcfgfiles));
                    finish();
                }


            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "loadFinalDefinitions:Error loadFinalDefinitions()");
                publishProgress(getString(R.string.checkcfgfiles));
                finish();
            }




            /*
            *********************************************************************************************************************
            */

            try
            {
                // Loading Vp Marker Images from Remote Storage and writing to local storage.

                for (int j = 0; j < (qtyVps); j++)
                {
                    Log.d(TAG,"loadFinalDefinitions:####### LOADING: MARKERVP CONTENTS j="+j);
                    File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (j) + ".png");
                    Log.d(TAG,"loadFinalDefinitions: markervpRemotePath: " + markervpRemotePath+ "markervp" + (j) + ".png");
                    if (MymUtils.isNewFileAvailable(  s3Client,
                            ("markervp" + (j) + ".png"),
                            (markervpRemotePath+ "markervp" + (j) + ".png"),
                            Constants.BUCKET_NAME,
                            getApplicationContext())) {
                        Log.d(TAG,"markervpFile loadFinalDefinitions: isNewFileAvailable= TRUE");
                        final TransferObserver observer = MymUtils.getRemoteFile(transferUtility, (markervpRemotePath+ "markervp" + (j) + ".png"), Constants.BUCKET_NAME, markervpFile);
                        observer.setTransferListener(new TransferListener() {

                            @Override
                            public void onStateChanged(int id, TransferState state) {
                                if (state.equals(TransferState.COMPLETED)) {
                                    Log.d(TAG,"loadFinalDefinitions: TransferListener Markervp="+observer.getKey()+" State="+state.toString());
                                }
                            }

                            @Override
                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                if (bytesTotal>0){
                                    int percentage = (int) (bytesCurrent / bytesTotal * 100);

                                }

                                //Display percentage transfered to user
                            }

                            @Override
                            public void onError(int id, Exception ex) {
                                Log.e(TAG, "loadFinalDefinitions: Markervp loading failed, see stack trace:"+observer.getKey());
                                publishProgress(getString(R.string.checkcfgfiles));
                                finish();
                            }

                        });
                    } else {
                        Log.d(TAG,"markervpFile loadFinalDefinitions: "+ "markervp" + (j) + ".jpg"+" isNewFileAvailable= FALSE");
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "markervpFile loading:Error: "+e.toString());
                publishProgress(getString(R.string.checkcfgfiles));
                finish();
            }

            // Checking if all images are already in the local storage, as network operations take place in background.

            int product = 0;

            do {
                for (int k = 0; k < (qtyVps); k++){
                    try{
                        File descvpFileCHK = new File(getApplicationContext().getFilesDir(), "descvp" + (k) + ".png");
                        File markervpFileCHK = new File(getApplicationContext().getFilesDir(), "markervp" + (k) + ".png");
                        if (descvpFileCHK.exists() && markervpFileCHK.exists()) {
                            if (k == 0) {
                                product = Math.abs(1);
                            } else {
                                product *= Math.abs(1);
                            }
                        } else {
                            if (k == 0) {
                                product = Math.abs(0);
                            } else {
                                product *= Math.abs(0);
                            }
                        }
                    } catch (Exception e) {
                        Log.e (TAG, "Image Files Checking Failed:"+e.toString());
                    }

                }

            } while (product==0);

            Log.d(TAG,"Loading Image Config Files: imageFilesOK="+product);
            publishProgress(getString(R.string.load_assets_finished));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            callingActivities(qtyVps);
        }
    }

    public void callingActivities(int qtyVps){

        Log.d(TAG,"callingActivities");
        Log.d(TAG,"callingActivities:####### LOADING: onPostExecute: callingARVewactivity: isTimeCertified=" + clockSetSuccess);
        Log.d(TAG,"callingActivities:####### LOADING: onPostExecute: callingARVewactivity: activityToBeCalled="+activityToBeCalled);
        TextView message = (TextView) findViewById(R.id.bottom_message);
        if (activityToBeCalled.equalsIgnoreCase("configactivity"))
        {
            try
            {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                intent.putExtra("mymensoraccount", mymensorAccount);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", qtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("isTimeCertified", clockSetSuccess);
                startActivity(intent);
            }
            catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
            finally
            {
                finish();
            }
        }
        if (activityToBeCalled.equalsIgnoreCase("imagecapactivity"))
        {
            try
            {
                Intent intent = new Intent(getApplicationContext(), ImageCapActivity.class);
                intent.putExtra("mymensoraccount", mymensorAccount);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", qtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("isTimeCertified", clockSetSuccess);
                startActivity(intent);
            }
            catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
            finally
            {
                finish();
            }
        }

    }

}
