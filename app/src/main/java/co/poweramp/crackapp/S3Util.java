package co.poweramp.crackapp;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

import co.poweramp.crackapp.receiver.Job;
import co.poweramp.crackapp.receiver.Payload;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 16/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class S3Util {
    private static final String TAG = "S3Util";
    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param context An Context instance.
     * @return A default credential provider.
     */
    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    Constants.COGNITO_POOL_ID,
                    Regions.US_EAST_1);
        }
        return sCredProvider;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param context An Context instance.
     * @return A default S3 client.
     */
    public static AmazonS3Client getS3Client(Context context) {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
        }
        return sS3Client;
    }

    public static boolean uploadFile(Context context, String key, File file) {
        try {
            getS3Client(context).putObject(Constants.BUCKET_NAME, key, file);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, String.format("Uploaded '%s' to S3", key));
        return true;
    }

    public interface UploadCompletionListener {
        void onSuccess(Payload p);
        void onFailure();
    }
}
