package com.mymensor;

import com.amazonaws.regions.Regions;

public class Constants {

    public static final String CURR_APP_VERSION = "10007";
    public static final String CLIENT_SOFTWARE_TYPE = "androidarCode10007Name10@mymensor.com";

    public static final short maxQtyVps = 31;
    public static final String frequencyUnit = "millis";
    public static final int frequencyValue = 20000;
    public static final float tolerancePosition = 50;
    public static final float toleranceRotation = 10;
    public static final String capturesFolder = "cap";
    public static final String usersConfigFolder = "usrcfg";
    public static final String vpsConfigFileName = "vps.xml";
    public static final String vpsCheckedConfigFileName = "vpschecked.xml";
    public static final long VpDescFileSize = 36156;
    public static final long VpMarkerFileSize = 32209;
    public static final short captureMarkerWidth = 400;
    public static final short captureMarkerHeight = 400;
    public static final short standardMarkerlessMarkerWidth = 400;
    public static final short standardMarkerlessMarkerHeigth = 400;
    public static final float idMarkerStdSize = 5.0f;
    public static final long shortVideoLength = 10000;
    public static final int cameraWidthInPixels = 1280;
    public static final int cameraHeigthInPixels = 720;
    public static final int xAxisTrackingCorrection = 440; // (1280-400)/2=440 // (1280-700)/2=290
    public static final int yAxisTrackingCorrection = 160; // (720-400)/2=160 // (720-700)/2=10
    public static final int[] validIdMarkersForMyMensor = { 10,11,12,13,14,15,16,17,18,19,
                                                            20,21,22,23,24,25,26,27,28,29,
                                                            30,31,32,33,34,35,36,37,38,39 };

    /**
     * OpenCV
     */
    public static final float mFOVY = 45f; // equivalent in 35mm photography: 28mm lens
    public static final float mFOVX = 60f; // equivalent in 35mm photography: 28mm lens

    /**
     * AWS Cognito
     */
    public static final String COGNITO_POOL_ID = "eu-west-1:963bc158-d9dd-4ae2-8279-b5a8b1524f73";
    public static final Regions COGNITO_POOL_ID_REGION = Regions.EU_WEST_1;
    public static final String AUTH_COGDEV_SERVER = "https://app.mymensor.com/cognito-auth/";
    public static final String AUTH_COGDEV_PROV_NAME = "cogdevserv.mymensor.com";
    /**
     * AWS S3
     */
    public static final String BUCKET_NAME = "mymstorageeuwest1";   // "mymstoragebr";
    public static final String CONN_TST_FILE = "admin/a2f3qw248fgsfreqlgkgjrufjsdadpdf";
    /**
     * MyMensor User Authorization
     */
    public static final String ACCOUNT_TYPE = "com.mymensor.app";
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an mymensor account";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to a mymensor account";
    public static final String AUTH_SERVER = "https://app.mymensor.com/api-token-auth/";
    public static final String REGISTER_SERVER = "https://app.mymensor.com/accounts/register/";
    public static final String MYM_KEY = "mym_authToken";
    public static final String MYM_USER = "mym_user";
    public static final String MYM_USR_GROUP = "MYM_USER_GROUP";
    public static final String MYM_CLIENT_GUID = "mym_client_guid";
    public static final String MYM_CLIENT_SALT = "mym_client_salt";
    public static final String MYM_CLIENT_IV = "mym_client_iv";
}


