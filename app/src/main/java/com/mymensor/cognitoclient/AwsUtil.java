package com.mymensor.cognitoclient;

import android.content.Context;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

public class AwsUtil {

    private static TransferUtility sTransferUtility;

    public static TransferUtility getTransferUtility(AmazonS3Client s3Client, Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(s3Client, context.getApplicationContext());
        }

        return sTransferUtility;
    }
}