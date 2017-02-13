package com.mymensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.imgproc.Imgproc;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import static com.mymensor.Constants.cameraWidthInPixels;
import static com.mymensor.R.drawable.circular_button_gray;
import static java.nio.charset.StandardCharsets.UTF_8;

import static com.mymensor.R.drawable.circular_button_green;
import static com.mymensor.R.drawable.circular_button_red;


public class ImageCapActivity extends Activity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        AdapterView.OnItemClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    static {
        System.loadLibrary("MyMensor");
    }
    public native String getSecretKeyFromJNI();

    private static final String TAG = "ImageCapActvty";

    private static long backPressed;

    private String mymensorAccount;
    private int dciNumber;
    private short qtyVps = 0;

    private String vpsCheckedRemotePath;
    private String vpsRemotePath;

    private static Bitmap vpLocationDescImageFileContents;

    private short[] vpNumber;
    private boolean[] vpChecked;
    private boolean[] vpFlashTorchIsOn;
    private long[] photoTakenTimeMillis;
    private long[] vpNextCaptureMillis;
    private String[] vpLocationDesText;

    private boolean[] vpIsAmbiguous;
    private boolean[] vpIsSuperSingle;
    private int[] vpSuperMarkerId;

    private boolean inPosition = false;
    private boolean inRotation = false;
    private boolean isShowingVpPhoto = false;
    private boolean firstFrameAfterArSwitchOff = false;
    private int isHudOn = 1;

    private boolean vpIsManuallySelected=false;

    private TrackingValues trackingValues;
    private int vpTrackedInPose;

    public boolean vpPhotoAccepted = false;
    public boolean vpPhotoRejected = false;
    public boolean lastVpPhotoRejected = false;
    public int lastVpSelectedByUser;
    public int mediaSelected = 0;

    private short assetId;

    private boolean[] vpArIsConfigured;
    private boolean[] vpIsVideo;

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

    private boolean waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
    private boolean vpIsDisambiguated = true;
    private boolean doubleCheckingProcedureFinalized = false;
    private boolean doubleCheckingProcedureStarted = false;
    private boolean resultSpecialTrk = false; //TODO
    private boolean singleImageTrackingIsSet = false;
    private boolean waitingUntilSingleImageTrackingIsSet  = false;
    private boolean multipleImageTrackingIsSet = false;
    private boolean waitingUntilMultipleImageTrackingIsSet = false;
    private boolean idTrackingIsSet = false;
    private boolean validMarkerFound = false;

    private long millisWhenSingleImageTrackingWasSet = 0;

    private String frequencyUnit;
    private int frequencyValue;

    ListView vpsListView;
    ImageView radarScanImageView;
    ImageView mProgress;
    TouchImageView imageView;
    VideoView videoView;
    ImageView vpCheckedView;
    TextView isVpPhotoOkTextView;

    TextView vpLocationDesTextView;
    TextView vpIdNumber;

    TextView recText;

    Animation rotationRadarScan;
    Animation rotationMProgress;
    Animation blinkingText;

    FloatingActionButton callConfigButton;
    FloatingActionButton alphaToggleButton;
    FloatingActionButton showVpCapturesButton;

    FloatingActionButton deleteLocalMediaButton;
    FloatingActionButton shareMediaButton;

    ImageButton showPreviousVpCaptureButton;
    ImageButton showNextVpCaptureButton;
    Button acceptVpPhotoButton;
    Button rejectVpPhotoButton;

    LinearLayout arSwitchLinearLayout;
    LinearLayout uploadPendingLinearLayout;
    LinearLayout videoRecorderTimeLayout;
    LinearLayout linearLayoutButtonsOnShowVpCaptures;
    LinearLayout linearLayoutImageViewsOnShowVpCaptures;

    ImageView uploadPendingmageview;
    TextView uploadPendingText;

    ImageView positionCertifiedImageview;
    ImageView timeCertifiedImageview;

    Chronometer videoRecorderChronometer;

    Switch arSwitch;

    private boolean isArSwitchOn = true;

    FloatingActionButton positionCertifiedButton;
    FloatingActionButton timeCertifiedButton;
    FloatingActionButton connectedToServerButton;
    FloatingActionButton cameraShutterButton;
    FloatingActionButton videoCameraShutterButton;
    FloatingActionButton videoCameraShutterStopButton;

    Drawable circularButtonGreen;
    Drawable circularButtonRed;
    Drawable circularButtonGray;


    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;
    private AmazonS3 s3Amazon;

    // A List of all transfers
    private List<TransferObserver> observers;

    private int pendingUploadTransfers = 0;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean isTimeCertified;
    public long videoCaptureStartmillis;

    private boolean askForManualPhoto = false;
    private boolean askForManualVideo = false;
    private boolean capturingManualVideo = false;
    private boolean videoRecorderPrepared = false;
    private boolean stopManualVideo = false;

    protected MediaRecorder mMediaRecorder;

    protected MediaController mMediaController;

    private String videoFileName;
    private String videoFileNameLong;

    public boolean isPositionCertified = false; // Or true ???????????
    public boolean isConnectedToServer = false;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;
    public List<Mat> markerBuffer;
    public List<Mat> markerBufferSingle;

    // Whether the next camera frame should be saved as a photo.
    private boolean vpPhotoRequestInProgress;

    // The filters.
    private ARFilter[] mImageDetectionFilters;

    // The indices of the active filters.
    private int mImageDetectionFilterIndex;

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX="imageDetectionFilterIndex";

    // Matrix to hold camera calibration
    // initially with absolute compute values
    private MatOfDouble mCameraMatrix;

    private float x1 = 0;
    private float x2 = 0;


    private SoundPool.Builder soundPoolBuilder;
    private SoundPool soundPool;
    private int camShutterSoundID;
    private int videoRecordStartedSoundID;
    private int videoRecordStopedSoundID;
    boolean camShutterSoundIDLoaded = false;
    boolean videoRecordStartedSoundIDLoaded = false;
    boolean videoRecordStopedSoundIDLoaded = false;

    Point pt1;
    Point pt2;
    Point pt3;
    Point pt4;
    Point pt5;
    Point pt6;
    Scalar color;

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;
    protected Boolean mLocationUpdated;

    /**
     * Time when the location was updated represented as a Long.
     */
    protected Long mLastUpdateTime;

    protected String[] locPhotoToExif;

    BroadcastReceiver receiver;

    protected String showingMediaFileName;
    protected String showingMediaType;
    protected String showingMediaSha256;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d(TAG, "SCRRES Display Width (Pixels):"+metrics.widthPixels);
        Log.d(TAG, "SCRRES Display Heigth (Pixels):"+metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels)*(metrics.heightPixels))<921600) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }


        setContentView(R.layout.activity_imagecap);

        // Retrieve SeaMensor configuration info
        mymensorAccount = getIntent().getExtras().get("mymensoraccount").toString();
        dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        isTimeCertified = Boolean.parseBoolean(getIntent().getExtras().get("isTimeCertified").toString());

        Log.d(TAG,"onCreate: Starting ImageCapActivity with qtyVps="+qtyVps);

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Create an instance of GoogleAPIClient and request Location Services API.
        buildGoogleApiClient();

        mRequestingLocationUpdates = true;
        mLocationUpdated = false;

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        s3Amazon = CognitoSyncClientManager.getInstance();

        vpsRemotePath = mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";
        vpsCheckedRemotePath = mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";

        if (savedInstanceState != null) {
            mImageDetectionFilterIndex = savedInstanceState.getInt(
                    STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mImageDetectionFilterIndex = 0;
        }

        this.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            AudioAttributes audioAttrib = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setAudioAttributes(audioAttrib).setMaxStreams(6).build();
        }
        else {

            soundPool = new SoundPool(6, AudioManager.STREAM_NOTIFICATION, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                if (i==camShutterSoundID) camShutterSoundIDLoaded=true;
                if (i==videoRecordStartedSoundID) videoRecordStartedSoundIDLoaded=true;
                if (i==videoRecordStopedSoundID) videoRecordStopedSoundIDLoaded=true;
            }
        });

        camShutterSoundID = soundPool.load(this, R.raw.camerashutter,1);
        videoRecordStartedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimeup, 1);
        videoRecordStopedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimedown, 1);

        pt1 = new Point((double)Constants.xAxisTrackingCorrection,(double)Constants.yAxisTrackingCorrection);
        pt2 = new Point((double)(Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth),(double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth));
        pt3 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)),(double)Constants.yAxisTrackingCorrection);
        pt4 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)),(double)(Constants.yAxisTrackingCorrection-40));
        pt5 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)-20),(double)(Constants.yAxisTrackingCorrection)-20);
        pt6 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)+20),(double)(Constants.yAxisTrackingCorrection)-20);
        color = new Scalar((double)168,(double)207,(double)69);

        final Camera camera;
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);
        camera = Camera.open(0);

        final Parameters parameters = camera.getParameters();
        camera.release();

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.imagecap_javaCameraView);
        mCameraView.setCameraIndex(0);
        mCameraView.setMaxFrameSize(cameraWidthInPixels, Constants.cameraHeigthInPixels);
        mCameraView.setCvCameraViewListener(this);

        circularButtonGreen = ContextCompat.getDrawable(getApplicationContext(), circular_button_green);

        circularButtonRed = ContextCompat.getDrawable(getApplicationContext(), circular_button_red);

        circularButtonGray = ContextCompat.getDrawable(getApplicationContext(), circular_button_gray);

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

        recText = (TextView) this.findViewById(R.id.cronoText);

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

        blinkingText = AnimationUtils.loadAnimation(this, R.anim.textblink);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        videoView = (VideoView) this.findViewById(R.id.videoView1);

        vpCheckedView = (ImageView) this.findViewById(R.id.imageViewVpChecked);
        vpCheckedView.setVisibility(View.GONE);

        uploadPendingLinearLayout = (LinearLayout) this.findViewById(R.id.uploadPendingLinearLayout);

        arSwitchLinearLayout = (LinearLayout) this.findViewById(R.id.arSwitchLinearLayout);

        videoRecorderTimeLayout = (LinearLayout) this.findViewById(R.id.videoRecorderTimeLayout);

        linearLayoutButtonsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutButtonsOnShowVpCaptures);

        linearLayoutImageViewsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutImageViewsOnShowVpCaptures);

        uploadPendingmageview = (ImageView) this.findViewById(R.id.uploadPendingmageview);

        uploadPendingText = (TextView) this.findViewById(R.id.uploadPendingText);

        positionCertifiedImageview = (ImageView) this.findViewById(R.id.positionCertifiedImageview);

        timeCertifiedImageview = (ImageView) this.findViewById(R.id.timeCertifiedImageview);

        showPreviousVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowPreviousVpCapture);

        showNextVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowNextVpCapture);

        videoRecorderChronometer = (Chronometer) this.findViewById(R.id.recordingChronometer);

        arSwitch = (Switch) findViewById(R.id.arSwitch);

        cameraShutterButton = (FloatingActionButton) findViewById(R.id.cameraShutterButton);
        videoCameraShutterButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterButton);
        videoCameraShutterStopButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterStopButton);

        arSwitch.setChecked(true);

        arSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    isArSwitchOn = true;
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    videoCameraShutterButton.setVisibility(View.INVISIBLE);
                    videoCameraShutterStopButton.setVisibility(View.GONE);
                    videoRecorderTimeLayout.setVisibility(View.GONE);
                    mImageDetectionFilterIndex=1;
                    Snackbar.make(arSwitch.getRootView(),getText(R.string.arswitchison), Snackbar.LENGTH_LONG).show();
                } else {
                    isArSwitchOn = false;
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    videoCameraShutterButton.setVisibility(View.VISIBLE);
                    mImageDetectionFilterIndex=0;
                    askForManualPhoto = false;
                    vpIsManuallySelected = false;
                    firstFrameAfterArSwitchOff = true;
                    Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchisoff), Snackbar.LENGTH_LONG).show();
                }
                Log.d(TAG, "isArSwitchOn="+ isArSwitchOn);
            }
        });

        positionCertifiedButton = (FloatingActionButton) findViewById(R.id.positionCertifiedButton);
        timeCertifiedButton = (FloatingActionButton) findViewById(R.id.timeCertifiedButton);
        connectedToServerButton = (FloatingActionButton) findViewById(R.id.connectedToServerButton);

        callConfigButton = (FloatingActionButton) findViewById(R.id.buttonCallConfig);
        alphaToggleButton = (FloatingActionButton) findViewById(R.id.buttonAlphaToggle);
        showVpCapturesButton = (FloatingActionButton) findViewById(R.id.buttonShowVpCaptures);

        deleteLocalMediaButton = (FloatingActionButton) findViewById(R.id.deleteLocalMediaButton);
        shareMediaButton = (FloatingActionButton) findViewById(R.id.shareMediaButton);


        // Camera Shutter Button

        cameraShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Camera Button clicked!!!");
                askForManualPhoto = true;
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                float volume = actualVolume / maxVolume;
                // Is the sound loaded already?
                if (camShutterSoundIDLoaded) {
                    soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                    Log.d(TAG, "cameraShutterButton.setOnClickListener: Played sound");
                }
            }
        });

        // videoCamera Shutter Button

        videoCameraShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Video Camera Start Button clicked!!!");
                askForManualVideo = true;
                videoCameraShutterButton.setVisibility(View.GONE);
                videoCameraShutterStopButton.setVisibility(View.VISIBLE);
                videoRecorderChronometer.setBase(SystemClock.elapsedRealtime());
                videoRecorderChronometer.start();
                videoRecorderTimeLayout.setVisibility(View.VISIBLE);
                recText.startAnimation(blinkingText);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                float volume = actualVolume / maxVolume;
                if (videoRecordStartedSoundIDLoaded) {
                    soundPool.play(videoRecordStartedSoundID, volume, volume, 1, 0, 1f);
                    Log.d(TAG, "videoCameraShutterButton.setOnClickListener START: Played sound");
                }

            }
        });

        // videoCamera Shutter Stop Button

        videoCameraShutterStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Video Camera Stop Button clicked!!!");
                stopManualVideo = true;
                videoCameraShutterButton.setVisibility(View.VISIBLE);
                videoCameraShutterStopButton.setVisibility(View.GONE);
                videoRecorderChronometer.stop();
                recText.clearAnimation();
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                float volume = actualVolume / maxVolume;
                if (videoRecordStopedSoundIDLoaded) {
                    soundPool.play(videoRecordStopedSoundID, volume, volume, 1, 0, 1f);
                    Log.d(TAG, "videoCameraShutterButton.setOnClickListener STOP: Played sound");
                }

            }
        });

        // Position Certified Button

        final View.OnClickListener turnOffClickListenerPositionButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdates();
                Snackbar.make(view, getText(R.string.position_not_certified), Snackbar.LENGTH_LONG).show();
            }
        };

        final View.OnClickListener turnOnClickListenerPositionButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdates();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zz");
                String lastUpdatedOn = sdf.format(mLastUpdateTime);
                lastUpdatedOn = " ("+lastUpdatedOn+")";
                Snackbar.make(view, getText(R.string.position_is_certified)+lastUpdatedOn, Snackbar.LENGTH_LONG).show();
            }
        };

        positionCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocationUpdated) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zz");
                    String lastUpdatedOn = sdf.format(mLastUpdateTime);
                    lastUpdatedOn = " ("+lastUpdatedOn+")";
                    Snackbar.make(view, getText(R.string.position_is_certified)+lastUpdatedOn, Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.turn_off_location_updates), turnOffClickListenerPositionButton).show();
                } else {
                    Snackbar.make(view, getText(R.string.position_not_certified), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.turn_on_location_updates), turnOnClickListenerPositionButton).show();
                }
            }
        });

        positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));

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

        checkConnectionToServer();

        if (isConnectedToServer) {
            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        } else {
            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        }

        final View.OnClickListener undoOnClickListenerServerButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.tryingtoconnecttoserver), Snackbar.LENGTH_LONG).show();
                checkConnectionToServer();
            }
        };

        connectedToServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnectedToServer) {
                    Snackbar.make(view, getText(R.string.connectedtoserver), Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(view, getText(R.string.notconnectedtoserver), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.trytoconnect), undoOnClickListenerServerButton).show();
                }
            }
        });

        // Call Config Button

        final View.OnClickListener confirmOnClickListenerCallConfigButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar.make(view, getText(R.string.callingconfigactivity), Snackbar.LENGTH_LONG).show();

                try {
                    Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                    intent.putExtra("mymensoraccount", mymensorAccount);
                    intent.putExtra("dcinumber", dciNumber);
                    intent.putExtra("QtyVps", qtyVps);
                    intent.putExtra("sntpTime", sntpTime);
                    intent.putExtra("sntpReference", sntpTimeReference);
                    intent.putExtra("isTimeCertified", isTimeCertified);
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
        };

        callConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.confirmconfigloading), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.confirm), confirmOnClickListenerCallConfigButton).show();
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
                callConfigButton.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
                showNextVpCaptureButton.setVisibility(View.VISIBLE);
                imageView.resetZoom();
                if (imageView.getImageAlpha()==128)
                {
                    imageView.setImageAlpha(255);
                }
                mediaSelected = -1;
                showVpCaptures(lastVpSelectedByUser);
            }
        });


        final View.OnClickListener undoOnClickListenerDeleteLocalAndRemoteMediaButton = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        };


        deleteLocalMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Snackbar snackbar = Snackbar.make(view, getText(R.string.deletinglocal), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.undo), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Log.d(TAG, "UNDO deleteLocalMediaButton: File NOT DELETED");
                            }
                        }).setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int dismissType) {
                        super.onDismissed(snackbar, dismissType);
                        if (    dismissType == DISMISS_EVENT_TIMEOUT ||
                                dismissType == DISMISS_EVENT_SWIPE   ||
                                dismissType == DISMISS_EVENT_CONSECUTIVE ||
                                dismissType == DISMISS_EVENT_MANUAL) {
                            Log.d(TAG, "deleteLocalMediaButton: File DELETED: dismissType="+dismissType);
                            deleteLocalShownCapture(lastVpSelectedByUser, view);
                            showVpCaptures(lastVpSelectedByUser);
                        } else {
                            Log.d(TAG, "deleteLocalMediaButton: File NOT DELETED");
                            Snackbar.make(view, getText(R.string.keepinglocal), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                snackbar.show();
            }
        });


        shareMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"shareMediaButton:");
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (showingMediaType.equalsIgnoreCase("p")){
                    shareIntent.setType("image/jpg");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                    } catch(IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to copy Photo file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(),"com.mymensor.fileprovider", shareFile);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT,"https://app.mymensor.com/landing/?type=1&key=cap/"+showingMediaFileName+"&signature="+showingMediaSha256);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingphotousing)));
                }
                if (showingMediaType.equalsIgnoreCase("v")){
                    shareIntent.setType("video/*");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                    } catch(IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to copy Video file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(),"com.mymensor.fileprovider", shareFile);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT,"https://app.mymensor.com/landing/?type=1&key=cap/"+showingMediaFileName+"&signature="+showingMediaSha256);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingvideousing)));
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,"User has put device in airplane mode");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isConnectedToServer = false;
                        connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                    }
                });
            }
        };

        this.registerReceiver(receiver, intentFilter);

        mMediaController = new MediaController(this);

    }


    private void checkConnectionToServer(){

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG,"checkConnectionToServer: onPreExecute");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = MymUtils.isS3Available(s3Amazon);
                return result;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                Log.d(TAG,"checkConnectionToServer: onPostExecute: result="+result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result) {
                            isConnectedToServer = true;
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                        } else {
                            isConnectedToServer = false;
                            connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                        }
                    }
                });
            }
        }.execute();
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getLong(LAST_UPDATED_TIME_STRING_KEY);
            }
            //updateUI();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        if (mCurrentLocation == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
            //updateUI();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        mLocationUpdated = false;
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mLocationUpdated = false;
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
        mLocationUpdated = true;
        Log.d(TAG,"onLocationChanged: mLastUpdateTime="+mLastUpdateTime+" mCurrentLocation="+mCurrentLocation.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
            }
        });
        isPositionCertified = true;
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS); //UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionCertifiedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
            }
        });
        mLocationUpdated = false;
        isPositionCertified = false;
    }


    public String[] getLocationToExifStrings(Location location, String photoTakenMillis)
    {
        String[] locationString = new String[14];
        try
        {
            double[] gps = new double[2];
            if (location != null)
            {
                gps[0] = location.getLatitude();
                gps[1] = location.getLongitude();

                if (gps[0]<0)
                {
                    locationString[1]="S";
                    gps[0]=(-1)*gps[0];
                }
                else
                {
                    locationString[1]="N";
                }
                if (gps[1]<0)
                {
                    locationString[3]="W";
                    gps[1]=(-1)*gps[1];
                }
                else
                {
                    locationString[3]="E";
                }
                long latDegInteger = (long) (gps[0] - (gps[0] % 1));
                long latMinInteger = (long) ((60*(gps[0]-latDegInteger))-((60*(gps[0]-latDegInteger)) % 1));
                long latSecInteger = (long) (((60*(gps[0]-latDegInteger)) % 1)*60*1000);
                locationString[0]=""+latDegInteger+"/1,"+latMinInteger+"/1,"+latSecInteger+"/1000";

                long lonDegInteger = (long) (gps[1] - (gps[1] % 1));
                long lonMinInteger = (long) ((60*(gps[1]-lonDegInteger))-((60*(gps[1]-lonDegInteger)) % 1));
                long lonSecInteger = (long) (((60*(gps[1]-lonDegInteger)) % 1)*60*1000);
                locationString[2]=""+lonDegInteger+"/1,"+lonMinInteger+"/1,"+lonSecInteger+"/1000";
                locationString[8]= Double.toString(location.getLatitude());
                locationString[9]= Double.toString(location.getLongitude());
                locationString[4]= Float.toString(location.getAccuracy());
                locationString[5]= mLastUpdateTime.toString();
                locationString[6]= location.getProvider();
                locationString[7]= Double.toString(location.getAltitude());
                if (isTimeCertified) {
                    locationString[10]= Integer.toString(1);
                    locationString[11]= photoTakenMillis;
                } else {
                    locationString[10]= Integer.toString(0);
                    locationString[11]= photoTakenMillis;
                }
                if (isPositionCertified){
                    locationString[12]= Integer.toString(1);
                } else {
                    locationString[12]= Integer.toString(0);
                }
                if (isArSwitchOn){
                    locationString[13]= Integer.toString(1);
                } else {
                    locationString[13]= Integer.toString(0);
                }
                Log.d(TAG, "getLocationToExifStrings: LAT:"+gps[0]+" "+(gps[0] % 1)+" "+locationString[0]+locationString[1]+" LON:"+gps[1]+" "+locationString[2]+locationString[3]);
            }
            else
            {
                locationString[0] = " ";
                locationString[1] = " ";
                locationString[2] = " ";
                locationString[3] = " ";
                locationString[4] = " ";
                locationString[5] = " ";
                locationString[6] = " ";
                locationString[7] = " ";

            }
            for (int index = 0; index<locationString.length; index++)
            {
                if (locationString[index]==null) locationString[index]=" ";
                Log.d(TAG, "getLocationToExifStrings: locationString[index]="+locationString[index]);
            }

        }
        catch (Exception e)
        {
            Log.d(TAG, "getLocationToExifStrings: failed:"+e.toString());
        }
        return locationString;
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current filter indices.
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        if (mLastUpdateTime != null) {
            savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        }
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onStart()
    {
        mGoogleApiClient.connect();
        super.onStart();
        Log.d(TAG,"onStart() ********************");

    }


    protected void returnToInitialScreen(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Closing VPx location photo");
                //Turning tracking On
                mCameraView.setVisibility(View.VISIBLE);
                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.GONE);
                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.GONE);
                mImageDetectionFilterIndex=1;
                isShowingVpPhoto = false;
                vpLocationDesTextView.setVisibility(View.GONE);
                vpIdNumber.setVisibility(View.GONE);
                if (videoView.isPlaying()) videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                callConfigButton.setVisibility(View.GONE);
                alphaToggleButton.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.GONE);
                showNextVpCaptureButton.setVisibility(View.GONE);
                showVpCapturesButton.setVisibility(View.GONE);
                vpsListView.setVisibility(View.VISIBLE);
                // TURNING ON RADAR SCAN
                radarScanImageView.setVisibility(View.VISIBLE);
                radarScanImageView.startAnimation(rotationRadarScan);
                // Turning on control buttons
                if (pendingUploadTransfers>0) uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                arSwitchLinearLayout.setVisibility(View.VISIBLE);
                arSwitch.setVisibility(View.VISIBLE);
                positionCertifiedButton.setVisibility(View.VISIBLE);
                timeCertifiedButton.setVisibility(View.VISIBLE);
                connectedToServerButton.setVisibility(View.VISIBLE);
            }
        });
    };


    @Override
    public void onBackPressed()
    {
        Log.d(TAG,"Testando JNI:"+getSecretKeyFromJNI());
        boolean specialBackClick = false;
        if (isShowingVpPhoto){
            specialBackClick = true;
            returnToInitialScreen();
        }

        if (!isArSwitchOn) {
            specialBackClick = true;
            arSwitch.setChecked(true);
            isArSwitchOn = true;
            cameraShutterButton.setVisibility(View.INVISIBLE);
            videoCameraShutterButton.setVisibility(View.INVISIBLE);
            videoCameraShutterStopButton.setVisibility(View.GONE);
            videoRecorderTimeLayout.setVisibility(View.GONE);
            mImageDetectionFilterIndex=1;
            Snackbar.make(arSwitch.getRootView(),getText(R.string.arswitchison), Snackbar.LENGTH_LONG).show();
        }

        if ((!isShowingVpPhoto)&&(isArSwitchOn)){
            if ((backPressed + 2000 > System.currentTimeMillis())&&(!specialBackClick))
            {
                super.onBackPressed();
            }
            else {
                if (!specialBackClick){
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Snackbar.make(mCameraView,getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }

            backPressed = System.currentTimeMillis();
        }

    }


    @Override
    public void recreate() {
            super.recreate();
            Log.d(TAG,"recreate() ********************");
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

        // Use TransferUtility to get all upload transfers.
        pendingUploadTransfers = 0;
        observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);
        TransferListener listener = new UploadListener();
        for (TransferObserver observer : observers) {

            // For each transfer we will will create an entry in
            // transferRecordMaps which will display
            // as a single row in the UI
            //HashMap<String, Object> map = new HashMap<String, Object>();
            //Util.fillMap(map, observer, false);
            //transferRecordMaps.add(map);

            // Sets listeners to in progress transfers
            if (!TransferState.COMPLETED.equals(observer.getState())) {
                observer.setTransferListener(listener);
                pendingUploadTransfers++;
                Log.d(TAG,"Observer ID:"+observer.getId()+" key:"+observer.getKey()+" state:"+observer.getState()+" %:"+(observer.getBytesTransferred()/observer.getBytesTotal())*100);
                transferUtility.resume(observer.getId());
            }
        }
        updatePendingUpload();

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                    Snackbar.make(vpsListView.getRootView(),getString(R.string.imagecapready), Snackbar.LENGTH_LONG).show();
                }

            }
        });
        returnToInitialScreen();
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
        returnToInitialScreen();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"onPause CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        if (observers != null && !observers.isEmpty()) {
            for (TransferObserver observer : observers) {
                observer.cleanTransferListener();
            }
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG,"onStop CALLED");
        mGoogleApiClient.disconnect();
        saveVpsChecked();
        if (mMediaRecorder!=null){
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
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
        this.unregisterReceiver(receiver);
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
                    mCameraMatrix = MymUtils.getCameraMatrix(cameraWidthInPixels, Constants.cameraHeigthInPixels);
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
        idTrackingIsSet = false;
        markerBufferSingle = new ArrayList<Mat>();
        try
        {
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (vpIndex) + ".png");
            Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            markerBufferSingle.add(tmpMarker);
        }
        catch (Exception e)
        {
            Log.e(TAG, "setSingleImageTrackingConfiguration(int vpIndex): markerImageFileContents failed:"+e.toString());
        }
        ARFilter trackFilter = null;
        Log.d(TAG,"setSingleImageTrackingConfiguration: markerBufferSingle.toArray().length="+markerBufferSingle.toArray().length);
        try {
            trackFilter = new ImageDetectionFilter(
                ImageCapActivity.this,
                markerBufferSingle.toArray(),
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
            millisWhenSingleImageTrackingWasSet = System.currentTimeMillis();
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
                        Log.d(TAG, "onPreExecute(): setMultipleImageTrackingConfiguration IN BACKGROUND - Lighting Waiting Circle");
                        Log.d(TAG,"waitingUntilMultipleImageTrackingIsSet="+ waitingUntilMultipleImageTrackingIsSet);
                        Log.d(TAG,"multipleImageTrackingIsSet="+multipleImageTrackingIsSet);
                        Log.d(TAG,"waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet);
                        Log.d(TAG,"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                        Log.d(TAG,"isHudOn="+isHudOn);
                        mProgress.setVisibility(View.VISIBLE);
                        mProgress.startAnimation(rotationMProgress);
                    }
                });

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
                Log.d(TAG,"setMultipleImageTrackingConfiguration: markerBuffer.toArray().length="+markerBuffer.toArray().length);
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
                        idTrackingIsSet = false;
                        multipleImageTrackingIsSet = true;
                        isHudOn = 1;

                    }
                });
                Log.d(TAG,"onPostExecute: waitingUntilMultipleImageTrackingIsSet="+ waitingUntilMultipleImageTrackingIsSet);
                Log.d(TAG,"multipleImageTrackingIsSet="+multipleImageTrackingIsSet);
                Log.d(TAG,"waitingUntilSingleImageTrackingIsSet="+waitingUntilSingleImageTrackingIsSet);
                Log.d(TAG,"singleImageTrackingIsSet="+singleImageTrackingIsSet);
                Log.d(TAG,"isHudOn="+isHudOn);
            }
        }.execute();
    }


    private void setIdTrackingConfiguration(){
        singleImageTrackingIsSet = false;
        ARFilter trackFilter = null;
        try {
            trackFilter = new IdMarkerDetectionFilter(
                    ImageCapActivity.this,
                    1,
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
            if (mImageDetectionFilterIndex==1){
                idTrackingIsSet = true;
                //idTrackingIsSetMillis = System.currentTimeMillis();
            } else {
                idTrackingIsSet = false;
            }
        }
    }


    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private boolean prepareVideoRecorder(String videoFileName){
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setOutputFile(videoFileName);
        try {
            mMediaRecorder.prepare();
            mCameraView.setRecorder(mMediaRecorder);
            mMediaRecorder.start();
            capturingManualVideo = true;
            videoRecorderPrepared = true;
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
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
            setVpsChecked();
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
                        if (firstFrameAfterArSwitchOff) {
                            vpsListView.smoothScrollToPosition(tmpvpfree);
                            firstFrameAfterArSwitchOff = false;
                        }
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

        // Start of AR OFF Photo

        if ((!isArSwitchOn)&&(askForManualPhoto)) {
            Log.d(TAG,"Requesting manual photo");
            takePhoto(rgba);
        }

        // End of AR OFF Photo

        // Start of AR OFF Video Recorder

        if ((!isArSwitchOn)&&(askForManualVideo) || (!isArSwitchOn)&&(capturingManualVideo))
        {
            if (askForManualVideo) {
                Log.d(TAG, "A manual video was requested");
                askForManualVideo = false;
                videoCaptureStartmillis = System.currentTimeMillis();
                long momentoLong = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
                photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
                String momento = String.valueOf(momentoLong);
                videoFileName = "cap_" + mymensorAccount + "_" + vpNumber[vpTrackedInPose] + "_v_" + momento + ".mp4";
                videoFileNameLong = getApplicationContext().getFilesDir() + "/" + videoFileName;
                if (!capturingManualVideo) prepareVideoRecorder(videoFileNameLong);
            }
            if (videoRecorderPrepared){
                if (((System.currentTimeMillis() - videoCaptureStartmillis) < Constants.shortVideoLength)&&(!stopManualVideo)) {
                    Log.d(TAG,"Waiting for video recording to end:"+(System.currentTimeMillis() - videoCaptureStartmillis));
                } else {
                    capturingManualVideo = false;
                    stopManualVideo = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recText.clearAnimation();
                            videoCameraShutterButton.setVisibility(View.VISIBLE);
                            videoCameraShutterStopButton.setVisibility(View.GONE);
                        }
                    });
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    float volume = actualVolume / maxVolume;
                    if (videoRecordStopedSoundIDLoaded) {
                        soundPool.play(videoRecordStopedSoundID, volume, volume, 1, 0, 1f);
                        Log.d(TAG, "Video STOP: Duartion limit exceeded Played sound");
                    }

                    try
                    {
                        mMediaRecorder.stop();
                        mCameraView.setRecorder(null);
                        videoRecorderPrepared = false;
                        String path = getApplicationContext().getFilesDir().getPath();
                        File directory = new File(path);
                        String[] fileInDirectory = directory.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                return filename.equalsIgnoreCase(videoFileName);
                            }
                        });
                        if (fileInDirectory!=null){
                            File videoFile = new File(getApplicationContext().getFilesDir(), videoFileName);
                            Log.d(TAG,"videoFile.getName()="+videoFile.getName());
                            Log.d(TAG,"videoFile.getPath()="+videoFile.getPath());
                            Log.d(TAG,"videoFileName="+videoFileName);
                            String fileSha256Hash = MymUtils.getFileHash(videoFile);
                            locPhotoToExif = getLocationToExifStrings(mCurrentLocation, Long.toString(photoTakenTimeMillis[vpTrackedInPose]));
                            ObjectMetadata myObjectMetadata = new ObjectMetadata();
                            //create a map to store user metadata
                            Map<String, String> userMetadata = new HashMap<String,String>();
                            userMetadata.put("loclatitude", locPhotoToExif[8]);
                            userMetadata.put("loclongitude", locPhotoToExif[9]);
                            userMetadata.put("vp", ""+(vpTrackedInPose));
                            userMetadata.put("mymensoraccount", mymensorAccount);
                            userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                            userMetadata.put("localtitude", locPhotoToExif[7]);
                            userMetadata.put("locmillis", locPhotoToExif[5]);
                            userMetadata.put("locmethod", locPhotoToExif[6]);
                            userMetadata.put("loccertified", locPhotoToExif[12]);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                            sdf.setTimeZone(TimeZone.getDefault());
                            String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose ]);
                            userMetadata.put("datetime", formattedDateTime);
                            userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                            userMetadata.put("timecertified", locPhotoToExif[10]);
                            userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                            userMetadata.put("sha-256", fileSha256Hash);
                            //call setUserMetadata on our ObjectMetadata object, passing it our map
                            myObjectMetadata.setUserMetadata(userMetadata);
                            //uploading the objects
                            TransferObserver observer = MymUtils.storeRemoteFileLazy(
                                    transferUtility,
                                    "cap/"+videoFileName,
                                    Constants.BUCKET_NAME,
                                    videoFile,
                                    myObjectMetadata);

                            observer.setTransferListener(new UploadListener());
                            pendingUploadTransfers++;
                            updatePendingUpload();
                            if (observer!=null){
                                Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getState().toString());
                                Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getAbsoluteFilePath());
                                Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getBucket());
                                Log.d(TAG, "takePhoto: AWS s3 Observer: "+observer.getKey());
                            } else {
                                Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                                vpChecked[vpTrackedInPose] = false;
                                setVpsChecked();
                                saveVpsChecked();
                            }
                            videoRecorderChronometer.stop();
                        } else {
                            Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                            vpChecked[vpTrackedInPose] = false;
                            setVpsChecked();
                            saveVpsChecked();
                        }
                    }
                    catch (AmazonServiceException ase) {
                        Log.e(TAG, "Failure to save video : AmazonServiceException: Error when writing captured image to Remote Storage:"+ase.toString());
                        isConnectedToServer = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isConnectedToServer) {
                                    connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                                } else {
                                    connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                                }
                            }
                        });
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "Failure to save video to remote storage:"+e.toString());
                        vpChecked[vpTrackedInPose] = false;
                        setVpsChecked();
                        saveVpsChecked();
                        //waitingToCaptureVpAfterDisambiguationProcedureSuccessful =true;
                        e.printStackTrace();
                    }
                }
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
            }

        }

        // End of AR OFF Video Recorder


        // Start of Photos with AR

        if ((mImageDetectionFilters!=null) && (isArSwitchOn)) {
            mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, isHudOn, 0);
            if (mImageDetectionFilters[mImageDetectionFilterIndex].getPose() != null)
            {
                trackingValues = trackingValues.setTrackingValues(mImageDetectionFilters[mImageDetectionFilterIndex].getPose());
                if (!singleImageTrackingIsSet) {
                    vpTrackedInPose = trackingValues.getVpNumberTrackedInPose();
                }
                if (idTrackingIsSet){
                    int markerIdInPose = trackingValues.getVpNumberTrackedInPose();
                    validMarkerFound = false;
                    Log.d(TAG, "idTrackingIsSet: markerIdInPose=" + markerIdInPose);
                    for (int j = 0; j < Constants.validIdMarkersForMyMensor.length; j++) {
                        if (Constants.validIdMarkersForMyMensor[j] == markerIdInPose) {
                            for (int k = 1; k < (qtyVps); k++) {
                                Log.d(TAG, "idTrackingIsSet: vpSuperMarkerId[" + k + "]=" + vpSuperMarkerId[k]);
                                if (vpSuperMarkerId[k] == markerIdInPose) {
                                    vpTrackedInPose = k;
                                    validMarkerFound = true;
                                    Log.d(TAG, "idTrackingIsSet: vpTrackedInPose=" + vpTrackedInPose);
                                }
                            }
                        }
                    }
                    if (!validMarkerFound) setMultipleImageTrackingConfiguration();
                }

                if ((vpTrackedInPose > 0) && (vpTrackedInPose < (qtyVps + 1)))
                {
                    final int tmpvp = vpTrackedInPose;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // TURNING OFF RADAR SCAN
                            radarScanImageView.clearAnimation();
                            radarScanImageView.setVisibility(View.GONE);
                            if (vpChecked[vpTrackedInPose]) {
                                vpCheckedView.setVisibility(View.VISIBLE);
                            } else {
                                if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                            }
                            int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                            int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            if (tmpvp < firstVisiblePosition || tmpvp > lastVisiblePosition) {
                                vpsListView.smoothScrollToPosition(tmpvp);
                                firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                                lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            }
                            int k = firstVisiblePosition - 1;
                            int i = -1;
                            do {
                                k++;
                                i++;
                                if (k == tmpvp) {
                                    vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255, 0, 175, 239));
                                } else {
                                    vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                                }
                            } while (k < lastVisiblePosition);
                        }
                    });

                    // If it is a NORMAL VP DETECTED, then set Single Image Tracking until capture, to speed up things.

                    if ((!vpIsAmbiguous[vpTrackedInPose]) ||
                            ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated) && (!vpIsSuperSingle[vpTrackedInPose])) ||
                            (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)) {
                        if (!vpChecked[vpTrackedInPose]) {
                            if (!singleImageTrackingIsSet) {
                                if (!waitingUntilSingleImageTrackingIsSet) {
                                    if (vpArIsConfigured[vpTrackedInPose])
                                        setSingleImageTrackingConfiguration(vpTrackedInPose);
                                }

                            }
                        }
                    }

                    // If it is a AMBIGUOUS VP DETECTED AND NOT SUPER then set Id tracking to disambiguate.

                    if (((vpIsAmbiguous[vpTrackedInPose])&&(!idTrackingIsSet)&&(!waitingToCaptureVpAfterDisambiguationProcedureSuccessful))||(doubleCheckingProcedureStarted))
                    {
                        Log.d(TAG, "MULTIIMAGE: AMBIGUOUS VP DETECTED then set Id tracking to disambiguate");
                        mImageDetectionFilterIndex = 1;
                        setIdTrackingConfiguration();
                        singleImageTrackingIsSet = false;
                        vpIsDisambiguated = false;
                        waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
                        if (doubleCheckingProcedureStarted){
                            doubleCheckingProcedureStarted = false;
                            doubleCheckingProcedureFinalized = true;
                        }
                    }

                    if (idTrackingIsSet)
                    {
                        Log.d(TAG, "idTrackingIsSet: validMarkerFound=" + validMarkerFound);
                        if (validMarkerFound) {
                            if (!vpIsSuperSingle[vpTrackedInPose]) {
                                vpIsDisambiguated = true;
                                waitingToCaptureVpAfterDisambiguationProcedureSuccessful = true;
                                Log.d(TAG, "Disambiguation SUCCESFULL: waiting for vp capture: vpTrackedInPose=" + vpTrackedInPose);
                                setSingleImageTrackingConfiguration(vpTrackedInPose);
                            } else {
                                if (isHudOn==1){

                                    float deltaX = (trackingValues.getXid() - vpXCameraDistance[vpTrackedInPose]);
                                    float deltaY = (trackingValues.getYid() - vpYCameraDistance[vpTrackedInPose]);
                                    float deltaZ = (trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]);

                                    float deltaRX = trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose];
                                    float deltaRY = trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose];
                                    float deltaRZ = trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose];

                                    double rotX = deltaRX;
                                    double rotY = deltaRZ;
                                    double rotZ = deltaRY;

                                    double dxp1_rotX = 0;
                                    double dxp2_rotX = 0;

                                    double dyp1_rotY = 0;
                                    double dyp4_rotY = 0;

                                    if ((rotZ>=(-20))||(rotZ<=20)){

                                        if (rotX<(-70)) {
                                            rotX = rotX + 360;
                                        } else {
                                            if (rotX>300) {
                                                rotX = rotX - 360;
                                            }
                                        }

                                        dxp1_rotX = rotX;
                                        dxp2_rotX = -rotX;

                                        dyp1_rotY = rotY;
                                        dyp4_rotY = -dyp1_rotY;
                                    }

                                    double xp1 = (double)Constants.xAxisTrackingCorrection+deltaX+deltaZ+dxp1_rotX;
                                    double yp1 = (double)Constants.yAxisTrackingCorrection+deltaY+deltaZ+dyp1_rotY;

                                    double xp2 = (double)Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth+deltaX-deltaZ+dxp2_rotX;
                                    double yp2 = (double)Constants.yAxisTrackingCorrection+deltaY+deltaZ-dyp1_rotY;

                                    double xp3 = (double)(Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth+deltaX-deltaZ-dxp2_rotX);
                                    double yp3 = (double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth+deltaY-deltaZ-dyp4_rotY);

                                    double xp4 = (double)(Constants.xAxisTrackingCorrection+deltaX+deltaZ-dxp1_rotX);
                                    double yp4 = (double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth+deltaY-deltaZ+dyp4_rotY);

                                    double xp5 = (xp1 + xp2) / 2;
                                    double yp5 = (yp1 + yp2) / 2;

                                    double xp6 = xp5;
                                    double yp6 = yp5-40;

                                    double xp7 = xp6-20;
                                    double yp7 = yp6+20;

                                    double xp8 = xp6+20;
                                    double yp8 = yp6+20;

                                    double deltaXAxis = (xp2 + xp1)/2;
                                    double deltaYAxis = (yp4 + yp1)/2;

                                    double xp1a = -(deltaXAxis-xp1)*Math.cos(rotZ*Math.PI/180)-(deltaYAxis-yp1)*Math.sin(rotZ*Math.PI/180);
                                    double yp1a = -(deltaXAxis-xp1)*Math.sin(rotZ*Math.PI/180)+(deltaYAxis-yp1)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb1 = new Point(xp1a+deltaXAxis, deltaYAxis-yp1a);

                                    double xp2a = (xp2-deltaXAxis)*Math.cos(rotZ*Math.PI/180)-(deltaYAxis-yp2)*Math.sin(rotZ*Math.PI/180);
                                    double yp2a = (xp2-deltaXAxis)*Math.sin(rotZ*Math.PI/180)+(deltaYAxis-yp2)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb1a = new Point(xp2a+deltaXAxis, deltaYAxis-yp2a);

                                    double xp3a = (xp3-deltaXAxis)*Math.cos(rotZ*Math.PI/180)+(yp3-deltaYAxis)*Math.sin(rotZ*Math.PI/180);
                                    double yp3a = (xp3-deltaXAxis)*Math.sin(rotZ*Math.PI/180)-(yp3-deltaYAxis)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb2 = new Point(xp3a+deltaXAxis, deltaYAxis-yp3a);

                                    double xp4a = -(deltaXAxis-xp4)*Math.cos(rotZ*Math.PI/180)+(yp4-deltaYAxis)*Math.sin(rotZ*Math.PI/180);
                                    double yp4a = -(deltaXAxis-xp4)*Math.sin(rotZ*Math.PI/180)-(yp4-deltaYAxis)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb2a = new Point(xp4a+deltaXAxis, deltaYAxis-yp4a);

                                    double xp5a = (xp1a + xp2a) / 2;
                                    double yp5a = (yp1a + yp2a) / 2;

                                    Point ptb3 = new Point(xp5a+deltaXAxis, deltaYAxis-yp5a);

                                    double xp6a = (xp5-deltaXAxis)*Math.cos(rotZ*Math.PI/180)-(deltaYAxis-yp5+40)*Math.sin(rotZ*Math.PI/180);
                                    double yp6a = (xp5-deltaXAxis)*Math.sin(rotZ*Math.PI/180)+(deltaYAxis-yp5+40)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb4 = new Point(xp6a+deltaXAxis, deltaYAxis-yp6a);

                                    double xp7a = (xp7 - deltaXAxis)*Math.cos(rotZ*Math.PI/180)-(deltaYAxis-yp7)*Math.sin(rotZ*Math.PI/180);
                                    double yp7a = (xp7 - deltaXAxis)*Math.sin(rotZ*Math.PI/180)+(deltaYAxis-yp7)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb5 = new Point(xp7a+deltaXAxis, deltaYAxis-yp7a);

                                    double xp8a = (xp8 - deltaXAxis)*Math.cos(rotZ*Math.PI/180)-(deltaYAxis-yp8)*Math.sin(rotZ*Math.PI/180);
                                    double yp8a = (xp8 - deltaXAxis)*Math.sin(rotZ*Math.PI/180)+(deltaYAxis-yp8)*Math.cos(rotZ*Math.PI/180);

                                    Point ptb6 = new Point(xp8a+deltaXAxis, deltaYAxis-yp8a);

                                    Scalar colorb = new Scalar((double)0,(double)175,(double)239);


                                    Log.d(TAG,"TST deltaX="+deltaX+" deltaY="+deltaY+" deltaZ="+deltaZ);
                                    Log.d(TAG,"TST deltaRX="+deltaRX+" deltaRY="+deltaRY+" deltaRZ="+deltaRZ);

                                    //Imgproc.rectangle(rgba,ptb1,ptb2,colorb,8);
                                    Imgproc.line(rgba,ptb1,ptb1a,colorb,8);
                                    Imgproc.line(rgba,ptb1a,ptb2,colorb,8);
                                    Imgproc.line(rgba,ptb2,ptb2a,colorb,8);
                                    Imgproc.line(rgba,ptb2a,ptb1,colorb,8);
                                    Imgproc.line(rgba,ptb3,ptb4,colorb,8);
                                    Imgproc.line(rgba,ptb4,ptb5,colorb,8);
                                    Imgproc.line(rgba,ptb4,ptb6,colorb,8);

                                    /*
                                    Drawing the initial Hud in MyMensor green
                                     */
                                    Imgproc.rectangle(rgba,pt1,pt2,color,8);
                                    Imgproc.line(rgba,pt3,pt4,color,8);
                                    Imgproc.line(rgba,pt4,pt5,color,8);
                                    Imgproc.line(rgba,pt4,pt6,color,8);
                                }
                                checkPositionToTarget(trackingValues, rgba);
                            }
                        }

                    }

                    if (singleImageTrackingIsSet)
                    {
                        if (((!vpIsAmbiguous[vpTrackedInPose]) ||
                                ((vpIsAmbiguous[vpTrackedInPose]) && (vpIsDisambiguated)) ||
                                (waitingToCaptureVpAfterDisambiguationProcedureSuccessful)) && (!vpIsSuperSingle[vpTrackedInPose])) {
                            if (!vpChecked[vpTrackedInPose]) {
                                if (!waitingUntilMultipleImageTrackingIsSet)
                                    checkPositionToTarget(trackingValues, rgba);
                            }
                        }

                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                            int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                            int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                            int k = firstVisiblePosition - 1;
                            int i = -1;
                            do {
                                k++;
                                i++;
                                vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                            } while (k<lastVisiblePosition);

                        }
                    });
                    if (isHudOn==0) isHudOn=1;
                    Log.d(TAG,"INVALID VP TRACKED IN POSE");
                }

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (vpCheckedView.isShown()) vpCheckedView.setVisibility(View.GONE);
                        int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                        int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                        int k = firstVisiblePosition - 1;
                        int i = -1;
                        do {
                            k++;
                            i++;
                            vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                        } while (k<lastVisiblePosition);
                        if ((!radarScanImageView.isShown()) && (isArSwitchOn) && (!isShowingVpPhoto)){
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);
                        }
                    }
                });
                /*
                Log.d(TAG,
                        "Tracking LOST!!!! (singleImageTrackingIsSet="+singleImageTrackingIsSet+
                        ") (waitingToCaptureVpAfterDisambiguationProcedureSuccessful="+waitingToCaptureVpAfterDisambiguationProcedureSuccessful+")");
                        */
                if ((singleImageTrackingIsSet)
                        &&((!waitingToCaptureVpAfterDisambiguationProcedureSuccessful)||
                        (System.currentTimeMillis()-millisWhenSingleImageTrackingWasSet>500))){
                    if (!isShowingVpPhoto){
                        singleImageTrackingIsSet = false;
                        setMultipleImageTrackingConfiguration();
                    }
                }
                if ((isHudOn==0)&&(!isShowingVpPhoto)) isHudOn=1;
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

            inPosition = ((Math.abs(trackingValues.getXid() - vpXCameraDistance[vpTrackedInPose]) <= (tolerancePosition/2)) &&
                    (Math.abs(trackingValues.getYid() - vpYCameraDistance[vpTrackedInPose]) <= (tolerancePosition/2)) &&
                    (Math.abs(trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose]) <= (tolerancePosition/2)));
            inRotation = ((Math.abs(trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose]) <= (toleranceRotation/2)) &&
                    (Math.abs(trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose]) <= (toleranceRotation/2)) &&
                    (Math.abs(trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose]) <= (toleranceRotation/2)));
        }

        Log.d(TAG,"TST inPosition="+inPosition+" inRotation="+inRotation+" waitingForMark...="+ waitingUntilMultipleImageTrackingIsSet +" vpPhReqInPress="+vpPhotoRequestInProgress);
        if ((inPosition) && (inRotation) && (!waitingUntilMultipleImageTrackingIsSet) && (!vpPhotoRequestInProgress)) {

            if ((vpIsAmbiguous[vpTrackedInPose])&&(!doubleCheckingProcedureStarted)) {
                mImageDetectionFilterIndex=1;
                setIdTrackingConfiguration();
                doubleCheckingProcedureStarted = true;

            }

            if ((!vpIsAmbiguous[vpTrackedInPose]) || ((vpIsAmbiguous[vpTrackedInPose])&&(vpIsDisambiguated)&&(doubleCheckingProcedureFinalized)) || vpIsSuperSingle[vpTrackedInPose]) {
                    if (!waitingUntilMultipleImageTrackingIsSet) {
                        if (isHudOn==1) {
                            isHudOn = 0;
                        } else {
                            Log.d(TAG,"Calling takePhoto: doubleCheckingProcedureFinalized="+doubleCheckingProcedureFinalized);
                            if (!vpChecked[vpTrackedInPose]) takePhoto(rgba);
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
        final String momento = String.valueOf(momentoLong);
        String pictureFileName;
        pictureFileName = "cap_"+mymensorAccount+"_"+vpNumber[vpTrackedInPose]+"_p_"+momento+".jpg";
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
        }
        if (bitmapImage != null)
        {
            // Turning tracking OFF
            mImageDetectionFilterIndex = 0;

            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            float volume = actualVolume / maxVolume;
            // Is the sound loaded already?
            if (camShutterSoundIDLoaded) {
                soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                Log.d(TAG, "takePhoto: Camera Shutter Played sound");
            }

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
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitch.setVisibility(View.INVISIBLE);
                        positionCertifiedButton.setVisibility(View.INVISIBLE);
                        timeCertifiedButton.setVisibility(View.INVISIBLE);
                        connectedToServerButton.setVisibility(View.INVISIBLE);
                        if (radarScanImageView.isShown()) {
                            radarScanImageView.clearAnimation();
                            radarScanImageView.setVisibility(View.GONE);
                        }
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
                        locPhotoToExif = getLocationToExifStrings(mCurrentLocation, momento);
                        if (pendingUploadTransfers>0) uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        arSwitch.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
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
                    ExifInterface locPhotoTags = new ExifInterface(pictureFile.getAbsolutePath());
                    locPhotoTags.setAttribute("GPSLatitude", locPhotoToExif[0]); //LocLatitude
                    locPhotoTags.setAttribute("GPSLatitudeRef", locPhotoToExif[1]);
                    locPhotoTags.setAttribute("GPSLongitude", locPhotoToExif[2]); //LocLongitude
                    locPhotoTags.setAttribute("GPSLongitudeRef", locPhotoToExif[3]);
                    locPhotoTags.setAttribute("GPSAltitude", locPhotoToExif[4]); //LocPrecisioninm
                    locPhotoTags.setAttribute("DateTime", locPhotoToExif[5]); //LocMillis
                    locPhotoTags.setAttribute("GPSProcessingMethod", locPhotoToExif[6]); //LocMethod
                    locPhotoTags.setAttribute("GPSAltitudeRef", locPhotoToExif[10]); //IsTimeCertified
                    locPhotoTags.setAttribute("GPSDateStamp", locPhotoToExif[11]); //photoTakenTimeMillis
                    locPhotoTags.setAttribute("Make", locPhotoToExif[12]); //IsPositionCertified
                    locPhotoTags.setAttribute("Model", locPhotoToExif[13]); //IsArSwitchOn
                    locPhotoTags.saveAttributes();
                    String fileSha256Hash = MymUtils.getFileHash(pictureFile);
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String,String>();
                    userMetadata.put("loclatitude", locPhotoToExif[8]);
                    userMetadata.put("loclongitude", locPhotoToExif[9]);
                    userMetadata.put("vp", ""+(vpTrackedInPose));
                    userMetadata.put("mymensoraccount", mymensorAccount);
                    userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                    userMetadata.put("localtitude", locPhotoToExif[7]);
                    userMetadata.put("locmillis", locPhotoToExif[5]);
                    userMetadata.put("locmethod", locPhotoToExif[6]);
                    userMetadata.put("loccertified", locPhotoToExif[12]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getDefault());
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose ]);
                    userMetadata.put("datetime", formattedDateTime);
                    userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                    userMetadata.put("timecertified", locPhotoToExif[10]);
                    userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                    userMetadata.put("sha-256", fileSha256Hash);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFile(
                            transferUtility,
                            "cap/"+pictureFileName,
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);

                    observer.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();

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
                catch (AmazonServiceException ase) {
                    Log.e(TAG, "takePhoto: AmazonServiceException: Error when writing captured image to Remote Storage:"+ase.toString());
                    isConnectedToServer = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isConnectedToServer) {
                                connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                            } else {
                                connectedToServerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    Log.e(TAG, "takePhoto: Error when writing captured image to Remote Storage:"+e.toString());
                    vpChecked[vpTrackedInPose] = false;
                    setVpsChecked();
                    saveVpsChecked();
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
                        if (pendingUploadTransfers>0) uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        arSwitch.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
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
            userMetadata.put("timestamp", MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsCheckedRemotePath + Constants.vpsCheckedConfigFileName), Constants.BUCKET_NAME, vpsCheckedFile, myObjectMetadata);
            observer.setTransferListener(new UploadListener());
            pendingUploadTransfers++;
            updatePendingUpload();

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgress.isShown()) {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
        vpLocationDescImageFileContents = null;
        lastVpSelectedByUser = position;
        if (!isArSwitchOn) {
            vpTrackedInPose = position;
            vpIsManuallySelected = true;
        } else {
            try
            {
                InputStream fis = MymUtils.getLocalFile("descvp"+(position)+".png",getApplicationContext());
                if (!(fis==null)){
                    vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                    fis.close();
                }
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "Showing vpLocationDescImageFile for VP="+position+"(vpLocationDescImageFileContents==null)"+(vpLocationDescImageFileContents==null));
                        // VP Location Picture ImageView
                        if (!(vpLocationDescImageFileContents==null))
                        {
                            imageView.setImageBitmap(vpLocationDescImageFileContents);
                            imageView.setVisibility(View.VISIBLE);
                        }
                        isShowingVpPhoto = true;
                        Log.d(TAG, "imageView.isShown()=" + imageView.isShown());
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                        arSwitch.setVisibility(View.INVISIBLE);
                        positionCertifiedButton.setVisibility(View.INVISIBLE);
                        timeCertifiedButton.setVisibility(View.INVISIBLE);
                        connectedToServerButton.setVisibility(View.INVISIBLE);
                        // Setting the correct listview set position
                        vpsListView.setItemChecked(position, vpChecked[position]);
                        vpsListView.setVisibility(View.GONE);
                        // Turning off tracking
                        mImageDetectionFilterIndex = 0;
                        // TURNING OFF RADAR SCAN
                        if (radarScanImageView.isShown()) {
                            radarScanImageView.clearAnimation();
                            radarScanImageView.setVisibility(View.GONE);
                        }
                        // Show last captured date and what is the frequency
                        String lastTimeAcquiredAndNextOne = "";
                        String formattedNextDate="";
                        if (photoTakenTimeMillis[position]>0)
                        {
                            Date lastDate = new Date(photoTakenTimeMillis[position]);
                            Date nextDate = new Date(vpNextCaptureMillis[position]);
                            SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ss zz"));
                            sdf.setTimeZone(TimeZone.getDefault());
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
                        // Activate Location Description Buttons
                        callConfigButton.setVisibility(View.VISIBLE);
                        alphaToggleButton.setVisibility(View.VISIBLE);
                        if (imageView.getImageAlpha()==128) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                        if (imageView.getImageAlpha()==255) alphaToggleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                        showVpCapturesButton.setVisibility(View.VISIBLE);
                    }
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "vpLocationDescImageFile failed:"+e.toString());
            }

        }

    }

    public void onButtonClick(View v)
    {
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
            mediaSelected++;
            showVpCaptures(lastVpSelectedByUser);
        }
        if (v.getId()==R.id.buttonShowNextVpCapture)
        {
            mediaSelected--;
            showVpCaptures(lastVpSelectedByUser);
        }

    }


    private void deleteLocalShownCapture(int vpSelected, final View view){
        Log.d(TAG,"deleteLocalShownCapture: vpSelected="+vpSelected+" lastVpSelectedByUser="+lastVpSelectedByUser);
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("cap_"+mymensorAccount+"_"+vpToList+"_");
            }
        });
        int numOfEntries = 0;
        try{
            if (!(capsInDirectory==null)){
                vpMediaFileName = capsInDirectory[mediaSelected];
                Log.d(TAG,"deleteLocalShownCapture: vpMediaFileName="+ path+"/"+vpMediaFileName);
                File fileToBeDeleted = new File(path+"/"+vpMediaFileName);
                if (fileToBeDeleted.delete()) {
                    Log.d(TAG,"deleteLocalShownCapture: vpMediaFileName="+ path+"/"+vpMediaFileName+" succesfully deleted from local storage.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = getString(R.string.local_file_deleted);
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                        }
                    });
                };
            }
        } catch (Exception e)
        {
            Log.e(TAG,"Error while deleting captures:"+e.toString());
        }

    }


    private void getRemotePictureFileMetadata (final String filename){

        new AsyncTask<Void, Void, ObjectMetadata>() {
            @Override
            protected void onPreExecute() { Log.d(TAG,"getRemotePictureFileMetadata: onPreExecute"); }

            @Override
            protected ObjectMetadata doInBackground(Void... params) {
                try{
                    final ObjectMetadata objMetadata = s3Amazon.getObjectMetadata(Constants.BUCKET_NAME, filename);
                    return objMetadata;
                } catch (Exception e){
                    Log.e(TAG,"getRemotePictureFileMetadata: exception: "+e.toString());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final ObjectMetadata objectMetadata) {
                Log.d(TAG,"getRemotePictureFileMetadata: onPostExecute");
                if (objectMetadata!=null){
                    Map<String, String> userMetadata = new HashMap<String,String>();
                    userMetadata = objectMetadata.getUserMetadata();
                    Log.d(TAG,"userMetadata="+userMetadata.toString());
                    Log.d(TAG,"Location=sha-256="+userMetadata.get("sha-256"));
                    showingMediaSha256 = userMetadata.get("sha-256");
                } else {
                    showingMediaSha256 = "";
                }
            }
        }.execute();
    }



    private void getRemoteFileMetadata (final String filename){

        new AsyncTask<Void, Void, ObjectMetadata>() {
            @Override
            protected void onPreExecute() { Log.d(TAG,"getRemoteFileMetadata: onPreExecute"); }

            @Override
            protected ObjectMetadata doInBackground(Void... params) {
                try{
                    final ObjectMetadata objMetadata = s3Amazon.getObjectMetadata(Constants.BUCKET_NAME, filename);
                    return objMetadata;
                } catch (Exception e){
                    Log.e(TAG,"getRemoteFileMetadata: exception: "+e.toString());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final ObjectMetadata objectMetadata) {
                Log.d(TAG,"getRemoteFileMetadata: onPostExecute");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (objectMetadata!=null){
                            Map<String, String> userMetadata = new HashMap<String,String>();
                            userMetadata = objectMetadata.getUserMetadata();
                            Log.d(TAG,"userMetadata="+userMetadata.toString());
                            Log.d(TAG,"Location=LocCertified="+userMetadata.get("loccertified")+" Time=TimeCertified="+userMetadata.get("timecertified"));
                            Log.d(TAG,"Location=sha-256="+userMetadata.get("sha-256"));
                            showingMediaSha256 = userMetadata.get("sha-256");
                            if (userMetadata.get("loccertified").equalsIgnoreCase("1")){
                                //IsPositionCertified
                                positionCertifiedImageview.setVisibility(View.VISIBLE);
                                positionCertifiedImageview.setBackground(circularButtonGreen);
                            } else {
                                positionCertifiedImageview.setVisibility(View.VISIBLE);
                                positionCertifiedImageview.setBackground(circularButtonRed);
                            }
                            if (userMetadata.get("timecertified").equalsIgnoreCase("1")){
                                //IsTimeCertified
                                timeCertifiedImageview.setVisibility(View.VISIBLE);
                                timeCertifiedImageview.setBackground(circularButtonGreen);
                            } else {
                                timeCertifiedImageview.setVisibility(View.VISIBLE);
                                timeCertifiedImageview.setBackground(circularButtonRed);
                            }
                        } else {
                            positionCertifiedImageview.setVisibility(View.VISIBLE);
                            positionCertifiedImageview.setBackground(circularButtonGray);
                            timeCertifiedImageview.setVisibility(View.VISIBLE);
                            timeCertifiedImageview.setBackground(circularButtonGray);
                        }
                    }
                });
            }
        }.execute();
    }


    private void showVpCaptures(int vpSelected)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCameraView.setVisibility(View.GONE);
            }
        });
        final Bitmap showVpPhotoImageFileContents;
        Log.d(TAG,"vpSelected="+vpSelected+" lastVpSelectedByUser="+lastVpSelectedByUser);
        final int position = vpSelected;
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("cap_"+mymensorAccount+"_"+vpToList+"_");
            }
        });
        int numOfEntries = 0;
        try
        {
            if (capsInDirectory.length>0)
            {
                numOfEntries = capsInDirectory.length;
                if (mediaSelected == -1) mediaSelected = numOfEntries - 1;
                if (mediaSelected <0) mediaSelected = 0;
                if (mediaSelected > (numOfEntries-1)) mediaSelected = 0;
                Log.d(TAG,"vpSelected="+vpSelected+" lastVpSelectedByUser="+lastVpSelectedByUser+" mediaSelected="+ mediaSelected);
                vpMediaFileName = capsInDirectory[mediaSelected];
                showingMediaFileName = vpMediaFileName;
                Log.d(TAG,"showVpCaptures: vpMediaFileName="+ vpMediaFileName);
                StringBuilder sb = new StringBuilder(vpMediaFileName);
                final String millisMoment = sb.substring(vpMediaFileName.length()-17, vpMediaFileName.length()-4);
                final String mediaType = sb.substring(vpMediaFileName.length()-19, vpMediaFileName.length()-18);
                showingMediaType = mediaType;
                if (mediaType.equalsIgnoreCase("p")){
                    // When the item is a photo
                    final InputStream fiscaps = MymUtils.getLocalFile(vpMediaFileName,getApplicationContext());
                    showVpPhotoImageFileContents = BitmapFactory.decodeStream(fiscaps);
                    fiscaps.close();
                    try {
                        getRemotePictureFileMetadata("cap/"+vpMediaFileName);
                    } catch (Exception e) {
                        Log.e(TAG,"Problem Remote files Metadata:"+e.toString());
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!(showVpPhotoImageFileContents==null))
                            {
                                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                try {
                                    ExifInterface tags = new ExifInterface(path+"/"+vpMediaFileName);
                                    Log.d(TAG,"Location=Make="+tags.getAttribute("Make")+" Time=GPSAltitudeRef="+tags.getAttribute("GPSAltitudeRef"));
                                    if (tags.getAttribute("Make").equalsIgnoreCase("1")){
                                        //IsPositionCertified
                                        positionCertifiedImageview.setVisibility(View.VISIBLE);
                                        positionCertifiedImageview.setBackground(circularButtonGreen);
                                    } else {
                                        positionCertifiedImageview.setVisibility(View.VISIBLE);
                                        positionCertifiedImageview.setBackground(circularButtonRed);
                                    }
                                    if (tags.getAttribute("GPSAltitudeRef").equalsIgnoreCase("1")){
                                        //IsTimeCertified
                                        timeCertifiedImageview.setVisibility(View.VISIBLE);
                                        timeCertifiedImageview.setBackground(circularButtonGreen);
                                    } else {
                                        timeCertifiedImageview.setVisibility(View.VISIBLE);
                                        timeCertifiedImageview.setBackground(circularButtonRed);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG,"Problem with Exif tags or drawable setting:"+e.toString());
                                }
                                videoView.setVisibility(View.GONE);
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageBitmap(showVpPhotoImageFileContents);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha()==128) imageView.setImageAlpha(255);
                                String lastTimeAcquired = "";
                                Date lastDate = new Date(Long.parseLong(millisMoment));
                                SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ss zz"));
                                sdf.setTimeZone(TimeZone.getDefault());
                                String formattedLastDate = sdf.format(lastDate);
                                lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " +formattedLastDate;
                                vpLocationDesTextView.setText(vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    // when the item is a video.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setVisibility(View.GONE);
                            linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                            linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                            positionCertifiedImageview.setVisibility(View.GONE);
                            timeCertifiedImageview.setVisibility(View.GONE);
                            try {
                                getRemoteFileMetadata("cap/"+vpMediaFileName);
                            } catch (Exception e) {
                                Log.e(TAG,"Problem Remote files Metadata:"+e.toString());
                            }
                            String lastTimeAcquired = "";
                            Date lastDate = new Date(Long.parseLong(millisMoment));
                            SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ss zz"));
                            sdf.setTimeZone(TimeZone.getDefault());
                            String formattedLastDate = sdf.format(lastDate);
                            lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " +formattedLastDate;
                            vpLocationDesTextView.setText(vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired);
                            vpLocationDesTextView.setVisibility(View.VISIBLE);
                            videoView.setVisibility(View.VISIBLE);
                            videoView.setVideoPath(path+"/"+vpMediaFileName);
                            videoView.setMediaController(mMediaController);
                            videoView.start();
                        }
                    });
                }
            }
            else
            {
                //when no item has been acquired to the vp.
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String message = getString(R.string.no_photo_captured_in_this_vp);
                        Snackbar.make(imageView, message, Snackbar.LENGTH_LONG).show();
                        returnToInitialScreen();
                        /*


                        String lastTimeAcquiredAndNextOne = "";
                        String formattedNextDate="";
                        if (photoTakenTimeMillis[position]>0)
                        {
                            Date lastDate = new Date(photoTakenTimeMillis[position]);
                            Date nextDate = new Date(vpNextCaptureMillis[position]);
                            SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(),"dd-MMM-yyyy HH:mm:ss zz"));
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
                        */
                    }
                });
            }
        }
        catch (Exception e)
        {
            Log.e(TAG,"Error while retrieving captures:"+e.toString());
        }
    }


    private void loadConfigurationFile()
    {
        vpTrackedInPose = 1;
        vpLocationDesText = new String[qtyVps];
        vpArIsConfigured = new boolean[qtyVps];
        vpIsVideo = new boolean[qtyVps];
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
                        else if(myparser.getName().equalsIgnoreCase("VpArIsConfigured"))
                        {
                            eventType = myparser.next();
                            vpArIsConfigured[vpListOrder] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsVideo"))
                        {
                            eventType = myparser.next();
                            vpIsVideo[vpListOrder] = Boolean.parseBoolean(myparser.getText());
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
            if (!(vpFrequencyUnit[i]==null))
            {
                if (vpFrequencyUnit[i].equalsIgnoreCase("")) vpFrequencyUnit[i]=frequencyUnit;
            } else {
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
                    if (sntpClient.requestTime("pool.ntp.org", 10000)) {
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


    /*
     * A TransferListener class that can listen to a upload task and be notified
     * when the status changes.
     */
    private class UploadListener implements TransferListener {

        // Simply updates the UI list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(TAG, "Observer: Error during upload: " + id, e);
            updatePendingUpload();
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(TAG, String.format("Observer: onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));
            updatePendingUpload();
        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(TAG, "Observer: onStateChanged: " + id + ", " + newState);
            if (newState.equals(TransferState.COMPLETED)){
                pendingUploadTransfers--;
                if (pendingUploadTransfers<0) pendingUploadTransfers=0;
            }
            updatePendingUpload();
        }
    }

    /*
     * Updates the ListView according to the observers.
     */
    private void updatePendingUpload() {

        if (pendingUploadTransfers==0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (uploadPendingText.isShown()) {
                        uploadPendingText.setText(Integer.toString(pendingUploadTransfers));
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    uploadPendingText.setText(Integer.toString(pendingUploadTransfers));
                }
            });
        }

        /*
        TransferObserver observer = null;
        HashMap<String, Object> map = null;
        for (int i = 0; i < observers.size(); i++) {
            observer = observers.get(i);
            //map = transferRecordMaps.get(i);
            //MymUtils.fillMap(map, observer, i == checkedIndex);
        }
        //simpleAdapter.notifyDataSetChanged();
        */
    }


}
