package com.mymensor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.amazonaws.AmazonClientException;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static com.mymensor.Constants.cameraWidthInPixels;
import static com.mymensor.R.drawable.circular_button_gray;
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

    private static final String TAG = "ImageCapActvty";

    private static long backPressed;

    private String mymensorAccount;
    private String origMymAcc;
    private String deviceId;
    private int dciNumber;
    private short qtyVps = 0;

    private String vpsRemotePath;

    private short[] vpNumber;
    private long[] photoTakenTimeMillis;
    private String[] vpLocationDesText;

    private boolean isShowingVpPhoto = false;

    private boolean vpIsManuallySelected = false;

    private int vpTrackedInPose;

    public boolean vpPhotoAccepted = false;
    public boolean vpPhotoRejected = false;
    public boolean vpPhotoTobeRemarked = false;
    public boolean vpVideoTobeReplayed = false;
    public boolean lastVpPhotoRejected = false;
    private String vpPhotoRemark = null;
    public int lastVpSelectedByUser;
    public int mediaSelected = 0;

    private short assetId;

    private boolean[] vpIsVideo;

    ListView vpsListView;
    ImageView mProgress;
    TouchImageView imageView;
    VideoView videoView;

    TextView vpAcquiredStatus;
    TextView idMarkerNumberTextView;

    TextView vpLocationDesTextView;
    TextView vpIdNumber;

    TextView recText;

    Animation rotationMProgress;
    Animation blinkingText;

    FloatingActionButton showVpCapturesButton;

    FloatingActionButton deleteLocalMediaButton;
    FloatingActionButton shareMediaButton;
    FloatingActionButton shareMediaButton2;

    ImageButton showPreviousVpCaptureButton;
    ImageButton showNextVpCaptureButton;
    ImageButton acceptVpPhotoButton;
    ImageButton rejectVpPhotoButton;
    ImageButton buttonRemarkVpPhoto;
    ImageButton buttonReplayVpVideo;
    ImageButton buttonStartVideoInVpCaptures;

    LinearLayout uploadPendingLinearLayout;
    LinearLayout videoRecorderTimeLayout;
    LinearLayout linearLayoutButtonsOnShowVpCaptures;
    LinearLayout linearLayoutImageViewsOnShowVpCaptures;
    LinearLayout linearLayoutAcceptImgButtons;

    ImageView uploadPendingmageview;
    TextView uploadPendingText;

    ImageView positionCertifiedImageview;
    ImageView timeCertifiedImageview;

    Chronometer videoRecorderChronometer;

    private boolean isArSwitchOn = false;

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
    private String videoThumbnailFileName;
    private String videoThumbnailFileNameLong;

    public boolean isPositionCertified = false;
    public boolean isConnectedToServer = false;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether the next camera frame should be saved as a photo.
    private boolean vpPhotoRequestInProgress;

    // The filters.
    private ARFilter[] mImageDetectionFilters;

    // The indices of the active filters.
    private int mImageDetectionFilterIndex;

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionFilterIndex";

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

    protected Boolean mymIsRunningOnKitKat = false;
    protected Boolean mymIsRunningOnFlippedDisplay = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mymIsRunningOnKitKat = true;
        }

        Log.d(TAG, "mymIsRunningOnKitKat = " + mymIsRunningOnKitKat);

        if (Build.MODEL.equals("Nexus 5X")) {
            mymIsRunningOnFlippedDisplay = true;
        }

        Log.d(TAG, "mymIsRunningOnFlippedDisplay = " + mymIsRunningOnFlippedDisplay);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
        Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            Log.d(TAG, "onCreate - Calling FULLSCREEN");
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
        //dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        //qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        isTimeCertified = Boolean.parseBoolean(getIntent().getExtras().get("isTimeCertified").toString());
        origMymAcc = getIntent().getExtras().get("origmymacc").toString();
        deviceId = getIntent().getExtras().get("deviceid").toString();

        Log.d(TAG, "onCreate: Starting ImageCapActivity with qtyVps=" + qtyVps + " MyM Account=" + mymensorAccount + " Orig MyM Account=" + origMymAcc);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Create an instance of GoogleAPIClient and request Location Services API.
        buildGoogleApiClient();

        mRequestingLocationUpdates = true;
        mLocationUpdated = false;

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        s3Amazon = CognitoSyncClientManager.getInstance();

        vpsRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";
        //vpsCheckedRemotePath = Constants.usersConfigFolder + "/" + mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";

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
        } else {

            soundPool = new SoundPool(6, AudioManager.STREAM_NOTIFICATION, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                if (i == camShutterSoundID) camShutterSoundIDLoaded = true;
                if (i == videoRecordStartedSoundID) videoRecordStartedSoundIDLoaded = true;
                if (i == videoRecordStopedSoundID) videoRecordStopedSoundIDLoaded = true;
            }
        });

        camShutterSoundID = soundPool.load(this, R.raw.camerashutter, 1);
        videoRecordStartedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimeup, 1);
        videoRecordStopedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimedown, 1);

        //pt1 = new Point((double) Constants.xAxisTrackingCorrection, (double) Constants.yAxisTrackingCorrection);
        //pt2 = new Point((double) (Constants.xAxisTrackingCorrection + Constants.standardMarkerlessMarkerWidth), (double) (Constants.yAxisTrackingCorrection + Constants.standardMarkerlessMarkerHeigth));
        //pt3 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2)), (double) Constants.yAxisTrackingCorrection);
        //pt4 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2)), (double) (Constants.yAxisTrackingCorrection - 40));
        //pt5 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2) - 20), (double) (Constants.yAxisTrackingCorrection) - 20);
        //pt6 = new Point((double) (Constants.xAxisTrackingCorrection + (Constants.standardMarkerlessMarkerWidth / 2) + 20), (double) (Constants.yAxisTrackingCorrection) - 20);
        //color = new Scalar((double) 168, (double) 207, (double) 69);

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

        //trackingValues = new TrackingValues();

        mImageDetectionFilterIndex = 1;

        photoTakenTimeMillis = new long[Constants.maxQtyVps];

        String[] newVpsList = new String[Constants.maxQtyVps];
        for (int i = 0; i < Constants.maxQtyVps; i++) newVpsList[i] = getString(R.string.vp_name) + i;

        vpsListView = (ListView) this.findViewById(R.id.vp_list);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, newVpsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(R.id.textViewVpList);
                // Set the text color of TextView (ListView Item)
                //tv.setTextColor(Color.WHITE);
                // Generate ListView Item using TextView
                return view;
            }
        };
        vpsListView.setAdapter(arrayAdapter);
        vpsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        vpsListView.setOnItemClickListener(this);
        vpsListView.setVisibility(View.VISIBLE);


        vpLocationDesTextView = (TextView) this.findViewById(R.id.textView1);
        vpIdNumber = (TextView) this.findViewById(R.id.textView2);

        recText = (TextView) this.findViewById(R.id.cronoText);

        linearLayoutAcceptImgButtons = (LinearLayout) this.findViewById(R.id.linearLayoutAcceptImgButtons);
        acceptVpPhotoButton = (ImageButton) this.findViewById(R.id.buttonAcceptVpPhoto);
        rejectVpPhotoButton = (ImageButton) this.findViewById(R.id.buttonRejectVpPhoto);
        buttonRemarkVpPhoto = (ImageButton) this.findViewById(R.id.buttonRemarkVpPhoto);
        buttonReplayVpVideo = (ImageButton) this.findViewById(R.id.buttonReplayVpVideo);

        //radarScanImageView = (ImageView) this.findViewById(R.id.imageViewRadarScan);
        //rotationRadarScan = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        //radarScanImageView.setVisibility(View.VISIBLE);
        //radarScanImageView.startAnimation(rotationRadarScan);

        mProgress = (ImageView) this.findViewById(R.id.waitingTrkLoading);
        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);

        blinkingText = AnimationUtils.loadAnimation(this, R.anim.textblink);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        videoView = (VideoView) this.findViewById(R.id.videoView1);

        uploadPendingLinearLayout = (LinearLayout) this.findViewById(R.id.uploadPendingLinearLayout);

        //arSwitchLinearLayout = (LinearLayout) this.findViewById(R.id.arSwitchLinearLayout);

        videoRecorderTimeLayout = (LinearLayout) this.findViewById(R.id.videoRecorderTimeLayout);

        linearLayoutButtonsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutButtonsOnShowVpCaptures);

        linearLayoutImageViewsOnShowVpCaptures = (LinearLayout) this.findViewById(R.id.linearLayoutImageViewsOnShowVpCaptures);

        //linearLayoutConfigCaptureVps = (LinearLayout) this.findViewById(R.id.linearLayoutConfigCaptureVps);

        //linearLayoutAmbiguousVp = (LinearLayout) this.findViewById(R.id.linearLayoutAmbiguousVp);

        //linearLayoutSuperSingleVp = (LinearLayout) this.findViewById(R.id.linearLayoutSuperSingleVp);

        //linearLayoutVpArStatus = (LinearLayout) this.findViewById(R.id.linearLayoutVpArStatus);

        //linearLayoutMarkerId = (LinearLayout) this.findViewById(R.id.linearLayoutMarkerId);

        uploadPendingmageview = (ImageView) this.findViewById(R.id.uploadPendingmageview);

        uploadPendingText = (TextView) this.findViewById(R.id.uploadPendingText);

        positionCertifiedImageview = (ImageView) this.findViewById(R.id.positionCertifiedImageview);

        timeCertifiedImageview = (ImageView) this.findViewById(R.id.timeCertifiedImageview);

        showPreviousVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowPreviousVpCapture);

        showNextVpCaptureButton = (ImageButton) this.findViewById(R.id.buttonShowNextVpCapture);

        buttonStartVideoInVpCaptures = (ImageButton) this.findViewById(R.id.buttonStartVideoInVpCaptures);

        videoRecorderChronometer = (Chronometer) this.findViewById(R.id.recordingChronometer);

        //arSwitch = (Switch) findViewById(R.id.arSwitch);

        idMarkerNumberTextView = (TextView) findViewById(R.id.idMarkerNumberTextView);
        vpAcquiredStatus = (TextView) this.findViewById(R.id.vpAcquiredStatus);

        //buttonAmbiguousVpToggle = (FloatingActionButton) findViewById(R.id.buttonAmbiguousVpToggle);
        //buttonSuperSingleVpToggle = (FloatingActionButton) findViewById(R.id.buttonSuperSingleVpToggle);

        cameraShutterButton = (FloatingActionButton) findViewById(R.id.cameraShutterButton);
        videoCameraShutterButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterButton);
        videoCameraShutterStopButton = (FloatingActionButton) findViewById(R.id.videoCameraShutterStopButton);

        //arSwitch.setChecked(true);
        /*
        arSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                if (isOn) {
                    isArSwitchOn = true;
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.INVISIBLE);
                    }
                    videoCameraShutterStopButton.setVisibility(View.GONE);
                    videoRecorderTimeLayout.setVisibility(View.GONE);
                    mImageDetectionFilterIndex = 1;
                    Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchison), Snackbar.LENGTH_LONG).show();
                } else {
                    isArSwitchOn = false;
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    if (!mymIsRunningOnKitKat) {
                        videoCameraShutterButton.setVisibility(View.VISIBLE);
                    }
                    mImageDetectionFilterIndex = 0;
                    askForManualPhoto = false;
                    vpIsManuallySelected = false;
                    firstFrameAfterArSwitchOff = true;
                    Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchisoff), Snackbar.LENGTH_LONG).show();
                }
                Log.d(TAG, "isArSwitchOn=" + isArSwitchOn);
            }
        });
        */
        positionCertifiedButton = (FloatingActionButton) findViewById(R.id.positionCertifiedButton);
        timeCertifiedButton = (FloatingActionButton) findViewById(R.id.timeCertifiedButton);
        connectedToServerButton = (FloatingActionButton) findViewById(R.id.connectedToServerButton);

        //callConfigButton = (FloatingActionButton) findViewById(R.id.buttonCallConfig);
        //alphaToggleButton = (FloatingActionButton) findViewById(R.id.buttonAlphaToggle);
        showVpCapturesButton = (FloatingActionButton) findViewById(R.id.buttonShowVpCaptures);

        deleteLocalMediaButton = (FloatingActionButton) findViewById(R.id.deleteLocalMediaButton);
        shareMediaButton = (FloatingActionButton) findViewById(R.id.shareMediaButton);
        shareMediaButton2 = (FloatingActionButton) findViewById(R.id.shareMediaButton2);

        // Camera Shutter Button

        cameraShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Camera Button clicked!!!");
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

        if (!mymIsRunningOnKitKat) {
            // videoCamera Shutter Button

            videoCameraShutterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Video Camera Start Button clicked!!!");
                    askForManualVideo = true;
                    stopManualVideo = false;
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
                    Log.d(TAG, "Video Camera Stop Button clicked!!!");
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
                    videoRecorderTimeLayout.setVisibility(View.GONE);

                }
            });

        }
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
                lastUpdatedOn = " (" + lastUpdatedOn + ")";
                Snackbar.make(view, getText(R.string.position_is_certified) + lastUpdatedOn, Snackbar.LENGTH_LONG).show();
            }
        };

        positionCertifiedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocationUpdated) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zz");
                    String lastUpdatedOn = sdf.format(mLastUpdateTime);
                    lastUpdatedOn = " (" + lastUpdatedOn + ")";
                    Snackbar.make(positionCertifiedButton.getRootView(), getText(R.string.position_is_certified) + lastUpdatedOn, Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.turn_off_location_updates), turnOffClickListenerPositionButton).show();
                } else {
                    Snackbar.make(positionCertifiedButton.getRootView(), getText(R.string.position_not_certified), Snackbar.LENGTH_LONG)
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
                if (isTimeCertified) {
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
                    Snackbar.make(view, getText(R.string.connectedtoserver), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            })
                            .show();
                } else {
                    Snackbar.make(view, getText(R.string.notconnectedtoserver), Snackbar.LENGTH_LONG)
                            .setAction(getText(R.string.trytoconnect), undoOnClickListenerServerButton).show();
                }
            }
        });


        // Show VP captures galley Button

        showVpCapturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isShowingVpPhoto = true;
                Log.d(TAG, "isShowingVpPhoto=" + isShowingVpPhoto);
                uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                positionCertifiedButton.setVisibility(View.INVISIBLE);
                timeCertifiedButton.setVisibility(View.INVISIBLE);
                connectedToServerButton.setVisibility(View.INVISIBLE);
                cameraShutterButton.setVisibility(View.INVISIBLE);
                videoCameraShutterButton.setVisibility(View.INVISIBLE);
                // Setting the correct listview set position
                vpsListView.setVisibility(View.GONE);
                // Turning off tracking
                mImageDetectionFilterIndex = 0;
                //alphaToggleButton.setVisibility(View.GONE);
                showVpCapturesButton.setVisibility(View.GONE);
                //callConfigButton.setVisibility(View.GONE);
                //linearLayoutConfigCaptureVps.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
                showNextVpCaptureButton.setVisibility(View.VISIBLE);
                imageView.resetZoom();
                mediaSelected = -1;
                showVpCaptures(lastVpSelectedByUser);
            }
        });


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
                                if (dismissType == DISMISS_EVENT_TIMEOUT ||
                                        dismissType == DISMISS_EVENT_SWIPE ||
                                        dismissType == DISMISS_EVENT_CONSECUTIVE ||
                                        dismissType == DISMISS_EVENT_MANUAL) {
                                    Log.d(TAG, "deleteLocalMediaButton: File DELETED: dismissType=" + dismissType);
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
                Log.d(TAG, "shareMediaButton:");
                String fileSha256Hash = "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (showingMediaType.equalsIgnoreCase("p")) {
                    try {
                        File inFile = new File(getApplicationContext().getFilesDir(),showingMediaFileName);
                        fileSha256Hash = MymUtils.getFileHash(inFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to hash Photo file to share");
                    }
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Photo shared by MyMensor Mobile App");
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingphotolinkusing)));
                }
                if (showingMediaType.equalsIgnoreCase("v")) {
                    try {
                        File inFile = new File(getApplicationContext().getFilesDir(),showingMediaFileName);
                        fileSha256Hash = MymUtils.getFileHash(inFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton: Failed to hash Video file to share");
                    }
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Video shared by MyMensor Mobile App");
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingvideolinkusing)));
                }
            }
        });


        shareMediaButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "shareMediaButton2:");
                String fileSha256Hash = "";
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (showingMediaType.equalsIgnoreCase("p")) {
                    shareIntent.setType("image/jpg");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                        fileSha256Hash = MymUtils.getFileHash(outFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton2: Failed to copy Photo file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorPhotoCaptureShare.jpg");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(), "com.mymensor.fileprovider", shareFile);
                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, shareFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Media Shared by MyMensor Mobile App - https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/" + fileSha256Hash);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingphotousing)));
                }
                if (showingMediaType.equalsIgnoreCase("v")) {
                    shareIntent.setType("video/*");
                    try {
                        InputStream in = getApplicationContext().openFileInput(showingMediaFileName);
                        File outFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                        OutputStream out = new FileOutputStream(outFile);
                        MymUtils.copyFile(in, out);
                        fileSha256Hash = MymUtils.getFileHash(outFile);
                    } catch (IOException e) {
                        Log.e(TAG, "shareMediaButton2: Failed to copy Video file to share");
                    }
                    File shareFile = new File(getApplicationContext().getFilesDir(), "MyMensorVideoCaptureShare.mp4");
                    Uri shareFileUri = FileProvider.getUriForFile(getApplicationContext(), "com.mymensor.fileprovider", shareFile);
                    List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, shareFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Media Shared by MyMensor Mobile App - https://app.mymensor.com/mc/1/cap/" + mymensorAccount + "/" + showingMediaFileName + "/ " + fileSha256Hash);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getText(R.string.sharingvideousing)));
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "User has put device in airplane mode");
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


    private void checkConnectionToServer() {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "checkConnectionToServer: onPreExecute");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = MymUtils.isS3Available(s3Amazon);
                return result;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                Log.d(TAG, "checkConnectionToServer: onPostExecute: result=" + result);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        //Log.d(TAG, "onLocationChanged: mLastUpdateTime=" + mLastUpdateTime + " mCurrentLocation=" + mCurrentLocation.toString());
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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


    public String[] getLocationToExifStrings(Location location, String photoTakenMillis) {
        String[] locationString = new String[14];
        try {
            double[] gps = new double[2];
            if (location != null) {
                gps[0] = location.getLatitude();
                gps[1] = location.getLongitude();

                if (gps[0] < 0) {
                    locationString[1] = "S";
                    gps[0] = (-1) * gps[0];
                } else {
                    locationString[1] = "N";
                }
                if (gps[1] < 0) {
                    locationString[3] = "W";
                    gps[1] = (-1) * gps[1];
                } else {
                    locationString[3] = "E";
                }
                long latDegInteger = (long) (gps[0] - (gps[0] % 1));
                long latMinInteger = (long) ((60 * (gps[0] - latDegInteger)) - ((60 * (gps[0] - latDegInteger)) % 1));
                long latSecInteger = (long) (((60 * (gps[0] - latDegInteger)) % 1) * 60 * 1000);
                locationString[0] = "" + latDegInteger + "/1," + latMinInteger + "/1," + latSecInteger + "/1000";

                long lonDegInteger = (long) (gps[1] - (gps[1] % 1));
                long lonMinInteger = (long) ((60 * (gps[1] - lonDegInteger)) - ((60 * (gps[1] - lonDegInteger)) % 1));
                long lonSecInteger = (long) (((60 * (gps[1] - lonDegInteger)) % 1) * 60 * 1000);
                locationString[2] = "" + lonDegInteger + "/1," + lonMinInteger + "/1," + lonSecInteger + "/1000";
                locationString[8] = Double.toString(location.getLatitude());
                locationString[9] = Double.toString(location.getLongitude());
                locationString[4] = Float.toString(location.getAccuracy());
                locationString[5] = mLastUpdateTime.toString();
                locationString[6] = location.getProvider();
                locationString[7] = Double.toString(location.getAltitude());
                if (isTimeCertified) {
                    locationString[10] = Integer.toString(1);
                    locationString[11] = photoTakenMillis;
                } else {
                    locationString[10] = Integer.toString(0);
                    locationString[11] = photoTakenMillis;
                }
                if (isPositionCertified) {
                    locationString[12] = Integer.toString(1);
                } else {
                    locationString[12] = Integer.toString(0);
                }
                if (isArSwitchOn) {
                    locationString[13] = Integer.toString(1);
                } else {
                    locationString[13] = Integer.toString(0);
                }
                Log.d(TAG, "getLocationToExifStrings: LAT:" + gps[0] + " " + (gps[0] % 1) + " " + locationString[0] + locationString[1] + " LON:" + gps[1] + " " + locationString[2] + locationString[3]);
            } else {
                locationString[0] = " ";
                locationString[1] = " ";
                locationString[2] = " ";
                locationString[3] = " ";
                locationString[4] = " ";
                locationString[5] = " ";
                locationString[6] = " ";
                locationString[7] = " ";
            }
            for (int index = 0; index < locationString.length; index++) {
                if (locationString[index] == null) locationString[index] = " ";
                Log.d(TAG, "getLocationToExifStrings: locationString[index]=" + locationString[index]);
            }

        } catch (Exception e) {
            Log.d(TAG, "getLocationToExifStrings: failed:" + e.toString());
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
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        Log.d(TAG, "onStart() ********************");

    }


    protected void returnToInitialScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.d(TAG, "returnToInitialScreen");
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                final Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                    Log.d(TAG, "returnToInitialScreen - Calling FULLSCREEN");
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                if (videoView.isPlaying()) videoView.stopPlayback();
                videoView.setZOrderOnTop(false);
                videoView.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                //mCameraView.setVisibility(View.VISIBLE);
                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.GONE);
                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.GONE);
                uploadPendingLinearLayout.setVisibility(View.GONE);
                //linearLayoutVpArStatus.setVisibility(View.GONE);
                mImageDetectionFilterIndex = 1;
                isShowingVpPhoto = false;
                vpLocationDesTextView.setVisibility(View.GONE);
                vpIdNumber.setVisibility(View.GONE);
                //callConfigButton.setVisibility(View.GONE);
                //alphaToggleButton.setVisibility(View.GONE);
                showPreviousVpCaptureButton.setVisibility(View.GONE);
                showNextVpCaptureButton.setVisibility(View.GONE);
                if (buttonStartVideoInVpCaptures.isShown())
                    buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                /*
                // Layout showing VP configuration state
                if (linearLayoutConfigCaptureVps.isShown()) {
                    linearLayoutConfigCaptureVps.setVisibility(View.GONE);
                    linearLayoutVpArStatus.setVisibility(View.GONE);
                    if (linearLayoutMarkerId.isShown()) {
                        linearLayoutMarkerId.setVisibility(View.GONE);
                        linearLayoutAmbiguousVp.setVisibility(View.GONE);
                        buttonAmbiguousVpToggle.setVisibility(View.GONE);
                    }
                    if (linearLayoutSuperSingleVp.isShown()) {
                        linearLayoutSuperSingleVp.setVisibility(View.GONE);
                        buttonSuperSingleVpToggle.setVisibility(View.GONE);
                    }
                }
                */
                showVpCapturesButton.setVisibility(View.VISIBLE);
                cameraShutterButton.setVisibility(View.VISIBLE);
                videoCameraShutterButton.setVisibility(View.VISIBLE);
                vpsListView.setVisibility(View.VISIBLE);
                /*
                // TURNING ON RADAR SCAN
                radarScanImageView.setVisibility(View.VISIBLE);
                radarScanImageView.startAnimation(rotationRadarScan);
                */
                // Turning on control buttons
                if (pendingUploadTransfers > 0)
                    uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                //arSwitchLinearLayout.setVisibility(View.VISIBLE);
                //arSwitch.setVisibility(View.VISIBLE);
                positionCertifiedButton.setVisibility(View.VISIBLE);
                timeCertifiedButton.setVisibility(View.VISIBLE);
                connectedToServerButton.setVisibility(View.VISIBLE);
            }
        });
    }

    ;


    @Override
    public void onBackPressed() {
        //Log.d(TAG, "Testando JNI:" + getSecretKeyFromJNI());
        boolean specialBackClick = false;
        if (isShowingVpPhoto) {
            specialBackClick = true;
            returnToInitialScreen();
        }
        /*
        if (!isArSwitchOn) {
            specialBackClick = true;
            //arSwitch.setChecked(true);
            isArSwitchOn = true;
            cameraShutterButton.setVisibility(View.INVISIBLE);
            if (!mymIsRunningOnKitKat) {
                videoCameraShutterButton.setVisibility(View.INVISIBLE);
                videoCameraShutterStopButton.setVisibility(View.GONE);
            }
            videoRecorderTimeLayout.setVisibility(View.GONE);
            mImageDetectionFilterIndex = 1;
            Snackbar.make(arSwitch.getRootView(), getText(R.string.arswitchison), Snackbar.LENGTH_LONG).show();
        }
        */
        if (!isShowingVpPhoto) {
            if ((backPressed + 2000 > System.currentTimeMillis()) && (!specialBackClick)) {
                super.onBackPressed();
            } else {
                if (!specialBackClick) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(mCameraView, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
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
        Log.d(TAG, "recreate() ********************");
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume CALLED");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
        Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            Log.d(TAG, "onResume - Calling FULLSCREEN");
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

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
                Log.d(TAG, "Observer ID:" + observer.getId() + " key:" + observer.getKey() + " state:" + observer.getState() + " %:" + observer.getBytesTransferred());
                transferUtility.resume(observer.getId());
            }
        }
        updatePendingUpload();

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                    Snackbar.make(vpsListView.getRootView(), getString(R.string.imagecapready), Snackbar.LENGTH_LONG).show();
                }

            }
        });
        returnToInitialScreen();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart CALLED");
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
        Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
            Log.d(TAG, "onRestart - Calling FULLSCREEN");
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
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
    protected void onPause() {
        Log.d(TAG, "onPause CALLED");
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
        if (buttonStartVideoInVpCaptures.isShown())
            buttonStartVideoInVpCaptures.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop CALLED");
        mGoogleApiClient.disconnect();
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        // Dispose of native resources.
        disposeFilters(mImageDetectionFilters);
        this.unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void disposeFilters(Filter[] filters) {
        if (filters != null) {
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
                    mCameraMatrix = MymUtils.getCameraMatrix(cameraWidthInPixels, Constants.cameraHeigthInPixels);
                    mCameraView.enableView();
                    //mCameraView.enableFpsMeter();
                    /*
                    if (!waitingUntilMultipleImageTrackingIsSet) {
                        setMultipleImageTrackingConfiguration();
                    }
                    */

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /*
    private void setSingleImageTrackingConfiguration(int vpIndex) {
        waitingUntilSingleImageTrackingIsSet = true;
        idTrackingIsSet = false;
        markerBufferSingle = new ArrayList<Mat>();
        try {
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + (vpIndex) + ".png");
            Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            markerBufferSingle.add(tmpMarker);
        } catch (Exception e) {
            Log.e(TAG, "setSingleImageTrackingConfiguration(int vpIndex): markerImageFileContents failed:" + e.toString());
        }
        ARFilter trackFilter = null;
        Log.d(TAG, "setSingleImageTrackingConfiguration: markerBufferSingle.toArray().length=" + markerBufferSingle.toArray().length);
        try {
            trackFilter = new ImageDetectionFilter(
                    ImageCapActivity.this,
                    markerBufferSingle.toArray(),
                    1,
                    mCameraMatrix,
                    Constants.standardMarkerlessMarkerWidth);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: " + e.toString());
        }
        if (trackFilter != null) {
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
    */
    /*
    private void setMultipleImageTrackingConfiguration() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        waitingUntilMultipleImageTrackingIsSet = true;
                        Log.d(TAG, "onPreExecute(): setMultipleImageTrackingConfiguration IN BACKGROUND - Lighting Waiting Circle");
                        Log.d(TAG, "waitingUntilMultipleImageTrackingIsSet=" + waitingUntilMultipleImageTrackingIsSet);
                        Log.d(TAG, "multipleImageTrackingIsSet=" + multipleImageTrackingIsSet);
                        Log.d(TAG, "waitingUntilSingleImageTrackingIsSet=" + waitingUntilSingleImageTrackingIsSet);
                        Log.d(TAG, "singleImageTrackingIsSet=" + singleImageTrackingIsSet);
                        Log.d(TAG, "isHudOn=" + isHudOn);
                        mProgress.setVisibility(View.VISIBLE);
                        mProgress.startAnimation(rotationMProgress);
                    }
                });

            }

            @Override
            protected Void doInBackground(Void... params) {
                //mBgr = new Mat();
                markerBuffer = new ArrayList<Mat>();
                for (int i = 1; i < (qtyVps); i++) {
                    try {
                        File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp" + i + ".png");
                        Mat tmpMarker = Imgcodecs.imread(markervpFile.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                        markerBuffer.add(tmpMarker);
                    } catch (Exception e) {
                        Log.e(TAG, "setMultipleImageTrackingConfiguration(): markerImageFileContents failed:" + e.toString());
                    }
                }
                ARFilter trackFilter = null;
                Log.d(TAG, "setMultipleImageTrackingConfiguration: markerBuffer.toArray().length=" + markerBuffer.toArray().length);
                try {
                    trackFilter = new ImageDetectionFilter(
                            ImageCapActivity.this,
                            markerBuffer.toArray(),
                            (qtyVps - 1),
                            mCameraMatrix,
                            Constants.standardMarkerlessMarkerWidth);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load marker: " + e.toString());
                }
                if (trackFilter != null) {
                    mImageDetectionFilters = new ARFilter[]{
                            new NoneARFilter(),
                            trackFilter
                    };
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
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
                        if ((!radarScanImageView.isShown()) && (isArSwitchOn)) {
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
                Log.d(TAG, "onPostExecute: waitingUntilMultipleImageTrackingIsSet=" + waitingUntilMultipleImageTrackingIsSet);
                Log.d(TAG, "multipleImageTrackingIsSet=" + multipleImageTrackingIsSet);
                Log.d(TAG, "waitingUntilSingleImageTrackingIsSet=" + waitingUntilSingleImageTrackingIsSet);
                Log.d(TAG, "singleImageTrackingIsSet=" + singleImageTrackingIsSet);
                Log.d(TAG, "isHudOn=" + isHudOn);
            }
        }.execute();
    }
    */
    /*
    private void setIdTrackingConfiguration() {
        singleImageTrackingIsSet = false;
        ARFilter trackFilter = null;
        try {
            trackFilter = new IdMarkerDetectionFilter(
                    ImageCapActivity.this,
                    1,
                    mCameraMatrix,
                    Constants.idMarkerStdSize);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: " + e.toString());
        }
        if (trackFilter != null) {
            mImageDetectionFilters = new ARFilter[]{
                    new NoneARFilter(),
                    trackFilter
            };
            if (mImageDetectionFilterIndex == 1) {
                idTrackingIsSet = true;
                //idTrackingIsSetMillis = System.currentTimeMillis();
            } else {
                idTrackingIsSet = false;
            }
        }
    }
    */

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @TargetApi(21)
    private boolean prepareVideoRecorder(String videoFileName) {
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
        Log.d(TAG, "onCameraViewStarted CALLED width:" + width + " height:" + height);
    }


    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped CALLED");
    }

    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        if (mymIsRunningOnFlippedDisplay) {
            Core.flip(rgba, rgba, -1);
        }

        //verifyVpsChecked();

        if (!isArSwitchOn) {
            if (!vpIsManuallySelected) vpTrackedInPose = 0;
            final int tmpvpfree = vpTrackedInPose;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                    int lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    if (tmpvpfree < firstVisiblePosition || tmpvpfree > lastVisiblePosition) {
                        //vpsListView.smoothScrollToPosition(tmpvpfree);
                        firstVisiblePosition = vpsListView.getFirstVisiblePosition();
                        lastVisiblePosition = vpsListView.getLastVisiblePosition();
                    }
                    int k = firstVisiblePosition - 1;
                    int i = -1;
                    do {
                        k++;
                        i++;
                        if (k == tmpvpfree) {
                            vpsListView.getChildAt(i).setBackgroundColor(Color.argb(255, 0, 175, 239));
                        } else {
                            vpsListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                        }
                    } while (k < lastVisiblePosition);
                }
            });
        }

        // Start of AR OFF Photo

        if (askForManualPhoto) {
            Log.d(TAG, "Requesting manual photo");
            takePhoto(rgba);
        }

        // End of AR OFF Photo

        // Start of AR OFF Video Recorder

        if ((!isArSwitchOn) && (askForManualVideo) || (!isArSwitchOn) && (capturingManualVideo)) {
            if (askForManualVideo) {
                Log.d(TAG, "A manual video was requested");
                askForManualVideo = false;
                videoCaptureStartmillis = System.currentTimeMillis();
                long momentoLong = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
                photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
                String momento = String.valueOf(momentoLong);
                videoFileName = vpTrackedInPose + "_v_" + momento + ".mp4";
                videoThumbnailFileName = vpTrackedInPose + "_t_" + momento + ".jpg";
                videoFileNameLong = getApplicationContext().getFilesDir() + "/" + videoFileName;
                videoThumbnailFileNameLong = getApplicationContext().getFilesDir() + "/" + videoThumbnailFileName;
                if (!capturingManualVideo) prepareVideoRecorder(videoFileNameLong);
            }
            if (videoRecorderPrepared) {
                if (((System.currentTimeMillis() - videoCaptureStartmillis) < Constants.shortVideoLength) && (!stopManualVideo)) {
                    //Log.d(TAG, "Waiting for video recording to end:" + (System.currentTimeMillis() - videoCaptureStartmillis));
                } else {
                    if (capturingManualVideo) {
                        stopManualVideo = false;
                        capturingManualVideo = false;
                        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                        float volume = actualVolume / maxVolume;
                        if (videoRecordStopedSoundIDLoaded) {
                            soundPool.play(videoRecordStopedSoundID, volume, volume, 1, 0, 1f);
                            Log.d(TAG, "Video STOP: Duartion limit exceeded Played sound");
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recText.clearAnimation();
                                videoCameraShutterButton.setVisibility(View.VISIBLE);
                                videoCameraShutterStopButton.setVisibility(View.GONE);
                                videoRecorderTimeLayout.setVisibility(View.GONE);
                            }
                        });
                        mMediaRecorder.stop();
                        mCameraView.setRecorder(null);
                        videoRecorderPrepared = false;
                        videoRecorderChronometer.stop();
                        releaseMediaRecorder();
                        captureVideo();
                    }
                }
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
            }

        }

        // End of AR OFF Video Recorder
        return rgba;
    }


    @TargetApi(21)
    private void captureVideo() {
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] fileInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.equalsIgnoreCase(videoFileName);
            }
        });

        // Preparing UI for user decision upon capture acceptance
        if ((!vpPhotoAccepted) && (!vpPhotoRejected)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.GONE);
                    uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                    //arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                    //arSwitch.setVisibility(View.INVISIBLE);
                    positionCertifiedButton.setVisibility(View.INVISIBLE);
                    timeCertifiedButton.setVisibility(View.INVISIBLE);
                    connectedToServerButton.setVisibility(View.INVISIBLE);
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    videoCameraShutterButton.setVisibility(View.INVISIBLE);
                    showVpCapturesButton.setVisibility(View.INVISIBLE);
                    /*
                    if (radarScanImageView.isShown()) {
                        radarScanImageView.clearAnimation();
                        radarScanImageView.setVisibility(View.GONE);
                    }
                    */
                    videoView.setVisibility(View.GONE);
                    Uri videoFileTMP = Uri.fromFile(new File(getApplicationContext().getFilesDir(), videoFileName));
                    Log.d(TAG, "media PATH:" + videoFileTMP.getPath());
                    boolean fileNotFound = true;
                    do {
                        try {
                            videoView.setVideoURI(videoFileTMP);
                            fileNotFound = false;
                        } catch (Exception e) {
                            fileNotFound = true;
                        }
                        Log.d(TAG, "Trying Media:" + fileNotFound);
                    } while (fileNotFound);
                    videoView.setZOrderOnTop(true);
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setMediaController(mMediaController);
                    videoView.start();
                    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            videoView.setZOrderOnTop(false);
                            videoView.setVisibility(View.GONE);
                            acceptVpPhotoButton.setVisibility(View.VISIBLE);
                            rejectVpPhotoButton.setVisibility(View.VISIBLE);
                            buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                            buttonReplayVpVideo.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Turned on VIDEO Decision Buttons!!!! captureVideo 1:vpphta:" + vpPhotoAccepted + "vpphtr:" + vpPhotoRejected);
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                            Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                            final Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                            }
                        }
                    });
                }
            });
        }

        do {
            // Waiting for user response
            if (vpVideoTobeReplayed) {
                vpVideoTobeReplayed = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        videoView.setVisibility(View.VISIBLE);
                        videoView.start();
                        videoView.setZOrderOnTop(true);
                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                videoView.setZOrderOnTop(false);
                                videoView.setVisibility(View.GONE);
                                acceptVpPhotoButton.setVisibility(View.VISIBLE);
                                rejectVpPhotoButton.setVisibility(View.VISIBLE);
                                buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                                buttonReplayVpVideo.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Turned on VIDEO Decision Buttons!!!! captureVideo 2");
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                            }
                        });
                    }
                });
            }
            if (vpPhotoTobeRemarked) {
                vpPhotoTobeRemarked = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder alert = new AlertDialog.Builder(ImageCapActivity.this);

                        alert.setTitle(R.string.remark);
                        alert.setMessage(R.string.sizelimit100);

                        // Set an EditText view to get user input
                        final EditText input = new EditText(ImageCapActivity.this);

                        alert.setView(input);

                        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                vpPhotoRemark = input.getText().toString();
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                            }
                        });

                        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                DisplayMetrics metrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(metrics);

                                Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);

                                final Window window = getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                    Log.d(TAG, "captureVideo Acceptance - Calling FULLSCREEN");
                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                }
                                // Canceled.
                            }
                        });

                        alert.show();
                    }
                });
            }
        } while ((!vpPhotoAccepted) && (!vpPhotoRejected));
        vpVideoTobeReplayed = false;
        vpPhotoTobeRemarked = false;
        Log.d(TAG, "takePhoto: LOOP ENDED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);

        if (vpPhotoAccepted) {
            Log.d(TAG, "AROFF Video: vpPhotoAccepted!!!!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.setVisibility(View.GONE);
                    acceptVpPhotoButton.setVisibility(View.GONE);
                    rejectVpPhotoButton.setVisibility(View.GONE);
                    buttonRemarkVpPhoto.setVisibility(View.GONE);
                    buttonReplayVpVideo.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.VISIBLE);
                    // TURNING ON RADAR SCAN
                    if (isArSwitchOn) {
                        //radarScanImageView.setVisibility(View.VISIBLE);
                        //radarScanImageView.startAnimation(rotationRadarScan);

                    }
                    if (!isArSwitchOn) {
                        cameraShutterButton.setVisibility(View.VISIBLE);
                        videoCameraShutterButton.setVisibility(View.VISIBLE);
                    }
                    if (pendingUploadTransfers > 0)
                        uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    //arSwitchLinearLayout.setVisibility(View.VISIBLE);
                    //arSwitch.setVisibility(View.VISIBLE);
                    showVpCapturesButton.setVisibility(View.VISIBLE);
                    positionCertifiedButton.setVisibility(View.VISIBLE);
                    timeCertifiedButton.setVisibility(View.VISIBLE);
                    connectedToServerButton.setVisibility(View.VISIBLE);
                }
            });
            String vpPhotoRemark1000 = null;
            if (vpPhotoRemark != null) {
                vpPhotoRemark1000 = Uri.encode(this.vpPhotoRemark.substring(0, Math.min(this.vpPhotoRemark.length(), 1000)), "@#&=*+-_.,:!?()/~'%");
                vpPhotoRemark = null;
            }
            try {
                if (fileInDirectory != null) {
                    File videoFile = new File(getApplicationContext().getFilesDir(), videoFileName);
                    File pictureVideoThumbnailFile = new File(videoThumbnailFileNameLong);
                    Bitmap videoThumbnailHdBitmap = null;
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    try {
                        mediaMetadataRetriever.setDataSource(videoFileNameLong);
                        videoThumbnailHdBitmap = mediaMetadataRetriever.getFrameAtTime();
                    } catch (IllegalArgumentException iae) {
                        Log.e(TAG, "MediaMetadataRetriever exception: " + iae.toString());
                    } finally {
                        mediaMetadataRetriever.release();
                    }
                    if (videoThumbnailHdBitmap != null) {
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureVideoThumbnailFile);
                            videoThumbnailHdBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap saving to videoThumbnailFileNameLong failed:" + e.toString());
                        }
                    } else {
                        try {
                            InputStream fis = MymUtils.getLocalFile("mymensoremptytn.png", getApplicationContext());
                            if (!(fis == null)) {
                                videoThumbnailHdBitmap = BitmapFactory.decodeStream(fis);
                                fis.close();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap contents from vpLocationDescImageFile0 failed:" + e.toString());
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureVideoThumbnailFile);
                            videoThumbnailHdBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e(TAG, "videoThumbnailHdBitmap saving to videoThumbnailFileNameLong from descvp0 failed:" + e.toString());
                        }
                    }
                    Log.d(TAG, "pictureFile.getName()=" + pictureVideoThumbnailFile.getName());
                    Log.d(TAG, "pictureVideoThumbnailFile.getPath()=" + pictureVideoThumbnailFile.getPath());
                    ObjectMetadata thumbnailMetadata = new ObjectMetadata();
                    Map<String, String> userThumbMetadata = new HashMap<String, String>();
                    userThumbMetadata.put("vp", "" + (vpTrackedInPose));
                    userThumbMetadata.put("mymensoraccount", mymensorAccount);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    thumbnailMetadata.setUserMetadata(userThumbMetadata);
                    //uploading the objects
                    TransferObserver thumbObserver = MymUtils.storeRemoteFile(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + videoThumbnailFileName,
                            Constants.BUCKET_NAME,
                            pictureVideoThumbnailFile,
                            thumbnailMetadata);
                    thumbObserver.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();

                    Log.d(TAG, "videoFile.getName()=" + videoFile.getName());
                    Log.d(TAG, "videoFile.getPath()=" + videoFile.getPath());
                    String fileSha256Hash = MymUtils.getFileHash(videoFile);
                    locPhotoToExif = getLocationToExifStrings(mCurrentLocation, Long.toString(photoTakenTimeMillis[vpTrackedInPose]));
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String, String>();
                    userMetadata.put("loclatitude", locPhotoToExif[8]);
                    userMetadata.put("loclongitude", locPhotoToExif[9]);
                    userMetadata.put("vp", "" + (vpTrackedInPose));
                    userMetadata.put("mymensoraccount", mymensorAccount);
                    userMetadata.put("origmymacc", origMymAcc);
                    userMetadata.put("deviceid", deviceId);
                    userMetadata.put("clitype",Constants.CLIENT_SOFTWARE_TYPE);
                    userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                    userMetadata.put("localtitude", locPhotoToExif[7]);
                    userMetadata.put("locmillis", locPhotoToExif[5]);
                    userMetadata.put("locmethod", locPhotoToExif[6]);
                    userMetadata.put("loccertified", locPhotoToExif[12]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getDefault());
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose]);
                    userMetadata.put("datetime", formattedDateTime);
                    userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                    userMetadata.put("timecertified", locPhotoToExif[10]);
                    userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                    userMetadata.put("sha-256", fileSha256Hash);
                    userMetadata.put("remark", vpPhotoRemark1000);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFileLazy(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + videoFileName,
                            Constants.BUCKET_NAME,
                            videoFile,
                            myObjectMetadata);

                    observer.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();
                    vpPhotoAccepted = false;
                    if (observer != null) {
                        Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getState().toString());
                        Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getAbsoluteFilePath());
                        Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getBucket());
                        Log.d(TAG, "takePhoto: AWS s3 Observer: " + observer.getKey());
                    } else {
                        Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                        //vpChecked[vpTrackedInPose] = false;
                    }
                } else {
                    Log.d(TAG, "Failure to save video to remote storage: videoFile.exists()==false");
                    //vpChecked[vpTrackedInPose] = false;
                }
            } catch (AmazonServiceException ase) {
                Log.e(TAG, "Failure to save video : AmazonServiceException: Error when writing captured image to Remote Storage:" + ase.toString());
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
            } catch (Exception e) {
                Log.e(TAG, "Failure to save video to remote storage:" + e.toString());
                //vpChecked[vpTrackedInPose] = false;
                //waitingToCaptureVpAfterDisambiguationProcedureSuccessful =true;
                e.printStackTrace();
            }
        }

        if (vpPhotoRejected) {
            Log.d(TAG, "AROFF Video: vpPhotoRejected!!!!");
            File videoFile = new File(getApplicationContext().getFilesDir(), videoFileName);
            try {
                if (!videoFile.delete()) {
                    Log.d(TAG, "Rejected video could not be deleted!!!!!!!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while deletion rejected video:" + e.toString());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.setVisibility(View.GONE);
                    //mCameraView.setZOrderOnTop(true);
                    acceptVpPhotoButton.setVisibility(View.GONE);
                    rejectVpPhotoButton.setVisibility(View.GONE);
                    buttonRemarkVpPhoto.setVisibility(View.GONE);
                    buttonReplayVpVideo.setVisibility(View.GONE);
                    vpsListView.setVisibility(View.VISIBLE);
                    // TURNING ON RADAR SCAN
                    if (isArSwitchOn) {
                        //radarScanImageView.setVisibility(View.VISIBLE);
                        //radarScanImageView.startAnimation(rotationRadarScan);

                    }
                    if (!isArSwitchOn) {
                        cameraShutterButton.setVisibility(View.VISIBLE);
                        videoCameraShutterButton.setVisibility(View.VISIBLE);
                    }
                    if (pendingUploadTransfers > 0)
                        uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                    //arSwitchLinearLayout.setVisibility(View.VISIBLE);
                    //arSwitch.setVisibility(View.VISIBLE);
                    showVpCapturesButton.setVisibility(View.VISIBLE);
                    positionCertifiedButton.setVisibility(View.VISIBLE);
                    timeCertifiedButton.setVisibility(View.VISIBLE);
                    connectedToServerButton.setVisibility(View.VISIBLE);
                }
            });
            //vpChecked[vpTrackedInPose] = false;
            lastVpPhotoRejected = true;
            vpPhotoRejected = false;
            vpPhotoRequestInProgress = false;
            Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
            Log.d(TAG, "takePhoto: vpPhotoRejected: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
        }
    }


    private void takePhoto(Mat rgba) {
        Bitmap bitmapImage = null;

        long momentoLong = MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference);
        photoTakenTimeMillis[vpTrackedInPose] = momentoLong;
        if (askForManualPhoto) askForManualPhoto = false;
        final String momento = String.valueOf(momentoLong);
        String pictureFileName;
        pictureFileName = vpTrackedInPose + "_p_" + momento + ".jpg";
        File pictureFile = new File(getApplicationContext().getFilesDir(), pictureFileName);

        Log.d(TAG, "takePhoto: a new camera frame image is delivered " + momento);
        Log.d(TAG, "takePhoto: pictureFileName including account: " + pictureFileName);
        if (rgba != null) {
            bitmapImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgba, bitmapImage);
            final int width = bitmapImage.getWidth();
            final int height = bitmapImage.getHeight();
            Log.d(TAG, "takePhoto: Camera frame width: " + width + " height: " + height);
        }
        if (bitmapImage != null) {
            // Turning tracking OFF
            mImageDetectionFilterIndex = 0;
            // Playing photo capture sound
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            float volume = actualVolume / maxVolume;
            // Is the sound loaded already?
            if (camShutterSoundIDLoaded) {
                soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                Log.d(TAG, "takePhoto: Camera Shutter Played sound");
            }
            // Preparing UI for user decision upon capture acceptance
            if ((!vpPhotoAccepted) && (!vpPhotoRejected)) {
                final Bitmap tmpBitmapImage = bitmapImage;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(tmpBitmapImage);
                        imageView.resetZoom();
                        imageView.setVisibility(View.VISIBLE);
                        if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);
                        acceptVpPhotoButton.setVisibility(View.VISIBLE);
                        rejectVpPhotoButton.setVisibility(View.VISIBLE);
                        buttonRemarkVpPhoto.setVisibility(View.VISIBLE);
                        vpsListView.setVisibility(View.GONE);
                        uploadPendingLinearLayout.setVisibility(View.INVISIBLE);
                        //arSwitchLinearLayout.setVisibility(View.INVISIBLE);
                        //arSwitch.setVisibility(View.INVISIBLE);
                        positionCertifiedButton.setVisibility(View.INVISIBLE);
                        timeCertifiedButton.setVisibility(View.INVISIBLE);
                        connectedToServerButton.setVisibility(View.INVISIBLE);
                        cameraShutterButton.setVisibility(View.INVISIBLE);
                        videoCameraShutterButton.setVisibility(View.INVISIBLE);
                        showVpCapturesButton.setVisibility(View.INVISIBLE);
                    }
                });
            }

            do {
                // Waiting for user response
                if (vpPhotoTobeRemarked) {
                    vpPhotoTobeRemarked = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alert = new AlertDialog.Builder(ImageCapActivity.this);

                            alert.setTitle(R.string.remark);
                            alert.setMessage(R.string.sizelimit100);

                            // Set an EditText view to get user input
                            final EditText input = new EditText(ImageCapActivity.this);

                            alert.setView(input);

                            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    vpPhotoRemark = input.getText().toString();
                                }
                            });

                            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Canceled.
                                }
                            });

                            alert.show();
                        }
                    });
                }
            } while ((!vpPhotoAccepted) && (!vpPhotoRejected));

            Log.d(TAG, "takePhoto: LOOP ENDED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);

            if (vpPhotoAccepted) {
                Log.d(TAG, "takePhoto: vpPhotoAccepted!!!!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        buttonRemarkVpPhoto.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        //vpChecked[vpTrackedInPose] = true;
                        locPhotoToExif = getLocationToExifStrings(mCurrentLocation, momento);
                        if (pendingUploadTransfers > 0)
                            uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        //arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        //arSwitch.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
                        showVpCapturesButton.setVisibility(View.VISIBLE);
                        if (!isArSwitchOn) {
                            cameraShutterButton.setVisibility(View.VISIBLE);
                            videoCameraShutterButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
                String vpPhotoRemark1000 = null;
                try {
                    if (vpPhotoRemark != null) {
                        vpPhotoRemark1000 = Uri.encode(this.vpPhotoRemark.substring(0, Math.min(this.vpPhotoRemark.length(), 1000)), "@#&=*+-_.,:!?()/~'%");
                        vpPhotoRemark = null;
                    }
                } catch (Exception e) {
                    vpPhotoRemark = null;
                }

                try {
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
                    Map<String, String> userMetadata = new HashMap<String, String>();
                    userMetadata.put("loclatitude", locPhotoToExif[8]);
                    userMetadata.put("loclongitude", locPhotoToExif[9]);
                    userMetadata.put("vp", "" + (vpTrackedInPose));
                    userMetadata.put("mymensoraccount", mymensorAccount);
                    userMetadata.put("origmymacc", origMymAcc);
                    userMetadata.put("deviceid", deviceId);
                    userMetadata.put("clitype",Constants.CLIENT_SOFTWARE_TYPE);
                    userMetadata.put("locprecisioninm", locPhotoToExif[4]);
                    userMetadata.put("localtitude", locPhotoToExif[7]);
                    userMetadata.put("locmillis", locPhotoToExif[5]);
                    userMetadata.put("locmethod", locPhotoToExif[6]);
                    userMetadata.put("loccertified", locPhotoToExif[12]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    sdf.setTimeZone(TimeZone.getDefault());
                    String formattedDateTime = sdf.format(photoTakenTimeMillis[vpTrackedInPose]);
                    userMetadata.put("datetime", formattedDateTime);
                    userMetadata.put("phototakenmillis", locPhotoToExif[11]);
                    userMetadata.put("timecertified", locPhotoToExif[10]);
                    userMetadata.put("isarswitchOn", locPhotoToExif[13]);
                    userMetadata.put("sha-256", fileSha256Hash);
                    userMetadata.put("remark", vpPhotoRemark1000);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFile(
                            transferUtility,
                            Constants.capturesFolder + "/" + mymensorAccount + "/" + pictureFileName,
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);

                    observer.setTransferListener(new UploadListener());
                    pendingUploadTransfers++;
                    updatePendingUpload();

                } catch (AmazonServiceException ase) {
                    Log.e(TAG, "takePhoto: AmazonServiceException: Error when writing captured image to Remote Storage:" + ase.toString());
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
                } catch (Exception e) {
                    Log.e(TAG, "takePhoto: Error when writing captured image to Remote Storage:" + e.toString());
                    //vpChecked[vpTrackedInPose] = false;
                }
                vpPhotoAccepted = false;
                vpPhotoRequestInProgress = false;
                Log.d(TAG, "takePhoto: vpPhotoAccepted: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
            }

            if (vpPhotoRejected) {
                Log.d(TAG, "takePhoto: vpPhotoRejected!!!!");
                try {
                    if (pictureFile.delete()) {
                        Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> " + pictureFile.getName() + " deleted successfully");
                    }
                    ;

                } catch (Exception e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(128);
                        imageView.setVisibility(View.GONE);
                        acceptVpPhotoButton.setVisibility(View.GONE);
                        rejectVpPhotoButton.setVisibility(View.GONE);
                        buttonRemarkVpPhoto.setVisibility(View.GONE);
                        vpsListView.setVisibility(View.VISIBLE);
                        // TURNING ON RADAR SCAN
                        if (isArSwitchOn) {
                            //radarScanImageView.setVisibility(View.VISIBLE);
                            //radarScanImageView.startAnimation(rotationRadarScan);

                        }
                        if (!isArSwitchOn) {
                            cameraShutterButton.setVisibility(View.VISIBLE);
                            videoCameraShutterButton.setVisibility(View.VISIBLE);
                        }
                        if (pendingUploadTransfers > 0)
                            uploadPendingLinearLayout.setVisibility(View.VISIBLE);
                        //arSwitchLinearLayout.setVisibility(View.VISIBLE);
                        //arSwitch.setVisibility(View.VISIBLE);
                        showVpCapturesButton.setVisibility(View.VISIBLE);
                        positionCertifiedButton.setVisibility(View.VISIBLE);
                        timeCertifiedButton.setVisibility(View.VISIBLE);
                        connectedToServerButton.setVisibility(View.VISIBLE);
                    }
                });
                lastVpPhotoRejected = true;
                vpPhotoRejected = false;
                vpPhotoRequestInProgress = false;
                Log.d(TAG, "takePhoto: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
                Log.d(TAG, "takePhoto: vpPhotoRejected: vpPhotoRequestInProgress = " + vpPhotoRequestInProgress);
            }
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View view, final int position, long id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgress.isShown()) {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
        lastVpSelectedByUser = position;
        if (!isArSwitchOn) {
            vpTrackedInPose = position;
            vpIsManuallySelected = true;
        }

    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.buttonAcceptVpPhoto) {
            vpPhotoAccepted = true;
            Log.d(TAG, "vpPhotoAccepted BUTTON PRESSED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);
        }
        if (v.getId() == R.id.buttonRejectVpPhoto) {
            vpPhotoRejected = true;
            Log.d(TAG, "vpPhotoRejected BUTTON PRESSED: vpPhotoAccepted:" + vpPhotoAccepted + " vpPhotoRejected:" + vpPhotoRejected);
        }
        if (v.getId() == R.id.buttonRemarkVpPhoto) {
            vpPhotoTobeRemarked = true;
            Log.d(TAG, "buttonRemarkVpPhoto BUTTON PRESSED");
        }
        if (v.getId() == R.id.buttonReplayVpVideo) {
            vpVideoTobeReplayed = true;
            Log.d(TAG, "buttonReplayVpVideo BUTTON PRESSED");
        }
        if (v.getId() == R.id.buttonShowPreviousVpCapture) {
            mediaSelected++;
            showVpCaptures(lastVpSelectedByUser);
        }
        if (v.getId() == R.id.buttonShowNextVpCapture) {
            mediaSelected--;
            showVpCaptures(lastVpSelectedByUser);
        }

    }


    private void deleteLocalShownCapture(int vpSelected, final View view) {
        Log.d(TAG, "deleteLocalShownCapture: vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser);
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(vpToList + "_");
            }
        });
        int numOfEntries = 0;
        try {
            if (!(capsInDirectory == null)) {
                vpMediaFileName = capsInDirectory[mediaSelected];
                Log.d(TAG, "deleteLocalShownCapture: vpMediaFileName=" + path + "/" + vpMediaFileName);
                File fileToBeDeleted = new File(path + "/" + vpMediaFileName);
                if (fileToBeDeleted.delete()) {
                    Log.d(TAG, "deleteLocalShownCapture: vpMediaFileName=" + path + "/" + vpMediaFileName + " succesfully deleted from local storage.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = getString(R.string.local_file_deleted);
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
                ;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while deleting captures:" + e.toString());
        }

    }


    private void getRemoteFileMetadata(final String filename) {

        new AsyncTask<Void, Void, ObjectMetadata>() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "getRemoteFileMetadata: onPreExecute");
            }

            @Override
            protected ObjectMetadata doInBackground(Void... params) {
                int retries = 4;
                boolean nthtry = false;
                try {
                    do {
                        final ObjectMetadata objMetadata = s3Amazon.getObjectMetadata(Constants.BUCKET_NAME, filename);
                        try {
                            if (objMetadata.getContentLength() > 5) nthtry = true;
                        } catch (Exception ex) {
                            nthtry = false;
                        }
                        if (nthtry) {
                            Log.d(TAG, "Request to s3Amazon.getObjectMetadata succeeded");
                            return objMetadata;
                        } else {
                            Log.d(TAG, "Request to s3Amazon.getObjectMetadata failed or object does not exist");
                        }
                    } while (retries-- > 0);
                } catch (AmazonServiceException ase) {
                    Log.d(TAG, "AmazonServiceException=" + ase.toString());
                    return null;
                } catch (AmazonClientException ace) {
                    Log.d(TAG, "AmazonClientException=" + ace.toString());
                    return null;
                } catch (Exception e) {
                    Log.d(TAG, "Exception=" + e.toString());
                    return null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(final ObjectMetadata objectMetadata) {
                Log.d(TAG, "getRemoteFileMetadata: onPostExecute");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (objectMetadata != null) {
                            Map<String, String> userMetadata = new HashMap<String, String>();
                            userMetadata = objectMetadata.getUserMetadata();
                            Log.d(TAG, "userMetadata=" + userMetadata.toString());
                            Log.d(TAG, "Location=LocCertified=" + userMetadata.get("loccertified") + " Time=TimeCertified=" + userMetadata.get("timecertified"));
                            if (userMetadata.get("loccertified").equalsIgnoreCase("1")) {
                                //IsPositionCertified
                                positionCertifiedImageview.setVisibility(View.VISIBLE);
                                positionCertifiedImageview.setBackground(circularButtonGreen);
                            } else {
                                positionCertifiedImageview.setVisibility(View.VISIBLE);
                                positionCertifiedImageview.setBackground(circularButtonRed);
                            }
                            if (userMetadata.get("timecertified").equalsIgnoreCase("1")) {
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


    private void showVpCaptures(int vpSelected) {
        final Bitmap showVpPhotoImageFileContents;
        final Bitmap showVpVideoThumbImageFileContents;
        Log.d(TAG, "vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser);
        final int position = vpSelected;
        final int vpToList = vpSelected;
        final String vpMediaFileName;
        final String path = getApplicationContext().getFilesDir().getPath();
        File directory = new File(path);
        String[] capsInDirectory = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(vpToList + "_p") || filename.startsWith(vpToList + "_v");
            }
        });
        int numOfEntries = 0;
        try {
            if (capsInDirectory.length > 0) {
                numOfEntries = capsInDirectory.length;
                if (mediaSelected == -1) mediaSelected = numOfEntries - 1;
                if (mediaSelected < 0) mediaSelected = 0;
                if (mediaSelected > (numOfEntries - 1)) mediaSelected = 0;
                Log.d(TAG, "SHOWVPCAPTURES: vpSelected=" + vpSelected + " lastVpSelectedByUser=" + lastVpSelectedByUser + " mediaSelected=" + mediaSelected);
                vpMediaFileName = capsInDirectory[mediaSelected];
                Log.d(TAG, "SHOWVPCAPTURES: vpMediaFileName=" + vpMediaFileName);
                showingMediaFileName = vpMediaFileName;
                Log.d(TAG, "showVpCaptures: vpMediaFileName=" + vpMediaFileName);
                StringBuilder sb = new StringBuilder(vpMediaFileName);
                final String millisMoment = sb.substring(vpMediaFileName.length() - 17, vpMediaFileName.length() - 4);
                final String mediaType = sb.substring(vpMediaFileName.length() - 19, vpMediaFileName.length() - 18);
                showingMediaType = mediaType;
                if (mediaType.equalsIgnoreCase("p")) {
                    // When the item is a photo
                    final InputStream fiscaps = MymUtils.getLocalFile(vpMediaFileName, getApplicationContext());
                    showVpPhotoImageFileContents = BitmapFactory.decodeStream(fiscaps);
                    fiscaps.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!(showVpPhotoImageFileContents == null)) {
                                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                try {
                                    ExifInterface tags = new ExifInterface(path + "/" + vpMediaFileName);
                                    Log.d(TAG, "Location=Make=" + tags.getAttribute("Make") + " Time=GPSAltitudeRef=" + tags.getAttribute("GPSAltitudeRef"));
                                    if (tags.getAttribute("Make").equalsIgnoreCase("1")) {
                                        //IsPositionCertified
                                        positionCertifiedImageview.setVisibility(View.VISIBLE);
                                        positionCertifiedImageview.setBackground(circularButtonGreen);
                                    } else {
                                        positionCertifiedImageview.setVisibility(View.VISIBLE);
                                        positionCertifiedImageview.setBackground(circularButtonRed);
                                    }
                                    if (tags.getAttribute("GPSAltitudeRef").equalsIgnoreCase("1")) {
                                        //IsTimeCertified
                                        timeCertifiedImageview.setVisibility(View.VISIBLE);
                                        timeCertifiedImageview.setBackground(circularButtonGreen);
                                    } else {
                                        timeCertifiedImageview.setVisibility(View.VISIBLE);
                                        timeCertifiedImageview.setBackground(circularButtonRed);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Problem with Exif tags or drawable setting:" + e.toString());
                                }
                                videoView.setVisibility(View.GONE);
                                if (buttonStartVideoInVpCaptures.isShown()) {
                                    buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                                }
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageBitmap(showVpPhotoImageFileContents);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);
                                String lastTimeAcquired = "";
                                Date lastDate = new Date(Long.parseLong(millisMoment));
                                SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd-MMM-yyyy HH:mm:ss zz"));
                                sdf.setTimeZone(TimeZone.getDefault());
                                String formattedLastDate = sdf.format(lastDate);
                                lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " + formattedLastDate;
                                vpLocationDesTextView.setText(getString(R.string.vp_name) + lastVpSelectedByUser + ": " + lastTimeAcquired);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    if (mediaType.equalsIgnoreCase("v")) {
                        // when the item is a video.
                        try {
                            getRemoteFileMetadata(Constants.capturesFolder + "/" + mymensorAccount + "/" + vpMediaFileName);
                        } catch (Exception e) {
                            Log.e(TAG, "Problem Remote files Metadata:" + e.toString());
                        }
                        String lastTimeAcquired = "";
                        Date lastDate = new Date(Long.parseLong(millisMoment));
                        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dd-MMM-yyyy HH:mm:ss zz"));
                        sdf.setTimeZone(TimeZone.getDefault());
                        String formattedLastDate = sdf.format(lastDate);
                        lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " + formattedLastDate;
                        final String desTextView = getString(R.string.vp_name) + lastVpSelectedByUser + ": "  + lastTimeAcquired;
                        final Uri videoFileTMP = Uri.fromFile(new File(getApplicationContext().getFilesDir(), vpMediaFileName));
                        final InputStream fisthumbs = MymUtils.getLocalFile(vpSelected + "_t_" + millisMoment + ".jpg", getApplicationContext());
                        showVpVideoThumbImageFileContents = BitmapFactory.decodeStream(fisthumbs);
                        fisthumbs.close();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                videoView.setVisibility(View.GONE);
                                imageView.setVisibility(View.VISIBLE);
                                imageView.setImageBitmap(showVpVideoThumbImageFileContents);
                                imageView.resetZoom();
                                if (imageView.getImageAlpha() == 128) imageView.setImageAlpha(255);

                                buttonStartVideoInVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutButtonsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                linearLayoutImageViewsOnShowVpCaptures.setVisibility(View.VISIBLE);
                                positionCertifiedImageview.setVisibility(View.GONE);
                                timeCertifiedImageview.setVisibility(View.GONE);
                                vpLocationDesTextView.setText(desTextView);
                                vpLocationDesTextView.setVisibility(View.VISIBLE);

                                buttonStartVideoInVpCaptures.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        buttonStartVideoInVpCaptures.setVisibility(View.GONE);
                                        videoView.setVisibility(View.VISIBLE);
                                        videoView.setVideoURI(videoFileTMP);
                                        videoView.setMediaController(mMediaController);
                                        videoView.start();
                                        videoView.setZOrderOnTop(true);
                                        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                            @Override
                                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                                String message = getString(R.string.error_while_playing_video);
                                                Snackbar.make(videoView, message, Snackbar.LENGTH_LONG).show();
                                                returnToInitialScreen();
                                                return false;
                                            }
                                        });
                                        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mp) {
                                                videoView.setZOrderOnTop(false);
                                                videoView.setVisibility(View.GONE);
                                                Log.d(TAG, "onCompletion Listener VIDEO showVpCaptures");
                                                buttonStartVideoInVpCaptures.setVisibility(View.VISIBLE);
                                                DisplayMetrics metrics = new DisplayMetrics();
                                                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                                                Log.d(TAG, "SCRRES Display Width (Pixels):" + metrics.widthPixels);
                                                Log.d(TAG, "SCRRES Display Heigth (Pixels):" + metrics.heightPixels);
                                                final Window window = getWindow();
                                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                if (((metrics.widthPixels) * (metrics.heightPixels)) <= 921600) {
                                                    Log.d(TAG, "showVpCaptures - Calling FULLSCREEN");
                                                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                }
            } else {
                //when no item has been acquired to the vp.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = getString(R.string.no_photo_captured_in_this_vp);
                        Snackbar.make(imageView, message, Snackbar.LENGTH_LONG).show();
                        returnToInitialScreen();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while retrieving captures:" + e.toString());
        }
    }


    private void callTimeServerInBackground() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Void doInBackground(Void... params) {
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
            protected void onPostExecute(Void result) {
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
            if (newState.equals(TransferState.COMPLETED)) {
                pendingUploadTransfers--;
                if (pendingUploadTransfers < 0) pendingUploadTransfers = 0;
            }
            updatePendingUpload();
        }
    }

    /*
     * Updates the ListView according to the observers.
     */
    private void updatePendingUpload() {

        if (pendingUploadTransfers == 0) {
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
