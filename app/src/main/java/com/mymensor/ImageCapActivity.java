package com.mymensor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.MotionEvent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.imgproc.Imgproc;
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


import static com.mymensor.Constants.cameraWidthInPixels;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_64FC1;


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
    public int photoSelected = 0;

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

    private long idTrackingIsSetMillis;

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

    TextView recText;

    Animation rotationRadarScan;
    Animation rotationMProgress;
    Animation blinkingText;

    FloatingActionButton callConfigButton;
    FloatingActionButton alphaToggleButton;
    FloatingActionButton showVpCapturesButton;
    Button showPreviousVpCaptureButton;
    Button showNextVpCaptureButton;
    Button acceptVpPhotoButton;
    Button rejectVpPhotoButton;

    LinearLayout arSwitchLinearLayout;
    LinearLayout videoRecorderTimeLayout;

    Chronometer videoRecorderChronometer;

    Switch arSwitch;

    private boolean isArSwitchOn = true;

    FloatingActionButton positionCertifiedButton;
    FloatingActionButton timeCertifiedButton;
    FloatingActionButton connectedToServerButton;
    FloatingActionButton cameraShutterButton;
    FloatingActionButton videoCameraShutterButton;
    FloatingActionButton videoCameraShutterStopButton;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

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

    private String videoFileName;
    private String videoFileNameLong;

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

    MatOfPoint3f objectPoints;
    MatOfPoint2f imagePoints;

    @TargetApi(21)
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

        Log.d(TAG,"onCreate: Starting ImageCapActivity with qtyVps="+qtyVps);

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

        blinkingText = AnimationUtils.loadAnimation(this, R.anim.textblink);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        vpCheckedView = (ImageView) this.findViewById(R.id.imageViewVpChecked);
        vpCheckedView.setVisibility(View.GONE);

        arSwitchLinearLayout = (LinearLayout) this.findViewById(R.id.arSwitchLinearLayout);

        videoRecorderTimeLayout = (LinearLayout) this.findViewById(R.id.videoRecorderTimeLayout);

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
                    Snackbar.make(vpsListView.getRootView(),getString(R.string.imagecapready), Snackbar.LENGTH_LONG).show();
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
        if (mMediaRecorder!=null){
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
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
                Log.d(TAG,"markerBuffer.toArray().length="+markerBuffer.toArray().length);
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
                idTrackingIsSetMillis = System.currentTimeMillis();
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

    @TargetApi(21)
    private boolean prepareVideoRecorder(String videoFileName){
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        profile.videoFrameWidth = Constants.cameraWidthInPixels;
        profile.videoFrameHeight = Constants.cameraHeigthInPixels;
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

    @TargetApi(21)
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
                String momento = String.valueOf(momentoLong);
                videoFileName = "cap_vid_" + mymensorAccount + "_" + vpNumber[vpTrackedInPose] + "_" + momento + ".mp4";
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
                            userMetadata.put("SHA-256", fileSha256Hash);
                            //call setUserMetadata on our ObjectMetadata object, passing it our map
                            myObjectMetadata.setUserMetadata(userMetadata);
                            //uploading the objects
                            TransferObserver observer = MymUtils.storeRemoteFileLazy(
                                    transferUtility,
                                    "cap/"+videoFileName,
                                    Constants.BUCKET_NAME,
                                    videoFile,
                                    myObjectMetadata);
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

                                    float multiplyFactor = 1f;

                                    float deltaX = (trackingValues.getXid() - vpXCameraDistance[vpTrackedInPose])*multiplyFactor;
                                    float deltaY = (trackingValues.getYid() - vpYCameraDistance[vpTrackedInPose])*multiplyFactor;
                                    float deltaZ = (trackingValues.getZ() - vpZCameraDistance[vpTrackedInPose])*multiplyFactor;

                                    float deltaRX = trackingValues.getEAX() - vpXCameraRotation[vpTrackedInPose];
                                    float deltaRY = trackingValues.getEAY() - vpYCameraRotation[vpTrackedInPose];
                                    float deltaRZ = trackingValues.getEAZ() - vpZCameraRotation[vpTrackedInPose];




                                    double rotZ = deltaRY;


                                    double xp1 = (double)Constants.xAxisTrackingCorrection+deltaX+deltaZ;
                                    double yp1 = (double)Constants.yAxisTrackingCorrection+deltaY+deltaZ;

                                    double xp2 = (double)Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth+deltaX-deltaZ;
                                    double yp2 = (double)Constants.yAxisTrackingCorrection+deltaY+deltaZ;

                                    double xp3 = (double)(Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth+deltaX-deltaZ);
                                    double yp3 = (double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth+deltaY-deltaZ);

                                    double xp4 = (double)(Constants.xAxisTrackingCorrection+deltaX+deltaZ);
                                    double yp4 = (double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth+deltaY-deltaZ);

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
                    Log.d(TAG,"Else 1: INVALID VP TRACKED IN POSE");
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
                //Log.d(TAG,"Else 2="+singleImageTrackingIsSet);
                if (singleImageTrackingIsSet){
                    singleImageTrackingIsSet = false;
                    setMultipleImageTrackingConfiguration();
                }
                if (isHudOn==0) isHudOn=1;
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
                    String fileSha256Hash = MymUtils.getFileHash(pictureFile);
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
                    userMetadata.put("SHA-256", fileSha256Hash);
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
                    // Activate Location Description Buttons
                    callConfigButton.setVisibility(View.VISIBLE);
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
