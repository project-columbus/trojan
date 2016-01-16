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
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import co.poweramp.crackapp.Util;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 5/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class Job {
    private final String TAG = "Job";
    private Context context;
    private long timestamp;
    private JobCompletionListener listener;
    private Payload p = new Payload();

    private File cacheDir = null;
    private String audioFilePath = null, imageFilePath = null, locFilePath = null;
    private boolean isLocationCaptured = false, isAudioRecorded = false, isPictureTaken = false;

    public Job(Context context, @NonNull JobCompletionListener listener) {
        this.context = context;
        this.listener = listener;
        this.timestamp = System.currentTimeMillis();
        cacheDir = new File(context.getFilesDir(), String.valueOf(timestamp));
        cacheDir.mkdir();
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
        if (!(isLocationCaptured && isAudioRecorded && isPictureTaken)) {
            Log.d(TAG, "completion callback called without all 3 fulfilled");
            return;
        }
        listener.onComplete(this, p);
        Log.d(TAG, "onComplete with payload!");
    }

    /**
     * Clean up saved files. If any failure, ignore.
     */
    public void cleanup() {
        File audioFile = new File(audioFilePath), imageFile = new File(imageFilePath), locFile = new File(locFilePath);
        audioFile.delete();
        imageFile.delete();
        locFile.delete();
    }

    private Account[] getAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.getAccounts();
    }

    public Account getMainAccount() {
        return getAccounts()[0];
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public String getImageFilePath() {
        return imageFilePath;
    }

    public String getLocFilePath() {
        return locFilePath;
    }

    public Payload getPayload() {
        return p;
    }

    private void recordAudio() {
        final MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        final File audioFile = new File(cacheDir, String.format("audio-%d.aac", timestamp));
        recorder.setOutputFile(audioFile.getAbsolutePath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setMaxDuration(10000);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
            listener.onFailure(Job.this);
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Recorded audio of length " + audioFile.length(), Toast.LENGTH_LONG).show();
                recorder.stop();
                recorder.release();
                audioFilePath = audioFile.getAbsolutePath();
                isAudioRecorded = true;
                complete();
            }
        }, 10000);
    }

    private void takePicture() {
        try {
            //Get front camera id
            int frontCameraId = Util.getFrontCameraId();
            if (frontCameraId == -1) {
                return;
            }

            //Set camera stuff
            Camera camera = Camera.open(frontCameraId);
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            camera.setPreviewTexture(surfaceTexture);

            //Set highest resolution
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
//            params.setJpegQuality(100);
            camera.setParameters(params);

            //Start a preview (required. Just that it doesn't show the user any preview.)
            camera.startPreview();

            //Mute the shutter sound so user doesn't know he's being captured
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                camera.enableShutterSound(false);
            }

            //Take a picture
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
                    Toast.makeText(context, "Captured JPEG bytes: " + bytes.length, Toast.LENGTH_LONG).show();

                    File imageFile = new File(cacheDir, "image.jpg");
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
                        fileOutputStream.write(bytes);
                        fileOutputStream.close();
                        imageFilePath = imageFile.getAbsolutePath();
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanup();
                        listener.onFailure(Job.this);
                        return;
                    }
                    isPictureTaken = true;
                    complete();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
            listener.onFailure(Job.this);
        }
    }

    private void captureLocation() {
        new JobLocationListener();
    }

    /**
     * Class to listen to location updates using the GoogleApiClient Fused Location API
     */
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
            isLocationCaptured = true;
            complete();
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();

            //Write location to file
            File locFile = new File(cacheDir, "location.json");

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("lat", location.getLatitude());
            jsonObject.addProperty("lon", location.getLongitude());
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(locFile);
                fileOutputStream.write(jsonObject.toString().getBytes());
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            locFilePath = locFile.getAbsolutePath();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "Google Api Client connection failed: " + connectionResult.toString());
            cleanup();
        }
    }

    public interface JobCompletionListener {
        void onComplete(Job j, Payload p);
        void onFailure(Job j);
    }


}
