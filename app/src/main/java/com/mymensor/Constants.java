package com.mymensor;

import com.amazonaws.regions.Regions;

public class Constants {


    public static final String vpsConfigFileName = "vps.xml";
    public static final String vpsCheckedConfigFileName = "vpschecked.xml";
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
    public static final String BUCKET_NAME = "mymstoragebr";
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
}


