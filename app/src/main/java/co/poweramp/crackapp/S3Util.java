package co.poweramp.crackapp;

import android.content.Context;
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

    /**
     * Upload to S3
     */
    public static void uploadJob(final Context context, final Job job, final UploadCompletionListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //Audio
                File audioFile = new File(job.getAudioFilePath());
                String audioObjName = String.format("%s/%d/audio.aac", job.getMainAccount().name, job.getPayload().getTimestamp());
                try {
                    getS3Client(context).putObject(Constants.BUCKET_NAME, audioObjName, audioFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onFailure();
                    return;
                }
                job.getPayload().setAudio(audioObjName);
                Log.d(TAG, "Uploaded audio to S3");

                //Image
                File imageFile = new File(job.getImageFilePath());
                String imageObjName = String.format("%s/%d/image.jpg", job.getMainAccount().name, job.getPayload().getTimestamp());
                try {
                    getS3Client(context).putObject(Constants.BUCKET_NAME, imageObjName, imageFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onFailure();
                    return;
                }
                job.getPayload().setImage(imageObjName);
                Log.d(TAG, "Uploaded image to S3");
                listener.onSuccess(job.getPayload());
                job.cleanup();
            }
        });
        thread.start();
    }

    public interface UploadCompletionListener {
        void onSuccess(Payload p);
        void onFailure();
    }
}
