package com.mymensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mymensor.cognitoclient.AwsUtil;
import com.mymensor.filters.ARFilter;
import com.mymensor.filters.Filter;
import com.mymensor.filters.IdMarkerDetectionFilter;
import com.mymensor.filters.ImageDetectionFilter;
import com.mymensor.filters.NoneARFilter;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgcodecs.Imgcodecs;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import static java.nio.charset.StandardCharsets.UTF_8;

public class ImageCapActivity extends Activity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        AdapterView.OnItemClickListener {

    private static final String TAG = "ImageCapActvty";

    private static long back_pressed;

    private String mymensorAccount;
    private int dciNumber;
    private short qtyVps = 0;

    private String descvpRemotePath;
    private String vpsRemotePath;
    private String vpsCheckedRemotePath;
    private String capRemotePath;

    private static Bitmap vpLocationDescImageFileContents;
    private static Bitmap selectedVpPhotoImageFileContents;

    private short[] vpNumber;
    private boolean[] vpChecked;
    private boolean[] vpFlashTorchIsOn;
    private long[] photoTakenTimeMillis;
    private long[] vpNextCaptureMillis;
    private String[] vpLocationDesText;

    private boolean[] vpIsAmbiguous;
    private boolean[] vpIsSuperSingle;
    private boolean[] vpSuperIdIs20mm;
    private boolean[] vpSuperIdIs100mm;
    private int[] vpSuperMarkerId;

    private boolean inPosition = false;
    private boolean inRotation = false;
    private boolean isTracking = false;
    private boolean isShowingVpPhoto = false;
    private int isHudOn = 1;

    private boolean vpIsDisambiguated = true;                   //TODO
    private boolean doubleCheckingProcedureFinalized = true;    //TODO

    private boolean vpIsManuallySelected=false;

    private TrackingValues trackingValues;
    private int vpTrackedInPose;

    public boolean vpPhotoAccepted = false;
    public boolean vpPhotoRejected = false;
    public boolean lastVpPhotoRejected = false;
    public int lastVpSelectedByUser;
    public int photoSelected = 0;

    private int[] vpXCameraDistance;
    private int[] vpYCameraDistance;
    private int[] vpZCameraDistance;
    private int[] vpXCameraRotation;
    private int[] vpYCameraRotation;
    private int[] vpZCameraRotation;
    private String[] vpFrequencyUnit;
    private long[] vpFrequencyValue;

    private static float tolerancePosition;
    private static float toleranceRotation;

    private boolean waitingToCaptureVpAfterDisambiguationProcedureSuccessful = true; //TODO
    private boolean doubleCheckingProcedureStarted = false; //TODO
    private boolean resultSpecialTrk = false; //TODO
    private boolean singleImageTrackingIsSet = false;
    private boolean waitingUntilSingleImageTrackingIsSet  = false;
    private boolean multipleImageTrackingIsSet = false;
    private boolean waitingUntilMultipleImageTrackingIsSet = false;
    private boolean idTrackingIsSet = false;
    private boolean waitingUntilIdTrackingIsSet = false;

    private short assetId;
    private String frequencyUnit;
    private int frequencyValue;

    ListView vpsListView;
    ImageView radarScanImageView;
    ImageView mProgress;
    TouchImageView imageView;
    ImageView vpCheckedView;
    TextView isVpPhotoOkTextView;

    TextView vpLocationDesTextView;
    TextView vpIdNumber;

    Animation rotationRadarScan;
    Animation rotationMProgress;

    FloatingActionButton alphaToggleButton;
    FloatingActionButton showVpCapturesButton;
    Button showPreviousVpCaptureButton;
    Button showNextVpCaptureButton;
    Button acceptVpPhotoButton;
    Button rejectVpPhotoButton;

    LinearLayout arSwitchLinearLayout;

    Switch arSwitch;

    private boolean isArSwitchOn = true;

    FloatingActionButton positionCertifiedButton;
    FloatingActionButton timeCertifiedButton;
    FloatingActionButton connectedToServerButton;
    FloatingActionButton cameraShutterButton;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean isTimeCertified;

    private boolean askForManualPhoto = false;


    public boolean isPositionCertified = false; // Or true ???????????
    public boolean isConnectedToServer = false; // Or true ???????????

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;
    public List<Mat> markerBuffer;

    // Whether the next camera frame should be saved as a photo.
    private boolean vpPhotoRequestInProgress;

    // The filters.
    private ARFilter[] mImageDetectionFilters;

    // The indices of the active filters.
    private int mImageDetectionFilterIndex;

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX =
            "imageDetectionFilterIndex";

    // Matrix to hold camera calibration
    // initially with absolute compute values
    private MatOfDouble mCameraMatrix;

    private float x1 = 0;
    private float x2 = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_imagecap);

        // Retrieve SeaMensor configuration info
        mymensorAccount = getIntent().getExtras().get("mymensoraccount").toString();
        dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        isTimeCertified = Boolean.parseBoolean(getIntent().getExtras().get("isTimeCertified").toString());

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        descvpRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/"+"descvp";
        vpsRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";
        vpsCheckedRemotePath = mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";
        capRemotePath = mymensorAccount+"/"+"cap"+"/";

        if (savedInstanceState != null) {
            mImageDetectionFilterIndex = savedInstanceState.getInt(
                    STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mImageDetectionFilterIndex = 0;
        }

        final Camera camera;
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);
        camera = Camera.open(0);

        final Parameters parameters = camera.getParameters();
        camera.release();

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.imagecap_javaCameraView);
        mCameraView.setCameraIndex(0);
        mCameraView.setMaxFrameSize(Constants.cameraWidthInPixels, Constants.cameraHeigthInPixels);
        mCameraView.setCvCameraViewListener(this);

        loadConfigurationFile();
        loadVpsChecked();
        verifyVpsChecked();

        trackingValues = new TrackingValues();

        mImageDetectionFilterIndex = 1;

        String[] newVpsList = new String[qtyVps];
        for (int i=0; i<(qtyVps); i++)
        {
            if (i==0){
                newVpsList[0] = getString(R.string.vp_00);
            } else {
                newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
            }
        }
        vpsListView = (ListView) this.findViewById(R.id.vp_list);
        vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
        vpsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        vpsListView.setOnItemClickListener(this);
        vpsListView.setVisibility(View.VISIBLE);


        vpLocationDesTextView = (TextView) this.findViewById(R.id.textView1);
        vpIdNumber = (TextView) this.findViewById(R.id.textView2);


        showPreviousVpCaptureButton = (Button) this.findViewById(R.id.buttonShowPreviousVpCapture);
        showNextVpCaptureButton = (Button) this.findViewById(R.id.buttonShowNextVpCapture);
        acceptVpPhotoButton = (Button) this.findViewById(R.id.buttonAcceptVpPhoto);
        rejectVpPhotoButton = (Button) this.findViewById(R.id.buttonRejectVpPhoto);

        isVpPhotoOkTextView = (TextView) this.findViewById(R.id.textViewIsPhotoOK);

        radarScanImageView = (ImageView) this.findViewById(R.id.imageViewRadarScan);
        rotationRadarScan = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        radarScanImageView.setVisibility(View.VISIBLE);
        radarScanImageView.startAnimation(rotationRadarScan);

        mProgress = (ImageView) this.findViewById(R.id.waitingTrkLoading);
        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        vpCheckedView = (ImageView) this.findViewById(R.id.imageViewVpChecked);
        vpCheckedView.setVisibility(View.GONE);

        arSwitchLinearLayout = (LinearLayout) this.findViewById(R.id.arSwitchLinearLayout);

        arSwitch = (Switch) findViewById(R.id.arSwitch);

        arSwitch.setChecked(true);

        arSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    isArSwitchOn = true;
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    mImageDetectionFilterIndex=1;
                    Snackbar.make(arSwitch.getRootView(),getText(R.string.arswitchison), Snackbar.LENGTH_LONG).show();
                } else {
                    isArSwitchOn = false;
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    mImageDetectionFilterIndex=0;
                    askForManualPhoto = false;
                    vpIsManuallySelected = false;
                    Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchisoff), Snackbar.LENGTH_LONG).show();
                }
                Log.d(TAG, "isArSwitchOn="+ isArSwitchOn);
            }
        });


        cameraShutterButton = (FloatingActionButton) findViewById(R.id.cameraShutterButton);

        positionCertifiedButton = (FloatingActionButton) findViewById(R.id.positionCertifiedButton);
        timeCertifiedButton = (FloatingActionButton) findViewById(R.id.timeCertifiedButton);
        connectedToServerButton = (FloatingActionButton) findViewById(R.id.connectedToServerButton);

        alphaToggleButton = (FloatingActionButton) findViewById(R.id.buttonAlphaToggle);
        showVpCapturesButton = (FloatingActionButton) findViewById(R.id.buttonShowVpCaptures);


        // Camera Shutter Button

        cameraShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Camera Button clicked!!!");
                askForManualPhoto = true;

            }
        });

        // Position Certified Button

        final View.OnClickListener undoOnClickListenerPositionButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar.make(view, getText(R.string.loadingimgcapactvty), Snackbar.LENGTH_LONG).show();

            }
        };

        positionCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.loadingcfgactvty), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.undo), undoOnClickListenerPositionButton).show();

            }
        });

        // Time Certified Button
        if (isTimeCertified) {
            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        } else {
            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        }


        final View.OnClickListener actionOnClickListenerTimeButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callTimeServerInBackground();
                Snackbar.make(view, getText(R.string.tryingtocertifytime), Snackbar.LENGTH_LONG).show();
            }
        };

        timeCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTimeCertified){
                    Snackbar.make(view, getText(R.string.usingcerttimeistrue), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            })
                            .show();
                } else {
                    Snackbar.make(view, getText(R.string.usingcerttimeisfalse), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.certify), actionOnClickListenerTimeButton).show();
                }


            }
        });

        // Connected to Server Button

        final View.OnClickListener undoOnClickListenerServerButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar.make(view, getText(R.string.loadingimgcapactvty), Snackbar.LENGTH_LONG).show();

            }
        };

        connectedToServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.loadingcfgactvty), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.undo), undoOnClickListenerServerButton).show();

            }
        });

        // Alpha Channel Toggle Button

        alphaToggleButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 Log.d(TAG, "Toggling imageView Transparency");
                 if (imageView.getImageAlpha()==128)
                 {
                     imageView.setImageAlpha(255);
                 }
                 else
                 {
                     imageView.setImageAlpha(128);
                 }
                 if (imageView.getImageAlpha()==128) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                 if (!(imageView.getImageAlpha()==128)) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
             }
        });

        // Show VP captures galley Button

        showVpCapturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alphaToggleButton.setVisibility(View.GONE);
                showVpCapturesButton.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
                showNextVpCaptureButton.setVisibility(View.VISIBLE);
                imageView.resetZoom();
                if (imageView.getImageAlpha()==128)
                {
                    imageView.setImageAlpha(255);
                }
                photoSelected = -1;
                showVpCaptures(lastVpSelectedByUser);
            }
        });

    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current filter indices.
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onStart()
    {
        super.onStart();


    }

    @Override
    public void onBackPressed()
    {
        if (isShowingVpPhoto){
            Log.d(TAG, "Closing VPx location photo");
            //Turning tracking On
            mImageDetectionFilterIndex=1;
            isShowingVpPhoto = false;
            vpLocationDesTextView.setVisibility(View.GONE);
            vpIdNumber.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            alphaToggleButton.setVisibility(View.GONE);
            showPreviousVpCaptureButton.setVisibility(View.GONE);
            showNextVpCaptureButton.setVisibility(View.GONE);
            showVpCapturesButton.setVisibility(View.GONE);
            vpsListView.setVisibility(View.VISIBLE);
            // TURNING ON RADAR SCAN
            radarScanImageView.setVisibility(View.VISIBLE);
            radarScanImageView.startAnimation(rotationRadarScan);
            // Turning on control buttons
            arSwitchLinearLayout.setVisibility(View.VISIBLE);
            arSwitch.setVisibility(View.VISIBLE);
            positionCertifiedButton.setVisibility(View.VISIBLE);
            timeCertifiedButton.setVisibility(View.VISIBLE);
            connectedToServerButton.setVisibility(View.VISIBLE);
            imageView.setOnTouchListener(null);
        } else {
            if (back_pressed + 2000 > System.currentTimeMillis())
            {
                super.onBackPressed();
            }
            else
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Snackbar.make(mCameraView,getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
                    }
                });
            back_pressed = System.currentTimeMillis();
        }

    }


    @Override
    public void recreate() {
            super.recreate();
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG,"onResume CALLED");
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            Log.d("ERROR", "Unable to load OpenCV");
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        //if (mGoogleApiClient.isConnected()) startLocationUpdates();
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG,"onRestart CALLED");
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"onPause CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
        //stopLocationUpdates();

    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG,"onStop CALLED");
        saveVpsChecked();
        //mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG,"onDestroy CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        // Dispose of native resources.
        disposeFilters(mImageDetectionFilters);
        super.onDestroy();
    }

    private void disposeFilters(Filter[] filters) {
        if (filters!=null) {
            for (Filter filter : filters) {
                filter.dispose();
            }
        }
    }


    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    //TODO: Fix this
                    mCameraMatrix = MymUtils.getCameraMatrix(Constants.cameraWidthInPixels, Constants.cameraHeigthInPixels);
                    mCameraView.enableView();
                    //mCameraView.enableFpsMeter();

                    if (!waitingUntilMultipleImageTrackingIsSet) {
                        setMultipleImageTrackingConfiguration();
                    }

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void setSingleImageTrackingConfiguration(int vpIndex)
    {
        waitingUntilSingleImageTrackingIsSet = true;
        markerBuffer = new ArrayList<Mat>();
        try
        {
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (vpIndex) + ".png");
            Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            markerBuffer.add(tmpMarker);
        }
        catch (Exception e)
        {
            Log.e(TAG, "setSingleImageTrackingConfiguration(int vpIndex): markerImageFileContents failed:"+e.toString());
        }
        ARFilter trackFilter = null;
        try {
            trackFilter = new ImageDetectionFilter(
                ImageCapActivity.this,
                markerBuffer.toArray(),
                1,
                mCameraMatrix,
                Constants.standardMarkerlessMarkerWidth);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: "+e.toString());
        }
        if (trackFilter!=null) {
            mImageDetectionFilters = new ARFilter[]{
                    new NoneARFilter(),
                    trackFilter
            };
            singleImageTrackingIsSet = true;
            waitingUntilSingleImageTrackingIsSet = false;
            multipleImageTrackingIsSet = false;
        }
    }


    private void setMultipleImageTrackingConfiguration(){

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected void onPreExecute(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        waitingUntilMultipleImageTrackingIsSet = true;
                        Log.d(TAG, "BEFORE STARTING setMultipleImageTrackingConfiguration IN BACKGROUND - Lighting Waiting Circle");
                        Log.d(TAG,"waitingUntilMultipleImageTrackingIsSet="+ waitingUntilMultipleImageTrackingIsSet);
                        Log.d(TAG,"multipleImageTrackingIsSet="+multipleImageTrackingIsSet);
                        Log.d(TAG,"waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet);
                        Log.d(TAG,"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                        Log.d(TAG,"isTracking="+isTracking);
                        Log.d(TAG,"isHudOn="+isHudOn);
                        mProgress.setVisibility(View.VISIBLE);
                        mProgress.startAnimation(rotationMProgress);
                    }
                });
                isTracking = false;
                isHudOn = 1;
            }

            @Override
            protected Void doInBackground(Void... params){
                //mBgr = new Mat();
                markerBuffer = new ArrayList<Mat>();
                for (int i=1; i<(qtyVps); i++ ){
                    try
                    {
                        File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + i + ".png");
                        Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                        markerBuffer.add(tmpMarker);
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "setMultipleImageTrackingConfiguration(): markerImageFileContents failed:"+e.toString());
                    }
                }
                ARFilter trackFilter = null;
                try {
                    trackFilter = new ImageDetectionFilter(
                            ImageCapActivity.this,
                            markerBuffer.toArray(),
                            (qtyVps-1),
                            mCameraMatrix,
                            Constants.standardMarkerlessMarkerWidth);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load marker: "+e.toString());
                }
                if (trackFilter!=null){
                    mImageDetectionFilters = new ARFilter[] {
                            new NoneARFilter(),
                            trackFilter
                    };
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND - Turning off Waiting Circle");
                        mProgress.clearAnimation();
                        mProgress.setVisibility(View.GONE);
                        Log.d(TAG, "FINISHING setMultipleImageTrackingConfiguration IN BACKGROUND - mProgress.isShown():" + mProgress.isShown());
                        // TURNING OFF TARGET
                        //targetImageView.setImageDrawable(drawableTargetWhite);
                        //targetImageView.setVisibility(View.GONE);
                        // TURNING ON RADAR SCAN
                        if ((!radarScanImageView.isShown())&&(isArSwitchOn)){
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);
                        }
                        mImageDetectionFilterIndex = 1;
                        waitingUntilMultipleImageTrackingIsSet = false;
                        singleImageTrackingIsSet = false;
                        multipleImageTrackingIsSet = true;

                    }
                });
                Log.d(TAG,"waitingUntilMultipleImageTrackingIsSet="+ waitingUntilMultipleImageTrackingIsSet);
                Log.d(TAG,"multipleImageTrackingIsSet="+multipleImageTrackingIsSet);
                Log.d(TAG,"waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet);
                Log.d(TAG,"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                Log.d(TAG,"isTracking="+isTracking);
                Log.d(TAG,"isHudOn="+isHudOn);
            }
        }.execute();
    }


    private void setIdTrackingConfiguration(){
        waitingUntilIdTrackingIsSet = true;
        ARFilter trackFilter = null;
        try {
            trackFilter = new IdMarkerDetectionFilter(
                    ImageCapActivity.this,
                    (qtyVps-1),
                    mCameraMatrix,
                    Constants.idMarkerStdSize);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: "+e.toString());
        }
        if (trackFilter!=null){
            mImageDetectionFilters = new ARFilter[] {
                    new NoneARFilter(),
                    trackFilter
            };
            idTrackingIsSet = true;
            waitingUntilIdTrackingIsSet = false;
        }
    }


    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }


    @Override
    public void onCameraViewStopped() {
    }


    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();
        verifyVpsChecked();
        if (!isArSwitchOn){
            if (!vpIsManuallySelected) vpTrackedInPose = 0;
            final int tmpvpfree = vpTrackedInPose;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (radarScanImageView.isShown()){
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                    int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    if (tmpvpfree<firstVisiblePosition || tmpvpfree>lastVisiblePosition){
                        vpsListView.smoothScrollToPosition(tmpvpfree);
                        firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                        lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    }
                    int k = firstVisiblePosition - 1;
                    int i = -1;
                    do {
                        k++;
                        i++;
                        if (k==tmpvpfree){
                            vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255,0,175,239));
                        } else {
                            vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                        }
                    } while (k<lastVisiblePosition);
                }
            });
        }
        if ((!isArSwitchOn)&&(askForManualPhoto)) {
            Log.d(TAG,"Requesting manual photo");
            takePhoto(rgba);
        }
        // Apply the active filters.
        if ((mImageDetectionFilters != null)&&(isArSwitchOn)) {
            //Log.d(TAG,"isTracking="+isTracking);
            //TODO Introduce measures to avoid an endless tracking with no detection in special cases.
            if (!isTracking){
                if (!singleImageTrackingIsSet){
                    mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, isHudOn, 0);
                    if (mImageDetectionFilters[mImageDetectionFilterIndex].getPose()!=null){
                        trackingValues = trackingValues.setTrackingValues(mImageDetectionFilters[mImageDetectionFilterIndex].getPose());
                        vpTrackedInPose = trackingValues.getVpNumberTrackedInPose();
                        if ((vpTrackedInPose>0)&&(vpTrackedInPose<(qtyVps+1))) {
                            isTracking = true;
                        } else {
                            isTracking = false;
                        }
                    } else {
                        isTracking = false;
                    }
                    if (isTracking){
                        Log.d(TAG,"trckValues: VP=" + vpTrackedInPose+" | "
                                + "Translations = " +trackingValues.getX()+" | "+trackingValues.getY()+" | "+trackingValues.getZ()+" | "
                                + "Rotations = "    +trackingValues.getEAX()+" | "+trackingValues.getEAX()+" | "+trackingValues.getEAX());
                        final int tmpvp = vpTrackedInPose;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // TURNING OFF RADAR SCAN
                                radarScanImageView.clearAnimation();
                                radarScanImageView.setVisibility(View.GONE);
                                int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                                if (tmpvp<firstVisiblePosition || tmpvp>lastVisiblePosition){
                                    vpsListView.smoothScrollToPosition(tmpvp);
                                    firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                    lastVisiblePosition = vpsListView.getLastVisiblePosition();
                                }
                                int k = firstVisiblePosition - 1;
                                int i = -1;
                                do {
                                    k++;
                                    i++;
                                    if (k==tmpvp){
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255,0,175,239));
                                    } else {
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                    }
                                } while (k<lastVisiblePosition);
                            }
                        });
                        if ((!vpIsAmbiguous[vpTrackedInPose]) || ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated)) || (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)){
                            if (!vpChecked[vpTrackedInPose]){
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                                    }
                                });
                                if (!singleImageTrackingIsSet) {
                                    if (!waitingUntilSingleImageTrackingIsSet){
                                        setSingleImageTrackingConfiguration(vpTrackedInPose);
                                    }

                                }

                            } else {
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        vpCheckedView.setVisibility(View.VISIBLE);
                                    }
                                });
                                isTracking = false;
                            }
                        }
                    } else {
                        //Log.d(TAG,"TRKSRVEY: NOT Tracking from the !isTracking Part");
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if ((!radarScanImageView.isShown())&&(!isShowingVpPhoto)){
                                    // TURNING ON RADAR SCAN
                                    radarScanImageView.setVisibility(View.VISIBLE);
                                    radarScanImageView.startAnimation(rotationRadarScan);
                                }
                                if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                                for (int i=0; i<(qtyVps); i++)
                                {
                                    if (vpsListView.getChildAt(i)!=null)
                                    {
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                    }

                                }
                            }
                        });
                    }
                }
            } else {
                if ((singleImageTrackingIsSet)&&((vpTrackedInPose>0)&&(vpTrackedInPose<(qtyVps+1)))) {
                    mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, isHudOn, vpTrackedInPose);
                    if (mImageDetectionFilters[mImageDetectionFilterIndex].getPose()!=null){
                        trackingValues = trackingValues.setTrackingValues(mImageDetectionFilters[mImageDetectionFilterIndex].getPose());
                        vpTrackedInPose = trackingValues.getVpNumberTrackedInPose();
                        if ((vpTrackedInPose>0)&&(vpTrackedInPose<(qtyVps+1))) {
                            isTracking = true;
                        } else {
                            isTracking = false;
                        }
                    } else {
                        isTracking = false;
                    }
                    if (isTracking){
                        Log.d(TAG,"trckValues: VP=" + vpTrackedInPose+" | "
                                + "Translations = " +trackingValues.getX()+" | "+trackingValues.getY()+" | "+trackingValues.getZ()+" | "
                                + "Rotations = "    +trackingValues.getEAX()+" | "+trackingValues.getEAX()+" | "+trackingValues.getEAX());
                        final int tmpvp = vpTrackedInPose;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // TURNING OFF RADAR SCAN
                                radarScanImageView.clearAnimation();
                                radarScanImageView.setVisibility(View.GONE);
                                int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                                if (tmpvp<firstVisiblePosition || tmpvp>lastVisiblePosition){
                                    vpsListView.smoothScrollToPosition(tmpvp);
                                    firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                    lastVisiblePosition = vpsListView.getLastVisiblePosition();
                                }
                                int k = firstVisiblePosition - 1;
                                int i = -1;
                                do {
                                    k++;
                                    i++;
                                    if (k==tmpvp){
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255,0,175,239));
                                    } else {
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                    }
                                } while (k<lastVisiblePosition);
                            }
                        });
                        if ((!vpIsAmbiguous[vpTrackedInPose]) || ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated)) || (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)){
                            if (!vpChecked[vpTrackedInPose]){
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                                    }
                                });
                                if (!waitingUntilMultipleImageTrackingIsSet) checkPositionToTarget(trackingValues, rgba);

                            } else {
                                runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        vpCheckedView.setVisibility(View.VISIBLE);
                                    }
                                });
                                isTracking = false;
                            }
                        }
                    } else {
                        Log.d(TAG,"TRKSRVEY: NOT Tracking from the singleImageTrackingIsSet: waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet+"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if ((!radarScanImageView.isShown())&&(!isShowingVpPhoto)){
                                    // TURNING ON RADAR SCAN
                                    radarScanImageView.setVisibility(View.VISIBLE);
                                    radarScanImageView.startAnimation(rotationRadarScan);
                                }
                                if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                                for (int i=0; i<(qtyVps); i++)
                                {
                                    if (vpsListView.getChildAt(i)!=null)
                                    {
                                        vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                    }

                                }
                            }
                        });
                        if ((!waitingUntilMultipleImageTrackingIsSet)&&(!waitingUntilSingleImageTrackingIsSet)) {
                            setMultipleImageTrackingConfiguration();
                        }
                    }
                } else {
                    Log.d(TAG,"We fell into a hole.... ");
                    Log.d(TAG,"waitingUntilMultipleImageTrackingIsSet="+ waitingUntilMultipleImageTrackingIsSet);
                    Log.d(TAG,"waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet);
                    Log.d(TAG,"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                    Log.d(TAG,"isTracking="+isTracking);
                    Log.d(TAG,"isHudOn="+isHudOn);

                }

            }


        }
        return rgba;
    }

    private void checkPositionToTarget(TrackingValues trackingValues, final Mat rgba) {


        if (!vpIsSuperSingle[vpTrackedInPose]) {
            inPosition = ((Math.abs(trackingValues.getX() - 0) <= tolerancePosition) &&
                    (Math.abs(trackingValues.getY() - 0) <= tolerancePosition) &&
                    (Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]) <= tolerancePosition));
            inRotation = ((Math.abs(trackingValues.getEAX() - 0) <= toleranceRotation) &&
                    (Math.abs(trackingValues.getEAY() - 0) <= toleranceRotation) &&
                    (Math.abs(trackingValues.getEAZ() - 0) <= toleranceRotation));
        } else {

            inPosition = ((Math.abs(trackingValues.getX() - vpXCameraDistance[vpTrackedInPose]) <= (tolerancePosition)) &&
                    (Math.abs(trackingValues.getY() - vpYCameraDistance[vpTrackedInPose]) <= (tolerancePosition)) &&
                    (Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]) <= (tolerancePosition)));
            inRotation = ((Math.abs(trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose]) <= (toleranceRotation)) &&
                    (Math.abs(trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose]) <= (toleranceRotation)) &&
                    (Math.abs(trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose]) <= (toleranceRotation)));
        }

        Log.d(TAG,"native inPosition="+inPosition+" inRotation="+inRotation+" waitingForMark...="+ waitingUntilMultipleImageTrackingIsSet +" vpPhReqInPress="+vpPhotoRequestInProgress);
        if ((inPosition) && (inRotation) && (!waitingUntilMultipleImageTrackingIsSet) && (!vpPhotoRequestInProgress)) {
            if ((vpIsAmbiguous[vpTrackedInPose])&&(!doubleCheckingProcedureFinalized)) {
                //TODO
                setIdTrackingConfiguration();
                doubleCheckingProcedureStarted = true;
            }
            if ((!vpIsAmbiguous[vpTrackedInPose]) || ((vpIsAmbiguous[vpTrackedInPose])&&(vpIsDisambiguated)&&(doubleCheckingProcedureFinalized))) {
                    if (!waitingUntilMultipleImageTrackingIsSet) {
                        if (isHudOn==1) {
                            isHudOn = 0;
                        } else {
                            takePhoto(rgba);
                        }
                    }
            }
        }

    }

    private void takePhoto (Mat rgba){
        Bitmap bitmapImage = null;

        long momentoLong = MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference);
        photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
        if (askForManualPhoto) askForManualPhoto = false;
        String momento = String.valueOf(momentoLong);
        String pictureFileName;
        pictureFileName = "cap_"+mymensorAccount+"_"+vpNumber[vpTrackedInPose]+"_"+momento+".jpg";
        File pictureFile = new File(getApplicationContext().getFilesDir(), pictureFileName);

        Log.d(TAG, "takePhoto: a new camera frame image is delivered " + momento);
        if (isArSwitchOn) {
            if ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated))
                waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
            if (doubleCheckingProcedureFinalized) {
                doubleCheckingProcedureStarted = false;
                doubleCheckingProcedureFinalized = false;
            }
        }
        if (rgba != null) {
            bitmapImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgba,bitmapImage);
            final int width = bitmapImage.getWidth();
            final int height = bitmapImage.getHeight();
            Log.d(TAG, "takePhoto: Camera frame width: " + width + " height: " + height);
            //locPhotoToExif = getGPSToExif(mCurrentLocation); //TODO
        }
        if (bitmapImage != null)
        {
            // Turning tracking OFF
            mImageDetectionFilterIndex = 0;
            if ((!vpPhotoAccepted) && (!vpPhotoRejected))
            {
                final Bitmap tmpBitmapImage = bitmapImage;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(tmpBitmapImage);
                        imageView.resetZoom();
                        imageView.setVisibility(View.VISIBLE);
                        if (imageView.getImageAlpha()==128) imageView.setImageAlpha(255);
                        isVpPhotoOkTextView.setVisibility(View.VISIBLE);
                        acceptVpPhotoButton.setVisibility(View.VISIBLE);
                        rejectVpPhotoButton.setVisibility(View.VISIBLE);
                        vpsListView.setVisibility(View.GONE);
                    }
                });
            }

            do
            {
                // Waiting for user response
            } while ((!vpPhotoAccepted)&&(!vpPhotoRejected));

            Log.d(TAG, "takePhoto: LOOP ENDED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);

            if (vpPhotoAccepted) {
                Log.d(TAG, "takePhoto: vpPhotoAccepted!!!!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.GONE);
                        isVpPhotoOkTextView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        vpChecked[vpTrackedInPose] = true;
                    }
                });
                setVpsChecked();
                saveVpsChecked();
                try
                {

                    //pictureFile.renameTo(new File(getApplicationContext().getFilesDir(), pictureFileName));
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    bitmapImage.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String,String>();
                    /*
                    userMetadata.put("GPSLatitude", locPhotoToExif[0]);
                    userMetadata.put("GPSLongitude", locPhotoToExif[1]);
                    */
                    userMetadata.put("VP", ""+(vpTrackedInPose));
                    userMetadata.put("seamensorAccount", mymensorAccount);
                    /*
                    userMetadata.put("Precisioninm", locPhotoToExif[4]);
                    userMetadata.put("LocationMillis", locPhotoToExif[5]);
                    userMetadata.put("LocationMethod", locPhotoToExif[6]);
                    */
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose ]);
                    userMetadata.put("DateTime", formattedDateTime);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFile(
                            transferUtility,
                            "cap/"+pictureFileName,
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);
                    Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getState().toString());
                    Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getAbsoluteFilePath());
                    Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getBucket());
                    Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getKey());

                    if ((singleImageTrackingIsSet)&&(isArSwitchOn))
                    {
                        Log.d(TAG, "takePhoto: vpPhotoAccepted >>>>> calling setMarkerlessTrackingConfiguration");
                        if (!waitingUntilMultipleImageTrackingIsSet) {
                            setMultipleImageTrackingConfiguration();
                        }
                        singleImageTrackingIsSet = false;
                        vpIsDisambiguated = false;
                    }
                }
                catch (Exception e)
                {
                    Log.e(TAG, "takePhoto: Error when writing captured image to Remote Storage:"+e.toString());
                    vpChecked[vpTrackedInPose] = false;
                    setVpsChecked();
                    saveVpsChecked();
                    //waitingToCaptureVpAfterDisambiguationProcedureSuccessful =true;
                    e.printStackTrace();
                }
                vpPhotoAccepted = false;
                vpPhotoRequestInProgress = false;
                Log.d(TAG, "takePhoto: vpPhotoAccepted: vpPhotoRequestInProgress = "+vpPhotoRequestInProgress);
                if (isArSwitchOn) isHudOn = 1;
                if ((!waitingUntilMultipleImageTrackingIsSet)&&(isArSwitchOn)) {
                    setMultipleImageTrackingConfiguration();
                }
            }

            if (vpPhotoRejected) {
                Log.d(TAG, "takePhoto: vpPhotoRejected!!!!");
                try
                {
                    if (pictureFile.delete()) {
                        Log.d(TAG,"takePhoto: vpPhotoRejected >>>>> "+pictureFile.getName()+" deleted successfully");
                    };

                } catch (Exception e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView.getImageAlpha()==128) imageView.setImageAlpha(128);
                        imageView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        isVpPhotoOkTextView.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        // TURNING ON RADAR SCAN
                        if (!isArSwitchOn) {
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);

                        }

                    }
                });
                if (isArSwitchOn) {
                    isHudOn = 1;
                    if (resultSpecialTrk)
                    {
                        resultSpecialTrk = false;
                        vpIsDisambiguated = false;
                    }
                }
                vpChecked[vpTrackedInPose] = false;
                setVpsChecked();
                saveVpsChecked();
                lastVpPhotoRejected = true;
                vpPhotoRejected = false;
                vpPhotoRequestInProgress = false;
                Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
                Log.d(TAG, "takePhoto: vpPhotoRejected: vpPhotoRequestInProgress = "+vpPhotoRequestInProgress);
                if ((!waitingUntilMultipleImageTrackingIsSet)&&(isArSwitchOn)) {
                    setMultipleImageTrackingConfiguration();
                }
            }
        }
    }


    private void saveVpsChecked()
    {
        // Saving vpChecked state.
        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("", "VpsChecked");
            xmlSerializer.text("\n");
            for (int i = 0; i < (qtyVps); i++) {
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "VpNumber");
                xmlSerializer.text(Short.toString(vpNumber[i]));
                xmlSerializer.endTag("", "VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Checked");
                xmlSerializer.text(Boolean.toString(vpChecked[i]));
                xmlSerializer.endTag("", "Checked");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text(Long.toString(photoTakenTimeMillis[i]));
                xmlSerializer.endTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("", "Vp");
                xmlSerializer.text("\n");
            }
            xmlSerializer.endTag("", "VpsChecked");
            xmlSerializer.endDocument();
            String vpsCheckedFileContents = writer.toString();
            File vpsCheckedFile = new File(getApplicationContext().getFilesDir(), Constants.vpsCheckedConfigFileName);
            FileUtils.writeStringToFile(vpsCheckedFile,vpsCheckedFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile, myObjectMetadata);
            observer.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state.equals(TransferState.COMPLETED)) {
                        Log.d(TAG,"SaveVpsChecked(): TransferListener="+state.toString());
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
                    Log.e(TAG, "SaveVpsChecked(): vpsCheckedFile saving failed:"+ ex.toString());
                }

            });

        } catch (Exception e) {
            Log.e(TAG, "SaveVpsChecked(): ERROR data saving to Remote Storage:"+e.toString());
        }
    }



    private void setVpsChecked()
    {
        try
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int i=0; i<(qtyVps); i++)
                    {
                        //MetaioDebug.log("setVpsChecked: vpChecked["+i+"]="+vpChecked[i]);
                        if (vpsListView != null)
                        {
                            vpsListView.setItemChecked(i, vpChecked[i]);
                        }

                    }
                }
            });
        }
        catch (Exception e)
        {
            Log.e(TAG, "setVpsChecked failed:"+e.toString());
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View view, final int position, long id)
    {
        lastVpSelectedByUser = position;
        if (!isArSwitchOn) {
            vpTrackedInPose = position;
            vpIsManuallySelected = true;
        } else {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                    arSwitch.setVisibility(View.INVISIBLE);
                    positionCertifiedButton.setVisibility(View.INVISIBLE);
                    timeCertifiedButton.setVisibility(View.INVISIBLE);
                    connectedToServerButton.setVisibility(View.INVISIBLE);
                }
            });
            try
            {
                File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (position) + ".png");
                FileInputStream fis = new FileInputStream(descvpFile);
                vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                fis.close();
            }
            catch (Exception e)
            {
                Log.e(TAG, "vpLocationDescImageFile failed:"+e.toString());
            }
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Turning off tracking
                    mImageDetectionFilterIndex = 0;
                    // TURNING OFF RADAR SCAN
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    isShowingVpPhoto = true;
                    // Setting the correct listview set position
                    vpsListView.setItemChecked(position, vpChecked[position]);
                    // Show last captured date and what is the frequency
                    String lastTimeAcquiredAndNextOne = "";
                    String formattedNextDate="";
                    if (photoTakenTimeMillis[position]>0)
                    {
                        Date lastDate = new Date(photoTakenTimeMillis[position]);
                        Date nextDate = new Date(vpNextCaptureMillis[position]);
                        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ssZ"));
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String formattedLastDate = sdf.format(lastDate);
                        formattedNextDate = sdf.format(nextDate);
                        lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                formattedLastDate+"  "+
                                getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                formattedNextDate;
                    }
                    else
                    {
                        lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                getString(R.string.date_vp_touched_not_acquired)+"  "+
                                getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                getString(R.string.date_vp_touched_first_acquisition);
                    }
                    // VP Location Description TextView
                    vpLocationDesTextView.setText(vpLocationDesText[position] + "\n" + lastTimeAcquiredAndNextOne);
                    vpLocationDesTextView.setVisibility(View.VISIBLE);
                    // VP Location # TextView
                    String vpId = Integer.toString(vpNumber[position]);
                    vpId = getString(R.string.vp_name)+vpId;
                    vpIdNumber.setText(vpId);
                    vpIdNumber.setVisibility(View.VISIBLE);
                    // VP Location Picture ImageView
                    if (!(vpLocationDescImageFileContents==null))
                    {
                        imageView.setImageBitmap(vpLocationDescImageFileContents);
                        imageView.setVisibility(View.VISIBLE);
                        imageView.resetZoom();
                        imageView.setImageAlpha(255);
                    }
                    // Dismiss Location Description Buttons
                    alphaToggleButton.setVisibility(View.VISIBLE);
                    if (imageView.getImageAlpha()==128) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                    if (imageView.getImageAlpha()==255) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    showVpCapturesButton.setVisibility(View.VISIBLE);
                    vpsListView.setVisibility(View.GONE);
                }
            });
        }

    }

    public void onButtonClick(View v)
    {
        if (v.getId() == R.id.button3)
        {
            final View vFinal = v;
            Log.d(TAG, "Show Program Version");
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String message = getString(R.string.app_version_seamensor);
                    Snackbar.make(vFinal, message, Snackbar.LENGTH_LONG).show();
                }
            });
        }
        if (v.getId()==R.id.buttonAcceptVpPhoto)
        {
            vpPhotoAccepted = true;
            Log.d(TAG,"vpPhotoAccepted BUTTON PRESSED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);
        }
        if (v.getId()==R.id.buttonRejectVpPhoto)
        {
            vpPhotoRejected = true;
            Log.d(TAG,"vpPhotoRejected BUTTON PRESSED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);
        }
        if (v.getId()==R.id.buttonShowPreviousVpCapture)
        {
            photoSelected++;
            showVpCaptures(lastVpSelectedByUser);
        }
        if (v.getId()==R.id.buttonShowNextVpCapture)
        {
            photoSelected--;
            showVpCaptures(lastVpSelectedByUser);
        }

    }


    private void showVpCaptures(int vpSelected)
    {
        final Bitmap showVpPhotoImageFileContents;

        Log.d(TAG,"vpSelected="+vpSelected+" lastVpSelectedByUser="+lastVpSelectedByUser);
        final int position = vpSelected;
        final int vpToList = vpSelected;
        String vpPhotoFileName=" ";
        String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        /*
        Log.d(TAG,"directory.list="+directory.list().length);
        String[] filesInDirectory = directory.list();
        for (int i=0; i<directory.list().length; i++){
            Log.d(TAG, "filesInDirectory["+i+"] ="+filesInDirectory[i]);
        }
        */
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("cap_"+mymensorAccount+"_"+vpToList+"_");
            }
        });
        int numOfEntries = 0;
        try
        {
            {
                if (!(capsInDirectory==null))
                {
                    numOfEntries = capsInDirectory.length;
                    /*
                    for (int i=0; i<numOfEntries; i++){
                        Log.d(TAG, "capsInDirectory["+i+"] ="+capsInDirectory[i]);
                    }
                    */
                    if (photoSelected==-1) photoSelected = numOfEntries - 1;
                    if (photoSelected<0) photoSelected = 0;
                    if (photoSelected > (numOfEntries-1)) photoSelected = 0;
                    Log.d(TAG,"vpSelected="+vpSelected+" lastVpSelectedByUser="+lastVpSelectedByUser+" photoSelected="+photoSelected);
                    vpPhotoFileName = capsInDirectory[photoSelected];
                    Log.d(TAG,"showVpCaptures: vpPhotoFileName="+vpPhotoFileName);
                    InputStream fiscaps = MymUtils.getLocalFile(vpPhotoFileName,getApplicationContext());
                    showVpPhotoImageFileContents = BitmapFactory.decodeStream(fiscaps);
                    fiscaps.close();

                    StringBuilder sb = new StringBuilder(vpPhotoFileName);
                    final String filename =sb.substring(vpPhotoFileName.length()-17,vpPhotoFileName.length()-4);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!(showVpPhotoImageFileContents==null))
                            {
                                imageView.setImageBitmap(showVpPhotoImageFileContents);
                                imageView.setVisibility(View.VISIBLE);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha()==128) imageView.setImageAlpha(255);
                                String lastTimeAcquired = "";
                                Date lastDate = new Date(Long.parseLong(filename));
                                SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ssZ"));
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                String formattedLastDate = sdf.format(lastDate);
                                lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " +formattedLastDate;
                                vpLocationDesTextView.setText(vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);
                                imageView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View view, MotionEvent motionEvent) {
                                        switch(motionEvent.getAction())
                                        {
                                            case MotionEvent.ACTION_DOWN:
                                                x1 = motionEvent.getX();
                                                break;
                                            case MotionEvent.ACTION_UP:
                                                x2 = motionEvent.getX();
                                                float deltaX = x2 - x1;

                                                if ((Math.abs(deltaX) > 50)&&(isShowingVpPhoto)){
                                                    // Left to Right swipe action
                                                    if (x2 > x1){
                                                        photoSelected++;
                                                        showVpCaptures(lastVpSelectedByUser);
                                                    } else {
                                                        // Right to left swipe action
                                                        photoSelected++;
                                                        showVpCaptures(lastVpSelectedByUser);
                                                    }
                                                } else {
                                                    // consider as something else - a screen tap for example
                                                }
                                                break;
                                        }
                                        return false;
                                    }
                                });
                            }
                        }
                    });
                }
                else
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            String message = getString(R.string.no_photo_captured_in_this_vp);
                            Snackbar.make(imageView, message, Snackbar.LENGTH_LONG).show();
                            String lastTimeAcquiredAndNextOne = "";
                            String formattedNextDate="";
                            if (photoTakenTimeMillis[position]>0)
                            {
                                Date lastDate = new Date(photoTakenTimeMillis[position]);
                                Date nextDate = new Date(vpNextCaptureMillis[position]);
                                SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ssZ"));
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                String formattedLastDate = sdf.format(lastDate);
                                formattedNextDate = sdf.format(nextDate);
                                lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                        formattedLastDate+"  "+
                                        getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                        formattedNextDate;
                            }
                            else
                            {
                                lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                        getString(R.string.date_vp_touched_not_acquired)+"  "+
                                        getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                        getString(R.string.date_vp_touched_first_acquisition);
                            }
                            vpLocationDesTextView.setText(vpLocationDesText[position] + "\n" + lastTimeAcquiredAndNextOne);
                        }
                    });
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    private void loadConfigurationFile()
    {
        vpTrackedInPose = 1;
        vpLocationDesText = new String[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
        short[] vpMarkerlessMarkerWidth = new short[qtyVps];
        short[] vpMarkerlessMarkerHeigth = new short[qtyVps];
        vpNumber = new short[qtyVps];
        vpFrequencyUnit = new String[qtyVps];
        vpFrequencyValue = new long[qtyVps];
        vpChecked = new boolean[qtyVps];
        vpIsAmbiguous = new boolean[qtyVps];
        vpFlashTorchIsOn = new boolean[qtyVps];
        vpIsSuperSingle = new boolean[qtyVps];
        vpSuperIdIs20mm = new boolean[qtyVps];
        vpSuperIdIs100mm = new boolean[qtyVps];
        vpSuperMarkerId = new int[qtyVps];
        photoTakenTimeMillis = new long[qtyVps];
        vpNextCaptureMillis = new long[qtyVps];

        Log.d(TAG,"loadConfigurationFile() started");

        for (int i=0; i<(qtyVps); i++)
        {
            vpFrequencyUnit[i] = "";
            vpFrequencyValue[i] = 0;
        }

        // Load Initialization Values from file
        short vpListOrder = -1;

        try
        {
            // Getting a file path for vps configuration XML file

            Log.d(TAG,"Vps Config Local name = "+Constants.vpsConfigFileName);
            File vpsFile = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);
            InputStream fis = MymUtils.getLocalFile(Constants.vpsConfigFileName, getApplicationContext());
            try
            {
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();
                myparser.setInput(fis, null);
                int eventType = myparser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if(eventType == XmlPullParser.START_DOCUMENT)
                    {
                        //
                    }
                    else if(eventType == XmlPullParser.START_TAG)
                    {
                        if(myparser.getName().equalsIgnoreCase("Parameters"))
                        {
                            //
                        }
                        else if(myparser.getName().equalsIgnoreCase("AssetId"))
                        {
                            eventType = myparser.next();
                            assetId= Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("FrequencyUnit"))
                        {
                            eventType = myparser.next();
                            frequencyUnit = myparser.getText();
                        }
                        else if(myparser.getName().equalsIgnoreCase("FrequencyValue"))
                        {
                            eventType = myparser.next();
                            frequencyValue = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("QtyVps"))
                        {
                            eventType = myparser.next();
                            qtyVps = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("TolerancePosition"))
                        {
                            eventType = myparser.next();
                            tolerancePosition = Float.parseFloat(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("ToleranceRotation"))
                        {
                            eventType = myparser.next();
                            toleranceRotation = Float.parseFloat(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("Vp"))
                        {
                            vpListOrder++;
                            //MetaioDebug.log("VpListOrder: "+vpListOrder);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpNumber"))
                        {
                            eventType = myparser.next();
                            vpNumber[vpListOrder] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpXCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpYCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpZCameraDistance[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpXCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpYCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpZCameraRotation[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpLocDescription"))
                        {
                            eventType = myparser.next();
                            vpLocationDesText[vpListOrder] = myparser.getText();
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerWidth"))
                        {
                            eventType = myparser.next();
                            vpMarkerlessMarkerWidth[vpListOrder] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerHeigth"))
                        {
                            eventType = myparser.next();
                            vpMarkerlessMarkerHeigth[vpListOrder] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsAmbiguous"))
                        {
                            eventType = myparser.next();
                            vpIsAmbiguous[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFlashTorchIsOn"))
                        {
                            eventType = myparser.next();
                            vpFlashTorchIsOn[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsSuperSingle"))
                        {
                            eventType = myparser.next();
                            vpIsSuperSingle[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpSuperIdIs20mm"))
                        {
                            eventType = myparser.next();
                            vpSuperIdIs20mm[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("vpSuperIdIs100mm"))
                        {
                            eventType = myparser.next();
                            vpSuperIdIs100mm[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpSuperMarkerId"))
                        {
                            eventType = myparser.next();
                            vpSuperMarkerId[vpListOrder] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFrequencyUnit"))
                        {
                            eventType = myparser.next();
                            vpFrequencyUnit[vpListOrder] = myparser.getText();
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFrequencyValue"))
                        {
                            eventType = myparser.next();
                            vpFrequencyValue[vpListOrder] = Long.parseLong(myparser.getText());
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG)
                    {
                        //MetaioDebug.log("End tag "+myparser.getName());
                    }
                    else if(eventType == XmlPullParser.TEXT)
                    {
                        //MetaioDebug.log("Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                fis.close();
            }
            finally {
                Log.d(TAG, "Vps Config Local file = " + vpsFile);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Vps data loading failed:"+e.toString());
        }



        for (int i=0; i<(qtyVps); i++)
        {
            Log.d(TAG,"vpNumber["+i+"]="+vpNumber[i]);
            vpChecked[i] = false;
            if (vpFrequencyUnit[i].equalsIgnoreCase(""))
            {
                vpFrequencyUnit[i]=frequencyUnit;
            }
            if (vpFrequencyValue[i]==0)
            {
                vpFrequencyValue[i]=frequencyValue;
            }
        }
    }


    private void verifyVpsChecked()
    {
        boolean change = false;
        long presentMillis = MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference);
        long presentHour = presentMillis/(1000*60*60);
        long presentDay = presentMillis/(1000*60*60*24);
        long presentWeek = presentDay/7;
        long presentMonth = presentWeek/(52/12);

        for (int i=0; i<(qtyVps); i++)
        {
            //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
            if (vpChecked[i])
            {
                if (vpFrequencyUnit[i].equalsIgnoreCase("millis"))
                {
                    //MetaioDebug.log("Present Millis since Epoch: "+presentMillis);
                    if ((presentMillis-(photoTakenTimeMillis[i]))>(vpFrequencyValue[i]))
                    {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Millis since Epoch: "+(photoTakenTimeMillis[i]));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("hour"))
                {
                    //MetaioDebug.log("Present Hour since Epoch: "+presentHour);
                    if ((presentHour-(photoTakenTimeMillis[i]/(1000*60*60)))>(vpFrequencyValue[i]))
                    {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Hour since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("day"))
                {
                    //MetaioDebug.log("Present Day since Epoch: "+presentDay);
                    if ((presentDay-(photoTakenTimeMillis[i]/(1000*60*60*24)))>(vpFrequencyValue[i]))
                    {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Day since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("week"))
                {
                    //MetaioDebug.log("Present Week since Epoch: "+presentWeek);
                    if ((presentWeek-(photoTakenTimeMillis[i]/(1000*60*60*24*7)))>(vpFrequencyValue[i]))
                    {
                        vpChecked[i] = false;
                        change=true;
                    }
                    //MetaioDebug.log("Photo Week since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7)));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (vpFrequencyUnit[i].equalsIgnoreCase("month"))
                {
                    //MetaioDebug.log("Present Month since Epoch: "+presentMonth);
                    if ((presentMonth-(photoTakenTimeMillis[i]/(1000*60*60*24*7*(52/12))))>(vpFrequencyValue[i]))
                    {
                        vpChecked[i] = false;
                        change = true;
                    }
                    //MetaioDebug.log("Photo Month since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7*(52/12))));
                    //MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
                }
                if (change) setVpsChecked();
            }

            if (vpFrequencyUnit[i].equalsIgnoreCase("millis"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + vpFrequencyValue[i];
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("hour"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("day"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*24*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("week"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*7*24*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("month"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*(52/12)*7*24*60*60*1000);
            }
        }
    }


    private void loadVpsChecked() {
        Log.d(TAG, "loadVpsChecked() started ");
        int vpListOrder = -1;
        try {
            InputStream fis = MymUtils.getLocalFile(Constants.vpsCheckedConfigFileName, getApplicationContext());
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser myparser = xmlFactoryObject.newPullParser();
            myparser.setInput(fis, null);
            int eventType = myparser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                   //
                } else if (eventType == XmlPullParser.START_TAG) {

                    if (myparser.getName().equalsIgnoreCase("Vp")) {
                        vpListOrder++;
                    } else if (myparser.getName().equalsIgnoreCase("VpNumber")) {
                        eventType = myparser.next();
                        vpNumber[vpListOrder] = Short.parseShort(myparser.getText());
                    } else if (myparser.getName().equalsIgnoreCase("Checked")) {
                        eventType = myparser.next();
                        vpChecked[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                    } else if (myparser.getName().equalsIgnoreCase("PhotoTakenTimeMillis")) {
                        eventType = myparser.next();
                        photoTakenTimeMillis[vpListOrder] = Long.parseLong(myparser.getText());
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    //
                } else if (eventType == XmlPullParser.TEXT) {
                    //
                }
                eventType = myparser.next();
            }
            fis.close();
        } catch (Exception e) {
            Log.e(TAG, "Checked Vps data loading failed:" + e.getMessage());
        }
    }

    private void callTimeServerInBackground(){

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected void onPreExecute(){

            }

            @Override
            protected Void doInBackground(Void... params){
                isTimeCertified = false;
                long now = 0;
                Long loopStart = System.currentTimeMillis();
                Log.d(TAG, "callTimeServerInBackground: Calling SNTP");
                SntpClient sntpClient = new SntpClient();
                do {
                    if (isCancelled()) {
                        Log.i("AsyncTask", "callTimeServerInBackground: cancelled");
                        break;
                    }
                    if (sntpClient.requestTime("pool.ntp.org", 5000)) {
                        sntpTime = sntpClient.getNtpTime();
                        sntpTimeReference = sntpClient.getNtpTimeReference();
                        now = sntpTime + SystemClock.elapsedRealtime() - sntpTimeReference;
                        Log.i("SNTP", "SNTP Present Time =" + now);
                        Log.i("SNTP", "System Present Time =" + System.currentTimeMillis());
                        isTimeCertified = true;
                    }
                    if (now != 0)
                        Log.d(TAG, "callTimeServerInBackground: ntp:now=" + now);

                } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 10000));
                Log.d(TAG, "callTimeServerInBackground: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);

                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                if (isTimeCertified) {
                    Log.d(TAG, "callTimeServerInBackground: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                    Log.d(TAG, "callTimeServerInBackground: System.currentTimeMillis() AFTER setTime=" + MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                            Snackbar.make(timeCertifiedButton.getRootView(), getText(R.string.usingcerttimeistrue), Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else {
                    sntpTime = 0;
                    sntpTimeReference = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                            Snackbar.make(timeCertifiedButton.getRootView(), getText(R.string.usingcerttimeisfalse), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }.execute();
    }


}
