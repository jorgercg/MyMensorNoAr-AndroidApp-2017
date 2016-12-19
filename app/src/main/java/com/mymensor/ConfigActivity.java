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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.mymensor.filters.NoneARFilter;
import com.mymensor.filters.VpConfigFilter;

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
import org.opencv.imgproc.Imgproc;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static android.R.color.holo_blue_bright;
import static com.mymensor.R.drawable.border_marker_id_blue;
import static com.mymensor.R.drawable.border_marker_id_red;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigActivity extends Activity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        AdapterView.OnItemClickListener {

    private static final String TAG = "ConfigActivity";

    private static long back_pressed;

    private String descvpRemotePath;
    private String markervpRemotePath;
    private String vpsRemotePath;

    private short qtyVps = 0;
    private short vpIndex;

    private String mymensorAccount;
    private int dciNumber;

    private boolean[] vpChecked;
    private boolean[] vpAcquired;

    private boolean drawTargetFrame = false;
    private boolean cameraShutterButtonClicked = false;
    private boolean isShowingVpPhoto = false;
    private boolean vpWasConfigured = false;

    private boolean idTrackingIsSet = false;
    private boolean waitingUntilIdTrackingIsSet = false;

    private short[] vpNumber;
    private String[] vpLocationDesText;
    private boolean[] vpArIsConfigured;
    private boolean[] vpIsVideo;
    private int[] vpXCameraDistance;
    private int[] vpYCameraDistance;
    private int[] vpZCameraDistance;
    private int[] vpXCameraRotation;
    private int[] vpYCameraRotation;
    private int[] vpZCameraRotation;
    private short[] vpMarkerlessMarkerWidth;
    private short[] vpMarkerlessMarkerHeigth;
    private boolean[] vpIsAmbiguous;
    private boolean[] vpFlashTorchIsOn;
    private boolean[] vpIsSuperSingle;
    private int[] vpSuperMarkerId;
    private String[] vpFrequencyUnit;
    private long[] vpFrequencyValue;
    private static Bitmap vpLocationDescImageFileContents;

    private int markerIdInPose;

    private static float tolerancePosition;
    private static float toleranceRotation;

    private short assetId;
    private String frequencyUnit;
    private int frequencyValue;

    ListView vpsListView;
    ImageView mProgress;
    TouchImageView imageView;

    FloatingActionButton cameraShutterButton;

    EditText vpLocationDesEditTextView;
    TextView vpIdNumber;
    TextView vpAcquiredStatus;
    TextView vpIdMarkerUsedTextView;
    TextView idMarkerNumberTextView;

    Animation rotationMProgress;

    Button acceptVpPhotoButton;
    Button rejectVpPhotoButton;

    FloatingActionButton requestPhotoButton;
    FloatingActionButton ambiguousVpToggle;
    FloatingActionButton superSingleVpToggle;

    FloatingActionButton increaseQtyVps;
    FloatingActionButton decreaseQtyVps;

    LinearLayout qtyVpsLinearLayout;
    LinearLayout linearLayoutCaptureNewVp;
    LinearLayout linearLayoutAmbiguousVp;
    LinearLayout linearLayoutSuperSingleVp;
    LinearLayout linearLayoutConfigCaptureVps;
    LinearLayout linearLayoutVpArStatus;
    LinearLayout linearLayoutMarkerId;

    FloatingActionButton buttonCallImagecap;


    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean isTimeCertified;
    private long acquisitionStartTime;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether the next camera frame should be saved as a photo and other boolean controllers
    private boolean cameraPhotoRequested;
    private boolean vpDescAndMarkerImageOK = false;
    private boolean doCheckPositionToTarget;
    private boolean vpSuperMarkerIdFound;
    private boolean waitingForVpSuperMarkerIdTrackingAcquisition = false;
    private boolean waitingForTrackingAcquisition = false;
    private boolean trackingConfigDone = false;
    private boolean vpIsDisambiguated = false;

    // The filters.
    private ARFilter[] mVpConfigureFilters;

    // The indices of the active filters.
    private int mVpConfigureFilterIndex;

    // The index of the active camera.
    private int mCameraIndex;

    // Whether the active camera is front-facing.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;

    // The number of cameras on the device.
    private int mNumCameras;

    // The image sizes supported by the active camera.
    private List<Camera.Size> mSupportedImageSizes;

    // The index of the active image size.
    private int mImageSizeIndex;

    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX =
            "imageSizeIndex";

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX =
            "imageDetectionFilterIndex";

    // Whether an asynchronous menu action is in progress.
    // If so, menu interaction should be disabled.
    private boolean mIsMenuLocked;

    // Matrix to hold camera calibration
    // initially with absolute compute values
    private MatOfDouble mCameraMatrix;

    Point pt1;
    Point pt2;
    Point pt3;
    Point pt4;
    Point pt5;
    Point pt6;
    Scalar color;

    private SoundPool.Builder soundPoolBuilder;
    private SoundPool soundPool;
    private int camShutterSoundID;
    private int errorLowToneID;
    private int videoRecordStartedSoundID;
    private int videoRecordStopedSoundID;
    boolean camShutterSoundIDLoaded = false;
    boolean errorLowToneIDLoaded = false;
    boolean videoRecordStartedSoundIDLoaded = false;
    boolean videoRecordStopedSoundIDLoaded = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_config);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        mymensorAccount = getIntent().getExtras().get("mymensoraccount").toString();
        dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        isTimeCertified = Boolean.parseBoolean(getIntent().getExtras().get("isTimeCertified").toString());

        descvpRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/";
        markervpRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"mrk"+"/";
        vpsRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
            mVpConfigureFilterIndex = savedInstanceState.getInt(STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mVpConfigureFilterIndex = 0;
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
                if (i==errorLowToneID) errorLowToneIDLoaded=true;
                if (i==videoRecordStartedSoundID) videoRecordStartedSoundIDLoaded=true;
                if (i==videoRecordStopedSoundID) videoRecordStopedSoundIDLoaded=true;
            }
        });

        camShutterSoundID = soundPool.load(this, R.raw.camerashutter,1);
        errorLowToneID = soundPool.load(this, R.raw.errorlowtonedescend,1);
        videoRecordStartedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimeup, 1);
        videoRecordStopedSoundID = soundPool.load(this, R.raw.minidvcamerabeepchimedown, 1);


        final Camera camera;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIndex, cameraInfo);
        mIsCameraFrontFacing =
                (cameraInfo.facing ==
                        Camera.CameraInfo.CAMERA_FACING_FRONT);
        mNumCameras = Camera.getNumberOfCameras();
        camera = Camera.open(mCameraIndex);

        final Camera.Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.config_javaCameraView);
        mCameraView.setCameraIndex(mCameraIndex);
        mCameraView.setMaxFrameSize(Constants.cameraWidthInPixels, Constants.cameraHeigthInPixels);
        mCameraView.setCvCameraViewListener(this);

        Camera.Size tmpCamProjSize = mSupportedImageSizes.get(mImageSizeIndex);;

        tmpCamProjSize.width = Constants.cameraWidthInPixels;
        tmpCamProjSize.height = Constants.cameraHeigthInPixels;

        final Camera.Size camProjSize = tmpCamProjSize;

        loadConfigurationFile();

        mVpConfigureFilterIndex = 1;

        String[] newVpsList = new String[qtyVps];

        for (int i=0; i<(qtyVps); i++)
        {
            if (i==0) {
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

        vpLocationDesEditTextView = (EditText) this.findViewById(R.id.descVPEditText);

        vpLocationDesEditTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus){
                    saveVpsData();
                }
            }
        });

        vpIdNumber = (TextView) this.findViewById(R.id.textView2);

        idMarkerNumberTextView = (TextView) findViewById(R.id.idMarkerNumberTextView);

        vpIdMarkerUsedTextView = (TextView) findViewById(R.id.vpIdMarkerUsedTextView);

        vpAcquiredStatus = (TextView) this.findViewById(R.id.vpAcquiredStatus);

        cameraShutterButton = (FloatingActionButton) findViewById(R.id.cameraShutterButton);

        requestPhotoButton = (FloatingActionButton) findViewById(R.id.buttonRequestPhoto);

        increaseQtyVps = (FloatingActionButton) findViewById(R.id.buttonIncreaseQtyVps);
        decreaseQtyVps = (FloatingActionButton) findViewById(R.id.buttonDecreaseQtyVps);

        ambiguousVpToggle = (FloatingActionButton) findViewById(R.id.buttonAmbiguousVpToggle);
        superSingleVpToggle= (FloatingActionButton) findViewById(R.id.buttonSuperSingleVpToggle);

        acceptVpPhotoButton = (Button) this.findViewById(R.id.buttonAcceptVpPhoto);
        rejectVpPhotoButton = (Button) this.findViewById(R.id.buttonRejectVpPhoto);

        mProgress = (ImageView) this.findViewById(R.id.waitingTrkLoading);
        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);

        pt1 = new Point((double)Constants.xAxisTrackingCorrection,(double)Constants.yAxisTrackingCorrection);
        pt2 = new Point((double)(Constants.xAxisTrackingCorrection+Constants.standardMarkerlessMarkerWidth),(double)(Constants.yAxisTrackingCorrection+Constants.standardMarkerlessMarkerHeigth));
        pt3 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)),(double)Constants.yAxisTrackingCorrection);
        pt4 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)),(double)(Constants.yAxisTrackingCorrection-40));
        pt5 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)-20),(double)(Constants.yAxisTrackingCorrection)-20);
        pt6 = new Point((double)(Constants.xAxisTrackingCorrection+(Constants.standardMarkerlessMarkerWidth/2)+20),(double)(Constants.yAxisTrackingCorrection)-20);
        color = new Scalar((double)0,(double)175,(double)239);

        qtyVpsLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutQtyVps);
        linearLayoutCaptureNewVp = (LinearLayout) findViewById(R.id.linearLayoutCaptureNewVp);
        linearLayoutAmbiguousVp = (LinearLayout) findViewById(R.id.linearLayoutAmbiguousVp);
        linearLayoutSuperSingleVp = (LinearLayout) findViewById(R.id.linearLayoutSuperSingleVp);
        linearLayoutConfigCaptureVps = (LinearLayout) findViewById(R.id.linearLayoutConfigCaptureVps);
        linearLayoutVpArStatus = (LinearLayout) findViewById(R.id.linearLayoutVpArStatus);
        linearLayoutMarkerId = (LinearLayout) findViewById(R.id.linearLayoutMarkerId);

        buttonCallImagecap = (FloatingActionButton) findViewById(R.id.buttonCallImagecap);

        // Call Config Button

        final View.OnClickListener confirmOnClickListenerButtonCallImagecap = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Snackbar.make(view, getText(R.string.callingimagecapactivity), Snackbar.LENGTH_LONG).show();

                callImageCapActivity();

            }
        };

        buttonCallImagecap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getText(R.string.confirmimagecaploading), Snackbar.LENGTH_LONG)
                        .setAction(getText(R.string.confirm), confirmOnClickListenerButtonCallImagecap).show();
            }
        });

        increaseQtyVps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qtyVps++;
                if (qtyVps>Constants.maxQtyVps) qtyVps = Constants.maxQtyVps;
                increaseQtyOfVps(qtyVps);
            }
        });

        decreaseQtyVps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qtyVps--;
                if (qtyVps<=2) qtyVps = 2;
                decreaseQtyOfVps(qtyVps);
            }
        });

        requestPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vpLocationDesEditTextView.setVisibility(View.GONE);
                vpIdNumber.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                requestPhotoButton.setVisibility(View.INVISIBLE);
                qtyVpsLinearLayout.setVisibility(View.INVISIBLE);
                buttonCallImagecap.setVisibility(View.INVISIBLE);
                linearLayoutCaptureNewVp.setVisibility(View.INVISIBLE);
                linearLayoutConfigCaptureVps.setVisibility(View.INVISIBLE);
                linearLayoutAmbiguousVp.setVisibility(View.INVISIBLE);
                linearLayoutSuperSingleVp.setVisibility(View.INVISIBLE);
                ambiguousVpToggle.setVisibility(View.INVISIBLE);
                superSingleVpToggle.setVisibility(View.INVISIBLE);
                cameraPhotoRequested = true;
                doCheckPositionToTarget=false;
                vpSuperMarkerIdFound = false;
                vpAcquiredStatus.setText(R.string.vpNotAcquiredStatus);
                Snackbar.make(requestPhotoButton.getRootView(),getString(R.string.takephoto), Snackbar.LENGTH_LONG).show();
                drawTargetFrame = true;
                if (vpIsAmbiguous[vpIndex]){
                    waitingUntilIdTrackingIsSet = true;
                    vpIsDisambiguated = false;
                    mVpConfigureFilterIndex = 1;
                    setIdTrackingConfiguration();
                } else {
                    waitingUntilIdTrackingIsSet = false;
                    vpIsDisambiguated = false;
                    mVpConfigureFilterIndex = 0;
                    //setIdTrackingConfiguration();
                }
            }
        });

        ambiguousVpToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vpIsAmbiguous[vpIndex])
                {
                    vpIsAmbiguous[vpIndex] = false;
                }
                else
                {
                    vpIsAmbiguous[vpIndex] = true;
                }
                if (vpIsAmbiguous[vpIndex]) ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                if (!vpIsAmbiguous[vpIndex])
                {
                    if (!vpIsSuperSingle[vpIndex])
                    {
                        ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    }
                    else
                    {
                        vpIsAmbiguous[vpIndex] = true;
                        ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                    }
                }
            }
        });

        superSingleVpToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vpIsSuperSingle[vpIndex]) {
                    vpIsSuperSingle[vpIndex] = false;
                }
                else
                {
                    vpIsSuperSingle[vpIndex] = true;
                }
                if (vpIsSuperSingle[vpIndex])
                {
                    superSingleVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                    if (!vpIsAmbiguous[vpIndex])
                    {
                        vpIsAmbiguous[vpIndex] = true;
                        ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                    }
                }
                if (!vpIsSuperSingle[vpIndex])
                {
                    superSingleVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                }
            }
        });


    } // End of OnCreate


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // Save the current image size index.
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        // Save the current filter indices.
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mVpConfigureFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onStart()
    {
        super.onStart();


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

        mIsMenuLocked = false;
        //if (mGoogleApiClient.isConnected()) startLocationUpdates();
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                    Snackbar.make(vpsListView.getRootView(),getString(R.string.configready), Snackbar.LENGTH_LONG).show();

                }

            }
        });
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

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void setIdTrackingConfiguration(){
        waitingUntilIdTrackingIsSet = true;
        ARFilter trackFilter = null;
        try {
            trackFilter = new IdMarkerDetectionFilter(
                    ConfigActivity.this,
                    1,
                    mCameraMatrix,
                    Constants.idMarkerStdSize);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load marker: "+e.toString());
        }
        if (trackFilter!=null){
            mVpConfigureFilters = new ARFilter[] {
                    new NoneARFilter(),
                    trackFilter
            };
            if (mVpConfigureFilterIndex==1){
                idTrackingIsSet = true;
                waitingUntilIdTrackingIsSet = false;
            } else {
                idTrackingIsSet = false;
            }

        }
    }


    private void configureTracking(Mat referenceImage){

        ARFilter trackFilter = null;
        try {
            Log.d(TAG," configureTracking(): DONE 1 ");
            trackFilter = new VpConfigFilter(
                    ConfigActivity.this,
                    referenceImage,
                    mCameraMatrix, Constants.standardMarkerlessMarkerWidth);
            trackingConfigDone = true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to configure tracking:"+ e.toString());
            trackingConfigDone = false;
        }
        if (trackFilter!=null){
            mVpConfigureFilters = new ARFilter[] {
                    new NoneARFilter(),
                    trackFilter
            };
            acquisitionStartTime = System.currentTimeMillis();
            if (!trackingConfigDone) trackingConfigDone=true;
            Log.d(TAG," configureTracking(): DONE ="+acquisitionStartTime);
        } else {
            Log.e(TAG," configureTracking(): FAILED ");
        }

    }

    @TargetApi(21)
    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        final Mat rgba = inputFrame.rgba();
        float[] trckValues;

        if (cameraShutterButtonClicked) {
            drawTargetFrame =false;
            if (vpIsSuperSingle[vpIndex]) waitingForVpSuperMarkerIdTrackingAcquisition = true;
        }

        if (drawTargetFrame){
            Imgproc.rectangle(rgba,pt1,pt2,color,8);
            Imgproc.line(rgba,pt3,pt4,color,8);
            Imgproc.line(rgba,pt4,pt5,color,8);
            Imgproc.line(rgba,pt4,pt6,color,8);
        }

        /*
        ImageTrackingAcquisition - Image Tracking
         */

        if ((mVpConfigureFilters != null) && waitingForTrackingAcquisition && (!vpIsSuperSingle[vpIndex])) {
            mVpConfigureFilterIndex = 1;
            mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba, 1, 0);
            trckValues = mVpConfigureFilters[mVpConfigureFilterIndex].getPose();
            Log.d(TAG,"(ImageTrackingAcquisition: trckValues!=null)="+(trckValues!=null));
            if (trckValues!=null){
                Log.d(TAG,"ImageTrackingAcquisition: (System.currentTimeMillis()-acquisitionStartTime)="+(System.currentTimeMillis()-acquisitionStartTime));
                Log.d(TAG,"ImageTrackingAcquisition: trckValues: Translations = "+trckValues[0]+" | "+trckValues[1]+" | "+trckValues[2]);
                Log.d(TAG,"ImageTrackingAcquisition: trckValues: Rotations = "+trckValues[3]*(180.0f/Math.PI)+" | "+trckValues[4]*(180.0f/Math.PI)+" | "+trckValues[5]*(180.0f/Math.PI));
                vpXCameraDistance[vpIndex] = Math.round(trckValues[0])+Constants.xAxisTrackingCorrection;
                vpYCameraDistance[vpIndex] = Math.round(trckValues[1])+Constants.yAxisTrackingCorrection;
                vpZCameraDistance[vpIndex] = Math.round(trckValues[2]);
                vpXCameraRotation[vpIndex] = (int) Math.round(trckValues[3]*(180.0f/Math.PI));
                vpYCameraRotation[vpIndex] = (int) Math.round(trckValues[4]*(180.0f/Math.PI));
                vpZCameraRotation[vpIndex] = (int) Math.round(trckValues[5]*(180.0f/Math.PI));
                vpAcquired[vpIndex]=true;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                    }
                });
                Log.d(TAG, "ImageTrackingAcquisition: onCameraFrame:Setting to true: VpAcquired: ["+(vpIndex)+"] = "+vpAcquired[vpIndex]);
                vpChecked[vpIndex]=true;
                setVpsChecked();
                saveVpsData();
                if (vpDescAndMarkerImageOK){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(mCameraView,getString(R.string.vp_capture_success), Snackbar.LENGTH_LONG).show();
                        }
                    });
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    float volume = actualVolume / maxVolume;
                    if (camShutterSoundIDLoaded) {
                        soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                    }
                }
                Log.d(TAG,"ImageTrackingAcquisition: waitingForTrackingAcquisition="+waitingForTrackingAcquisition);
                waitingForTrackingAcquisition = false;
                trackingConfigDone = false;
            } else {
                if (vpDescAndMarkerImageOK && ((System.currentTimeMillis()-acquisitionStartTime)>3000)) {
                    Log.d(TAG, "ImageTrackingAcquisition: (System.currentTimeMillis()-acquisitionStartTime)=" + (System.currentTimeMillis() - acquisitionStartTime));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(mCameraView,getString(R.string.vp_acquisition_failure), Snackbar.LENGTH_LONG).show();
                        }
                    });
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    float volume = actualVolume / maxVolume;
                    if (errorLowToneIDLoaded) {
                        soundPool.play(errorLowToneID, volume, volume, 1, 0, 1f);
                    }
                    // Giving up tracking acquisition as it is taking too long>>>> need to change marker or method
                    Log.d(TAG, "ImageTrackingAcquisition: waitingForTrackingAcquisition=" + waitingForTrackingAcquisition);
                    vpAcquired[vpIndex]=false;
                    waitingForTrackingAcquisition = false;
                    trackingConfigDone = false;
                    vpChecked[vpIndex]=false;
                    setVpsChecked();
                }
            }
        }


        /*
        vpIsAmbiguous - Ambiguos Image Tracking - Using Marker Ids for disambiguation
         */

        if ((mVpConfigureFilters != null) && idTrackingIsSet && vpIsAmbiguous[vpIndex] && (!vpIsSuperSingle[vpIndex])) {
            mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba, 1, 0);
            trckValues = mVpConfigureFilters[mVpConfigureFilterIndex].getPose();
            Log.d(TAG,"vpIsAmbiguous: (trckValues!=null)="+(trckValues!=null));
            if (trckValues!=null) {
                Log.d(TAG, "vpIsAmbiguous: trckValues: vpSuperMarkerId = " + trckValues[6]);
                Log.d(TAG, "vpIsAmbiguous: trckValues: Translations = " + trckValues[0] + " | " + trckValues[1] + " | " + trckValues[2]);
                Log.d(TAG, "vpIsAmbiguous: trckValues: Rotations = " + trckValues[3] * (180.0f / Math.PI) + " | " + trckValues[4] * (180.0f / Math.PI) + " | " + trckValues[5] * (180.0f / Math.PI));
                vpXCameraDistance[vpIndex] = Math.round(trckValues[0]);
                vpYCameraDistance[vpIndex] = Math.round(trckValues[1]);
                vpZCameraDistance[vpIndex] = Math.round(trckValues[2]);
                vpXCameraRotation[vpIndex] = (int) Math.round(trckValues[3] * (180.0f / Math.PI));
                vpYCameraRotation[vpIndex] = (int) Math.round(trckValues[4] * (180.0f / Math.PI));
                vpZCameraRotation[vpIndex] = (int) Math.round(trckValues[5] * (180.0f / Math.PI));
                markerIdInPose = (int) Math.round(trckValues[6]);
                boolean isMarkerIdInPose = false;
                int markerIdInPose_inner = 0;
                int k_inner = 0;
                for (int j=0; j < Constants.validIdMarkersForMyMensor.length; j++) {
                    if (Constants.validIdMarkersForMyMensor[j] == markerIdInPose) {
                        markerIdInPose_inner = markerIdInPose;
                        isMarkerIdInPose = true;
                    }
                }
                if (isMarkerIdInPose) {
                    boolean wasMarkerIdAlreadyUsed = false;
                    for (int k=1; k < (qtyVps); k++) {
                        Log.d(TAG, "vpIsAmbiguous: vpSuperMarkerId[" + k + "]=" + vpSuperMarkerId[k]);
                        if ((vpSuperMarkerId[k] == markerIdInPose) && (k != vpIndex)) {
                            k_inner = k;
                            wasMarkerIdAlreadyUsed = true;
                        }
                    }
                    final int k_inner_final = k_inner;
                    if (wasMarkerIdAlreadyUsed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                linearLayoutMarkerId.setVisibility(View.VISIBLE);
                                linearLayoutMarkerId.setBackgroundDrawable(getDrawable(border_marker_id_red));
                                idMarkerNumberTextView.setText(Integer.toString(markerIdInPose));
                                vpIdMarkerUsedTextView.setVisibility(View.VISIBLE);
                                vpIdMarkerUsedTextView.setText("@ VP#"+k_inner_final);
                                cameraShutterButton.setEnabled(false);
                                cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                            }
                        });
                    } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    linearLayoutMarkerId.setVisibility(View.VISIBLE);
                                    linearLayoutMarkerId.setBackgroundDrawable(getDrawable(border_marker_id_blue));
                                    idMarkerNumberTextView.setText(Integer.toString(markerIdInPose));
                                    vpIdMarkerUsedTextView.setVisibility(View.GONE);
                                    cameraShutterButton.setEnabled(true);
                                    cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_bright)));
                                }
                            });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                            idMarkerNumberTextView.setText("--");
                            vpIdMarkerUsedTextView.setVisibility(View.GONE);
                            cameraShutterButton.setEnabled(false);
                            cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                        }
                    });
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                        idMarkerNumberTextView.setText("--");
                        vpIdMarkerUsedTextView.setVisibility(View.GONE);
                        cameraShutterButton.setEnabled(false);
                        cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    }
                });
            }
        }

        /*
        vpIsSuperSingle - Super Ambiguos Image Tracking - Using Marker Ids for tracking
         */

        if ((mVpConfigureFilters != null) && idTrackingIsSet && vpIsAmbiguous[vpIndex] && vpIsSuperSingle[vpIndex]) {
            mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba, 1, 0);
            trckValues = mVpConfigureFilters[mVpConfigureFilterIndex].getPose();
            Log.d(TAG,"vpIsSuperSingle: (trckValues!=null)="+(trckValues!=null));
            if (trckValues!=null) {
                Log.d(TAG, "vpIsSuperSingle: trckValues: vpSuperMarkerId = " + trckValues[6]);
                Log.d(TAG, "vpIsSuperSingle: trckValues: Translations = " + trckValues[0] + " | " + trckValues[1] + " | " + trckValues[2]);
                Log.d(TAG, "vpIsSuperSingle: trckValues: Rotations = " + trckValues[3] * (180.0f / Math.PI) + " | " + trckValues[4] * (180.0f / Math.PI) + " | " + trckValues[5] * (180.0f / Math.PI));
                vpXCameraDistance[vpIndex] = Math.round(trckValues[0]);
                vpYCameraDistance[vpIndex] = Math.round(trckValues[1]);
                vpZCameraDistance[vpIndex] = Math.round(trckValues[2]);
                vpXCameraRotation[vpIndex] = (int) Math.round(trckValues[3] * (180.0f / Math.PI));
                vpYCameraRotation[vpIndex] = (int) Math.round(trckValues[4] * (180.0f / Math.PI));
                vpZCameraRotation[vpIndex] = (int) Math.round(trckValues[5] * (180.0f / Math.PI));
                markerIdInPose = (int) Math.round(trckValues[6]);
                boolean isMarkerIdInPose = false;
                int markerIdInPose_inner = 0;
                int k_inner = 0;
                for (int j=0; j < Constants.validIdMarkersForMyMensor.length; j++) {
                    if (Constants.validIdMarkersForMyMensor[j] == markerIdInPose) {
                        markerIdInPose_inner = markerIdInPose;
                        isMarkerIdInPose = true;
                    }
                }
                if (isMarkerIdInPose) {
                    boolean wasMarkerIdAlreadyUsed = false;
                    for (int k=1; k < (qtyVps); k++) {
                        Log.d(TAG, "vpIsSuperSingle: vpSuperMarkerId[" + k + "]=" + vpSuperMarkerId[k]);
                        if ((vpSuperMarkerId[k] == markerIdInPose_inner) && (k != vpIndex)) {
                            k_inner = k;
                            wasMarkerIdAlreadyUsed = true;
                        }
                    }
                    final int k_inner_final = k_inner;
                    if (wasMarkerIdAlreadyUsed){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                linearLayoutMarkerId.setVisibility(View.VISIBLE);
                                linearLayoutMarkerId.setBackgroundDrawable(getDrawable(border_marker_id_red));
                                idMarkerNumberTextView.setText(Integer.toString(markerIdInPose));
                                vpIdMarkerUsedTextView.setVisibility(View.VISIBLE);
                                vpIdMarkerUsedTextView.setText("@ VP#"+k_inner_final);
                                cameraShutterButton.setEnabled(false);
                                cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                            }
                        });
                    } else {
                        Log.d(TAG,"vpIsSuperSingle: markerIdInPose vpSuperMarkerId["+vpIndex+"]="+markerIdInPose);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                linearLayoutMarkerId.setVisibility(View.VISIBLE);
                                linearLayoutMarkerId.setBackgroundDrawable(getDrawable(border_marker_id_blue));
                                idMarkerNumberTextView.setText(Integer.toString(markerIdInPose));
                                vpIdMarkerUsedTextView.setVisibility(View.GONE);
                                cameraShutterButton.setEnabled(true);
                                cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_bright)));
                            }
                        });
                    }
                    if (waitingForVpSuperMarkerIdTrackingAcquisition){
                        Log.d(TAG,"vpIsSuperSingle: waitingForVpSuperMarkerIdTrackingAcquisition="+waitingForVpSuperMarkerIdTrackingAcquisition);
                        vpAcquired[vpIndex]=true;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                            }
                        });
                        Log.d(TAG, "vpIsSuperSingle: Setting to true: VpAcquired: ["+(vpIndex)+"] = "+vpAcquired[vpIndex]);
                        vpChecked[vpIndex]=true;
                        setVpsChecked();
                        saveVpsData();
                        waitingForVpSuperMarkerIdTrackingAcquisition = false;
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                            idMarkerNumberTextView.setText("--");
                            vpIdMarkerUsedTextView.setVisibility(View.GONE);
                            cameraShutterButton.setEnabled(false);
                            cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                        }
                    });
                }
            } else {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                        idMarkerNumberTextView.setText("--");
                        vpIdMarkerUsedTextView.setVisibility(View.GONE);
                        cameraShutterButton.setEnabled(false);
                        cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    }
                });
            }
        }
        if (cameraPhotoRequested) {
            cameraPhotoRequested = false;
            runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    linearLayoutConfigCaptureVps.setVisibility(View.VISIBLE);
                    cameraShutterButton.setVisibility(View.VISIBLE);
                    drawTargetFrame = true;
                    cameraShutterButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "Camera Button clicked!!!");
                            cameraShutterButtonClicked = true;
                            //idTrackingIsSet = false;
                        }
                    });
                }
            });
        }

        if ((cameraShutterButtonClicked)&&(!drawTargetFrame)){
            cameraShutterButtonClicked = false;
            takePhoto(rgba);
        }

        return rgba;
    }

    private void takePhoto(final Mat rgba) {
        try {
            if (isShowingVpPhoto) isShowingVpPhoto=false;
            if (idTrackingIsSet) idTrackingIsSet = false;
            mBgr = new Mat();
            // bitmap to descvp and markervp
            Bitmap bitmapImage = null;
            Bitmap markerFromBitmapImage = null;
            // creating temp local storage files
            File pictureFile = new File(getApplicationContext().getFilesDir(), "descvp"+vpIndex+".png");
            File markerFile = new File(getApplicationContext().getFilesDir(), "markervp"+vpIndex+".png");
            if(rgba != null)
            {
                // getting bitmap from cameraframe
                Log.d(TAG, "takePhoto: a new camera frame image was delivered");
                bitmapImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgba,bitmapImage);
                final int width = bitmapImage.getWidth();
                final int height = bitmapImage.getHeight();
                int markerWidthLocal = 0;
                int markerHeightLocal = 0;
                if (!vpIsSuperSingle[vpIndex]) {
                    if (vpMarkerlessMarkerWidth[vpIndex]>1) {
                        markerWidthLocal = vpMarkerlessMarkerWidth[vpIndex];
                    } else {
                        markerWidthLocal = Constants.standardMarkerlessMarkerWidth;
                    }
                    if (vpMarkerlessMarkerHeigth[vpIndex]>1) {
                        markerHeightLocal = vpMarkerlessMarkerHeigth[vpIndex];
                    } else {
                        markerHeightLocal = Constants.standardMarkerlessMarkerHeigth;
                    }
                } else {
                    markerWidthLocal = Constants.standardMarkerlessMarkerWidth;
                    markerHeightLocal = Constants.standardMarkerlessMarkerHeigth;
                }
                if (vpMarkerlessMarkerWidth[vpIndex]>width) markerWidthLocal=width;
                if (vpMarkerlessMarkerHeigth[vpIndex]>height) markerHeightLocal=height;
                int x = (width - markerWidthLocal)/2;
                int y = (height - markerHeightLocal)/2;
                // getting marker from bitmap, centering the marker in the original bitmap
                markerFromBitmapImage = Bitmap.createBitmap(bitmapImage, x, y, markerWidthLocal, markerHeightLocal);
                //markerFromBitmapImage = greyScaler(markerFromBitmapImage);
                markerFromBitmapImage = markerFromBitmapImage.createScaledBitmap(markerFromBitmapImage, Constants.captureMarkerWidth, Constants.captureMarkerHeight, false);
                Utils.bitmapToMat(markerFromBitmapImage,mBgr);
                Imgproc.cvtColor(mBgr, mBgr, Imgproc.COLOR_BGR2GRAY);
                Utils.matToBitmap(mBgr, markerFromBitmapImage);
                Log.d(TAG, "Camera frame width: "+width+" height: "+height);
            }
            if (pictureFile == null)
            {
                Log.e(TAG, "Error creating PICTURE media file, check storage permissions. ");
                return;
            }
            if (markerFile == null)
            {
                Log.e(TAG, "Error creating MARKER media file, check storage permissions. ");
                return;
            }
            FileOutputStream fos_d = new FileOutputStream(pictureFile);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos_d);
            fos_d.close();
            FileOutputStream fos_m = new FileOutputStream(markerFile);
            markerFromBitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos_m);
            fos_m.close();
            // if marker from bitmap image is ok and not a super VP then save it to remote storage
            if (markerFromBitmapImage != null)
            {
                // Set the tracking configuration using the markerFile currently in the app data folder
                if (vpIsSuperSingle[vpIndex]) {
                    vpSuperMarkerId[vpIndex]=markerIdInPose;
                    Bitmap superMarkerBitmapImage = BitmapFactory.decodeResource(getResources(), R.drawable.mymensormarker);
                    superMarkerBitmapImage = superMarkerBitmapImage.createScaledBitmap(superMarkerBitmapImage, Constants.captureMarkerWidth, Constants.captureMarkerHeight, false);
                    markerFile = new File(getApplicationContext().getFilesDir(), "markervp"+vpIndex+".png");
                    FileOutputStream fos_msvp = new FileOutputStream(markerFile);
                    superMarkerBitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos_msvp);
                    fos_m.close();
                    //setSLAMTrackingConfig();
                    saveVpsData();
                    Log.d(TAG, "takePhoto: turning off tracking as vp is Super and thus tracking is acquired already.");
                    mVpConfigureFilterIndex = 0;
                    mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba, 0, 0);
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                    float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    float volume = actualVolume / maxVolume;
                    if (camShutterSoundIDLoaded) {
                        soundPool.play(camShutterSoundID, volume, volume, 1, 0, 1f);
                    }
                    //waitingForVpSuperMarkerId=true;
                    //setSuperIdMarkersTrackingConfig();
                    //setSuperSingleIdMarkerTrackingConfig();
                } else {
                    Log.d(TAG, "takePhoto: calling configureTracking() with the acquired image marker");
                    Log.d(TAG, "waitingForTrackingAcquisition=" + waitingForTrackingAcquisition);
                    Log.d(TAG, "trackingConfigDone=" + trackingConfigDone);
                    vpSuperMarkerId[vpIndex]=markerIdInPose;
                    waitingForTrackingAcquisition = true;
                    if (!trackingConfigDone) configureTracking(mBgr);
                    mVpConfigureFilterIndex = 1;
                    mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba, 0, 0);
                }
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String, String>();
                userMetadata.put("VP", "" + (vpIndex));
                userMetadata.put("mymensorAccount", mymensorAccount);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedDateTime = sdf.format(MymUtils.timeNow(isTimeCertified, sntpTime, sntpTimeReference));
                userMetadata.put("DateTime", formattedDateTime);
                //call setUserMetadata on our ObjectMetadata object, passing it our map
                myObjectMetadata.setUserMetadata(userMetadata);
                //uploading the objects
                TransferObserver observer = MymUtils.storeRemoteFile(
                        transferUtility,
                        markervpRemotePath + markerFile.getName(),
                        Constants.BUCKET_NAME,
                        markerFile,
                        myObjectMetadata);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"takephoto(): Markervp TransferListener="+state.toString());
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
                        Log.e(TAG, "takephoto(): Markervp saving failed:"+ ex.toString());
                    }

                });

            }
            // if bitmapimage is OK it is saved to remote storage
            if (bitmapImage != null)
            {
                cameraPhotoRequested =false;
                vpDescAndMarkerImageOK = true;
                vpArIsConfigured[vpIndex]=true;
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String,String>();
                userMetadata.put("VP", ""+(vpIndex));
                userMetadata.put("mymensorAccount", mymensorAccount);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedDateTime = sdf.format(MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference));
                userMetadata.put("DateTime", formattedDateTime);
                //call setUserMetadata on our ObjectMetadata object, passing it our map
                myObjectMetadata.setUserMetadata(userMetadata);
                //uploading the objects
                TransferObserver observer = MymUtils.storeRemoteFile(
                        transferUtility,
                        descvpRemotePath+pictureFile.getName(),
                        Constants.BUCKET_NAME,
                        pictureFile,
                        myObjectMetadata);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"takephoto(): Descvp TransferListener="+state.toString());
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
                        Log.e(TAG, "takephoto(): Descvp saving failed:"+ ex.toString());
                    }

                });
            }
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (cameraShutterButton.isShown()) {
                        cameraShutterButton.setVisibility(View.INVISIBLE);
                        drawTargetFrame = false;
                    }
                }
            });

        } catch (Exception e){
            vpChecked[vpIndex] = false;
            vpArIsConfigured[vpIndex]=false;
            setVpsChecked();
            mIsMenuLocked = false;
            cameraPhotoRequested =true;
            vpDescAndMarkerImageOK = false;
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(mCameraView,getString(R.string.vp_capture_failure), Snackbar.LENGTH_LONG).show();
                }
            });
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            float volume = actualVolume / maxVolume;
            if (errorLowToneIDLoaded) {
                soundPool.play(errorLowToneID, volume, volume, 1, 0, 1f);
            }
        }

        vpWasConfigured = true;
        returnToInitialScreen();
    }


    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }


    @Override
    public void onCameraViewStopped() {
    }


    protected void returnToInitialScreen(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isShowingVpPhoto = false;
                vpWasConfigured=false;
                drawTargetFrame = false;
                vpLocationDesEditTextView.setVisibility(View.GONE);
                vpIdNumber.setVisibility(View.GONE);
                vpsListView.setVisibility(View.VISIBLE);
                linearLayoutVpArStatus.setVisibility(View.INVISIBLE);
                vpAcquiredStatus.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.GONE);
                requestPhotoButton.setVisibility(View.INVISIBLE);
                qtyVpsLinearLayout.setVisibility(View.VISIBLE);
                buttonCallImagecap.setVisibility(View.VISIBLE);
                linearLayoutCaptureNewVp.setVisibility(View.INVISIBLE);
                linearLayoutConfigCaptureVps.setVisibility(View.INVISIBLE);
                linearLayoutAmbiguousVp.setVisibility(View.INVISIBLE);
                linearLayoutSuperSingleVp.setVisibility(View.INVISIBLE);
                ambiguousVpToggle.setVisibility(View.INVISIBLE);
                superSingleVpToggle.setVisibility(View.INVISIBLE);
                linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                if (cameraShutterButton.isShown()) {
                    cameraShutterButton.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onBackPressed: turning off tracking.");
                    mVpConfigureFilterIndex = 0;
                    setIdTrackingConfiguration();
                }
                idTrackingIsSet = false;
            }
        });
    };


    @Override
    public void onBackPressed()
    {
        if (isShowingVpPhoto){
            returnToInitialScreen();
        } else {
            if (back_pressed + 2000 > System.currentTimeMillis())
                super.onBackPressed();
            else
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(mCameraView, getString(R.string.double_bck_exit), Snackbar.LENGTH_LONG).show();
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
        finish();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG,"onStop CALLED");
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
        disposeFilters(mVpConfigureFilters);
        super.onDestroy();
    }

    private void disposeFilters(Filter[] filters) {
        if (filters!=null) {
            for (Filter filter : filters) {
                filter.dispose();
            }
        }
    }


    private void setVpsChecked()
    {
        try
        {
            // set the checked state of the vp items
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int i=0; i<(qtyVps); i++)
                    {
                        vpsListView.setItemChecked(i, vpChecked[i]);
                    }
                }
            });
        }
        catch (Exception e)
        {
            Log.e(TAG , "SetVpsChecked failed: "+e.toString());
        }
    }


    private void saveVpsData()
    {
        // Saving Vps Data configuration.
        Log.d(TAG,"qtyVps="+qtyVps);
        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","VpsData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","AssetId");
            xmlSerializer.text(Short.toString(assetId));
            xmlSerializer.endTag("","AssetId");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyUnit");
            xmlSerializer.text(frequencyUnit);
            xmlSerializer.endTag("","FrequencyUnit");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyValue");
            xmlSerializer.text(Integer.toString(frequencyValue));
            xmlSerializer.endTag("","FrequencyValue");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","QtyVps");
            xmlSerializer.text(Short.toString(qtyVps));
            xmlSerializer.endTag("","QtyVps");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TolerancePosition");
            xmlSerializer.text(Float.toString(tolerancePosition));
            xmlSerializer.endTag("","TolerancePosition");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ToleranceRotation");
            xmlSerializer.text(Float.toString(toleranceRotation));
            xmlSerializer.endTag("","ToleranceRotation");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=0; i<(qtyVps); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("","VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpArIsConfigured");
                xmlSerializer.text(Boolean.toString(vpArIsConfigured[i]));
                xmlSerializer.endTag("","VpArIsConfigured");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsVideo");
                xmlSerializer.text(Boolean.toString(vpIsVideo[i]));
                xmlSerializer.endTag("","VpIsVideo");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraDistance");
                xmlSerializer.text(Integer.toString(vpXCameraDistance[i]));
                xmlSerializer.endTag("","VpXCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraDistance");
                xmlSerializer.text(Integer.toString(vpYCameraDistance[i]));
                xmlSerializer.endTag("","VpYCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraDistance");
                xmlSerializer.text(Integer.toString(vpZCameraDistance[i]));
                xmlSerializer.endTag("","VpZCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraRotation");
                xmlSerializer.text(Integer.toString(vpXCameraRotation[i]));
                xmlSerializer.endTag("","VpXCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraRotation");
                xmlSerializer.text(Integer.toString(vpYCameraRotation[i]));
                xmlSerializer.endTag("","VpYCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraRotation");
                xmlSerializer.text(Integer.toString(vpZCameraRotation[i]));
                xmlSerializer.endTag("","VpZCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpLocDescription");
                xmlSerializer.text(vpLocationDesText[i]);
                xmlSerializer.endTag("","VpLocDescription");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerWidth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerHeigth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsAmbiguous");
                xmlSerializer.text(Boolean.toString(vpIsAmbiguous[i]));
                xmlSerializer.endTag("","VpIsAmbiguous");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFlashTorchIsOn");
                xmlSerializer.text(Boolean.toString(vpFlashTorchIsOn[i]));
                xmlSerializer.endTag("","VpFlashTorchIsOn");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsSuperSingle");
                xmlSerializer.text(Boolean.toString(vpIsSuperSingle[i]));
                xmlSerializer.endTag("","VpIsSuperSingle");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperMarkerId");
                if (vpIsAmbiguous[i])
                {
                    xmlSerializer.text(Integer.toString(vpSuperMarkerId[i]));
                }
                else
                {
                    xmlSerializer.text(Integer.toString(0));
                }
                xmlSerializer.endTag("","VpSuperMarkerId");
                if (!(vpFrequencyUnit[i].equalsIgnoreCase(frequencyUnit)))
                {
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("","VpFrequencyUnit");
                    xmlSerializer.text(vpFrequencyUnit[i]);
                    xmlSerializer.endTag("","VpFrequencyUnit");
                }
                if (vpFrequencyValue[i]!=frequencyValue)
                {
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("","VpFrequencyValue");
                    xmlSerializer.text(Long.toString(vpFrequencyValue[i]));
                    xmlSerializer.endTag("","VpFrequencyValue");
                }
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Vp");
            }
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","VpsData");
            xmlSerializer.endDocument();
            String vpsConfigFileContents = writer.toString();
            File vpsConfigFile = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);
            FileUtils.writeStringToFile(vpsConfigFile,vpsConfigFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", MymUtils.timeNow(isTimeCertified,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsRemotePath + Constants.vpsConfigFileName), Constants.BUCKET_NAME, vpsConfigFile, myObjectMetadata);
            observer.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (state.equals(TransferState.COMPLETED)) {
                        Log.d(TAG,"TransferListener="+state.toString());
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
                    Log.e(TAG, "saveVpsData(): vpsConfigFile saving failed, see stack trace"+ ex.toString());
                }

            });

        }
        catch (Exception e)
        {
            Log.e(TAG, "saveVpsData(): failed, see stack trace: "+e.toString());
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View view, final int position, long id)
    {
        vpLocationDescImageFileContents = null;
        vpIndex = (short) (position);
        if (position > (qtyVps+1))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    vpsListView.setItemChecked(position, false);
                    String message = getString(R.string.vp_name)+vpIndex+" "+getString(R.string.vp_out_of_bounds);
                    Snackbar.make(vpsListView.getRootView(),message, Snackbar.LENGTH_LONG).show();
                }
            });
            return;
        }
        if (vpIndex==0) {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    vpsListView.setItemChecked(position, false);
                    String message = getString(R.string.vp_name)+vpIndex+" "+getString(R.string.vp_notconfigurable);
                    Snackbar.make(vpsListView.getRootView(),message, Snackbar.LENGTH_LONG).show();
                }
            });
            return;
        }
        // Local file path of VP Location Picture Image
        try
        {
            InputStream fis = MymUtils.getLocalFile("descvp"+(position)+".png",getApplicationContext());
            if (!(fis==null)){
                vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "vpLocationDescImageFile failed, see stack trace"+e.toString());
        }
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Showing vpLocationDescImageFile for VP="+vpIndex+"(vpLocationDescImageFileContents==null)"+(vpLocationDescImageFileContents==null));
                // VP Location Picture ImageView
                if (!(vpLocationDescImageFileContents==null))
                {
                    imageView.setImageBitmap(vpLocationDescImageFileContents);
                    imageView.setVisibility(View.VISIBLE);
                }
                isShowingVpPhoto = true;
                vpsListView.setItemChecked(position, vpChecked[position]);
                // VP Location # TextView
                String vpId = Integer.toString(vpNumber[position]);
                vpId = getString(R.string.vp_name)+vpId;
                vpIdNumber.setText(vpId);
                vpIdNumber.setVisibility(View.VISIBLE);
                // VP Location Description TextView
                vpLocationDesEditTextView.setText(vpLocationDesText[position]);
                vpLocationDesEditTextView.setVisibility(View.VISIBLE);
                vpLocationDesEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            vpLocationDesText[position] = vpLocationDesEditTextView.getText().toString();
                        }
                        return false;
                    }

                });
                vpsListView.setVisibility(View.GONE);
                // VP Acquired
                if (vpAcquired[vpIndex]) vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                if (!vpAcquired[vpIndex]) vpAcquiredStatus.setText(R.string.vpNotAcquiredStatus);
                linearLayoutVpArStatus.setVisibility(View.VISIBLE);
                vpAcquiredStatus.setVisibility(View.VISIBLE);
                cameraShutterButton.setEnabled(true);
                cameraShutterButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_bright)));
                cameraShutterButton.setVisibility(View.INVISIBLE);
                drawTargetFrame = false;
                // Draw Location Description Button and other buttons
                requestPhotoButton.setVisibility(View.VISIBLE);
                qtyVpsLinearLayout.setVisibility(View.INVISIBLE);
                buttonCallImagecap.setVisibility(View.INVISIBLE);
                linearLayoutConfigCaptureVps.setVisibility(View.VISIBLE);
                linearLayoutCaptureNewVp.setVisibility(View.VISIBLE);
                linearLayoutAmbiguousVp.setVisibility(View.VISIBLE);
                linearLayoutSuperSingleVp.setVisibility(View.VISIBLE);
                ambiguousVpToggle.setVisibility(View.VISIBLE);
                if (vpIsAmbiguous[vpIndex]) ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                if (!vpIsAmbiguous[vpIndex]) ambiguousVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                superSingleVpToggle.setVisibility(View.VISIBLE);
                if (vpIsSuperSingle[vpIndex]) superSingleVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
                if (!vpIsSuperSingle[vpIndex]) superSingleVpToggle.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                Log.d(TAG,"onItemClick: markerIdInPose vpSuperMarkerId["+vpIndex+"]="+vpSuperMarkerId[vpIndex]);
                if (vpSuperMarkerId[vpIndex]!=0) {
                    linearLayoutMarkerId.setVisibility(View.VISIBLE);
                    idMarkerNumberTextView.setText(Integer.toString(vpSuperMarkerId[vpIndex]));
                } else {
                    linearLayoutMarkerId.setVisibility(View.INVISIBLE);
                    idMarkerNumberTextView.setText("--");
                }
            }
        });

    }


    public void loadConfigurationFile()
    {
        short vpListOrder = 0;

        vpLocationDesText = new String[qtyVps];
        vpArIsConfigured = new boolean[qtyVps];
        vpIsVideo = new boolean[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
        vpNumber = new short[qtyVps];
        vpFrequencyUnit = new String[qtyVps];
        vpFrequencyValue = new long[qtyVps];
        vpChecked = new boolean[qtyVps];
        vpMarkerlessMarkerWidth = new short[qtyVps];
        vpMarkerlessMarkerHeigth = new short[qtyVps];
        vpIsAmbiguous = new boolean[qtyVps];
        vpFlashTorchIsOn = new boolean[qtyVps];
        vpIsSuperSingle = new boolean[qtyVps];
        vpSuperMarkerId = new int[qtyVps];
        vpAcquired = new boolean[qtyVps];

        Log.d(TAG, "loadConfigurationFile() started");

        for (int i=0; i<(qtyVps); i++)
        {
            vpFrequencyUnit[i] = "";
            vpFrequencyValue[i] = 0;
            vpMarkerlessMarkerWidth[i] = Constants.standardMarkerlessMarkerWidth;
            vpMarkerlessMarkerHeigth[i] = Constants.standardMarkerlessMarkerHeigth;
            vpIsAmbiguous[i] = false;
            vpFlashTorchIsOn[i] = false;
            vpIsSuperSingle[i] = false;
            vpSuperMarkerId[i] = 0;
        }

        // Load Initialization Values from file
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
                        //
                        if(myparser.getName().equalsIgnoreCase("Parameters"))
                        {
                            //
                        }
                        else if(myparser.getName().equalsIgnoreCase("AssetId"))
                        {
                            eventType = myparser.next();
                            assetId = Short.parseShort(myparser.getText());
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
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpNumber"))
                        {
                            eventType = myparser.next();
                            vpNumber[vpListOrder-1] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpArIsConfigured"))
                        {
                            eventType = myparser.next();
                            vpArIsConfigured[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsVideo"))
                        {
                            eventType = myparser.next();
                            vpIsVideo[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpXCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpYCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpZCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpXCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpYCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpZCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpLocDescription"))
                        {
                            eventType = myparser.next();
                            vpLocationDesText[vpListOrder-1] = myparser.getText();
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerWidth"))
                        {
                            eventType = myparser.next();
                            vpMarkerlessMarkerWidth[vpListOrder-1] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerHeigth"))
                        {
                            eventType = myparser.next();
                            vpMarkerlessMarkerHeigth[vpListOrder-1] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsAmbiguous"))
                        {
                            eventType = myparser.next();
                            vpIsAmbiguous[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFlashTorchIsOn"))
                        {
                            eventType = myparser.next();
                            vpFlashTorchIsOn[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpIsSuperSingle"))
                        {
                            eventType = myparser.next();
                            vpIsSuperSingle[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpSuperMarkerId"))
                        {
                            eventType = myparser.next();
                            vpSuperMarkerId[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFrequencyUnit"))
                        {
                            eventType = myparser.next();
                            vpFrequencyUnit[vpListOrder-1] = myparser.getText();
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpFrequencyValue"))
                        {
                            eventType = myparser.next();
                            vpFrequencyValue[vpListOrder-1] = Long.parseLong(myparser.getText());
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG)
                    {
                        Log.d(TAG, "End tag "+myparser.getName());
                    }
                    else if(eventType == XmlPullParser.TEXT)
                    {
                        Log.d(TAG, "Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                fis.close();
            }
            finally
            {
                Log.d(TAG, "Vps Config file = "+vpsFile);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Vps data loading failed:"+e.toString());
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Snackbar.make(mCameraView,getString(R.string.vpcfgfileloadfailed), Snackbar.LENGTH_LONG).show();
                }
            });
        }

        for (int i=0; i<(qtyVps); i++)
        {
            vpChecked[i] = false;
            vpAcquired[i] = true;
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


    public void decreaseQtyOfVps(int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpNumber =Arrays.copyOf(vpNumber, newlength);
        vpLocationDesText = Arrays.copyOf(vpLocationDesText, newlength);
        vpMarkerlessMarkerWidth = Arrays.copyOf(vpMarkerlessMarkerWidth, newlength);
        vpMarkerlessMarkerHeigth = Arrays.copyOf(vpMarkerlessMarkerHeigth, newlength);
        vpIsAmbiguous = Arrays.copyOf(vpIsAmbiguous, newlength);
        vpFlashTorchIsOn = Arrays.copyOf(vpFlashTorchIsOn, newlength);
        vpIsSuperSingle = Arrays.copyOf(vpIsSuperSingle, newlength);
        vpSuperMarkerId = Arrays.copyOf(vpSuperMarkerId, newlength);
        vpArIsConfigured = Arrays.copyOf(vpArIsConfigured, newlength);
        vpIsVideo = Arrays.copyOf(vpIsVideo, newlength);
        vpXCameraDistance = Arrays.copyOf(vpXCameraDistance, newlength);
        vpYCameraDistance = Arrays.copyOf(vpYCameraDistance, newlength);
        vpZCameraDistance = Arrays.copyOf(vpZCameraDistance, newlength);
        vpXCameraRotation = Arrays.copyOf(vpXCameraRotation, newlength);
        vpYCameraRotation = Arrays.copyOf(vpYCameraRotation, newlength);
        vpZCameraRotation = Arrays.copyOf(vpZCameraRotation, newlength);
        vpFrequencyUnit = Arrays.copyOf(vpFrequencyUnit, newlength);
        vpFrequencyValue = Arrays.copyOf(vpFrequencyValue, newlength);
        final int newlengthF = newlength;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String[] newVpsList = new String[newlengthF];
                for (int i=0; i<newlengthF; i++)
                {
                    if (i==0){
                        newVpsList[0] = getString(R.string.vp_00);
                    } else {
                        newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
                    }
                }
                vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
                String message = getString(R.string.qtyvps_decreased)+(newlengthF+1-1)+getString(R.string.to)+(newlengthF-1)+"";
                Snackbar.make(mCameraView,message, Snackbar.LENGTH_LONG).show();
            }
        });
        saveVpsData();
        try {
            File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp"+(newlength)+".png");
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp"+(newlength)+".png");

            if (descvpFile.exists()) {
                descvpFile.delete();
            }
            if (markervpFile.exists()) {
                markervpFile.delete();
            }

            Log.d(TAG,"increaseQtyOfVps: Waiting for initial images to be deleted for the deleted VP");

            Boolean imageFilesOK = false;

            do {
                File descvpFileCHK = new File(getApplicationContext().getFilesDir(),"descvp"+(newlength)+".png");
                File markervpFileCHK = new File(getApplicationContext().getFilesDir(),"markervp"+(newlength)+".png");
                imageFilesOK = ((!descvpFileCHK.exists())&&(!markervpFileCHK.exists()));
            } while (!imageFilesOK);

            Log.d(TAG,"increaseQtyOfVps: initial image files DELETION DONE: imageFilesOK="+imageFilesOK);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "increaseQtyOfVps(): failed to delete all image files, see stack trace "+e.toString());
        }
    }


    public void increaseQtyOfVps(int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpNumber =Arrays.copyOf(vpNumber, newlength);
        vpLocationDesText = Arrays.copyOf(vpLocationDesText, newlength);
        vpMarkerlessMarkerWidth = Arrays.copyOf(vpMarkerlessMarkerWidth, newlength);
        vpMarkerlessMarkerHeigth = Arrays.copyOf(vpMarkerlessMarkerHeigth, newlength);
        vpIsAmbiguous = Arrays.copyOf(vpIsAmbiguous, newlength);
        vpFlashTorchIsOn = Arrays.copyOf(vpFlashTorchIsOn, newlength);
        vpIsSuperSingle = Arrays.copyOf(vpIsSuperSingle, newlength);
        vpSuperMarkerId = Arrays.copyOf(vpSuperMarkerId, newlength);
        vpArIsConfigured = Arrays.copyOf(vpArIsConfigured, newlength);
        vpIsVideo = Arrays.copyOf(vpIsVideo, newlength);
        vpXCameraDistance = Arrays.copyOf(vpXCameraDistance, newlength);
        vpYCameraDistance = Arrays.copyOf(vpYCameraDistance, newlength);
        vpZCameraDistance = Arrays.copyOf(vpZCameraDistance, newlength);
        vpXCameraRotation = Arrays.copyOf(vpXCameraRotation, newlength);
        vpYCameraRotation = Arrays.copyOf(vpYCameraRotation, newlength);
        vpZCameraRotation = Arrays.copyOf(vpZCameraRotation, newlength);
        vpFrequencyUnit = Arrays.copyOf(vpFrequencyUnit, newlength);
        vpFrequencyValue = Arrays.copyOf(vpFrequencyValue, newlength);

        vpFrequencyUnit[newlength-1] = frequencyUnit;
        vpFrequencyValue[newlength-1] = frequencyValue;
        vpMarkerlessMarkerWidth[newlength-1] = Constants.standardMarkerlessMarkerWidth;
        vpMarkerlessMarkerHeigth[newlength-1] = Constants.standardMarkerlessMarkerHeigth;
        vpNumber[newlength-1]= (short) (newlength-1);
        vpLocationDesText[newlength-1]= getString(R.string.vp_capture_placeholder_description)+(newlength-1);
        vpArIsConfigured[newlength-1]=false;
        vpIsVideo[newlength-1]=false;

        final int newlengthF = newlength;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String[] newVpsList = new String[newlengthF];
                for (int i=0; i<newlengthF; i++)
                {
                    if (i==0){
                        newVpsList[0] = getString(R.string.vp_00);
                    } else {
                        newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
                    }
                }
                vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
                String message = getString(R.string.qtyvps_increased)+(newlengthF-1-1)+getString(R.string.to)+(newlengthF-1)+"";
                Snackbar.make(mCameraView,message, Snackbar.LENGTH_LONG).show();
            }
        });
        saveVpsData();
        try {
            File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp"+(newlength-1)+".png");
            File markervpFile = new File(getApplicationContext().getFilesDir(), "markervp"+(newlength-1)+".png");

            if (!descvpFile.exists()) {
                ConfigFileCreator.createDescvpFile(getApplicationContext(),
                        getApplicationContext().getFilesDir(),
                        "descvp"+(newlength-1)+".png",
                        transferUtility,
                        descvpRemotePath,
                        vpIndex,
                        mymensorAccount);
            }
            if (!markervpFile.exists()) {
                ConfigFileCreator.createMarkervpFile(getApplicationContext(),
                        getApplicationContext().getFilesDir(),
                        "markervp"+(newlength-1)+".png",
                        transferUtility,
                        markervpRemotePath,
                        vpIndex,
                        mymensorAccount);
            }

            Log.d(TAG,"increaseQtyOfVps: Waiting for initial images to be created for the new VP");

            Boolean imageFilesOK = false;

            do {
                File descvpFileCHK = new File(getApplicationContext().getFilesDir(),"descvp"+(newlength-1)+".png");
                File markervpFileCHK = new File(getApplicationContext().getFilesDir(),"markervp"+(newlength-1)+".png");
                imageFilesOK = ((descvpFileCHK.exists())&&(markervpFileCHK.exists()));
            } while (!imageFilesOK);

            Log.d(TAG,"increaseQtyOfVps: initial image files CREATION DONE: imageFilesOK="+imageFilesOK);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "increaseQtyOfVps(): failed to create all image files, see stack trace "+e.toString());
        }
    }


    private void callImageCapActivity(){
        Log.d(TAG,"callImageCapActivity: Calling ImageCapActivity with qtyVps="+qtyVps);
        try {
            Intent intent = new Intent(getApplicationContext(), ImageCapActivity.class);
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



}
