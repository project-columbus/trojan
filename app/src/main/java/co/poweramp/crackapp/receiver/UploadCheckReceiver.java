package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;

import co.poweramp.crackapp.R;
import co.poweramp.crackapp.S3Util;
import co.poweramp.crackapp.Util;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 13/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class UploadCheckReceiver extends BroadcastReceiver {
    private final String TAG = "UploadCheckReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        //Check queue
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            //Upload the queue
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
            final int accountId = sharedPref.getInt("accountId", -1);
            if (accountId == -1) {
                Log.d(TAG, "Account id is -1, ignored!");
                return;
            }
            final Handler handler = new Handler();
//
//            Log.d(TAG, "Sending " + jobQueue.size() + " jobs for uploading");
//            while (!jobQueue.isEmpty()) {
//                Job j = jobQueue.remove();
//                S3Util.uploadJob(context, j, new S3Util.UploadCompletionListener() {
//                    @Override
//                    public void onSuccess(final Payload p) {
//                        Log.d(TAG, "Queued job with timestamp " + p.getTimestamp() + " uploaded!");
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                p.setAccountId(accountId);
//                                Util.submitPayload(context, p);
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onFailure() {
//                        Log.d(TAG, "Queued job failed to upload!");
//                    }
//                });
//            }

            //Get files in cache
            final File[] cacheDir = context.getFilesDir().listFiles();

            //Upload cached files
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (File pointDir : cacheDir) {
                        File audioFile = new File(pointDir, "audio.aac"),
                                imageFile = new File(pointDir, "image.jpg"),
                                locFile = new File(pointDir, "location.json");


                        Log.d(TAG, String.format("Processing cached point '%s'", pointDir.getName()));

                        if (!(audioFile.exists() && imageFile.exists() && locFile.exists())) {
                            Log.d(TAG, "Data point is incomplete!");
                            //Don't cleanup, data collection might be in progress
                            return;
                        }

                        Payload p = new Payload();

                        String acc = Util.getMainAccount(context);
                        if (acc == null) {
                            //Use unique device id
                            acc = Util.getUniqueDeviceId(context);
                        }

                        long timestamp = Long.valueOf(pointDir.getName());

                        p.setAccountId(accountId);
                        p.setAccounts(Util.getAccounts(context));
                        p.setTimestamp(timestamp);

                        //Process audio
                        String audioObjName = String.format("%s/%d/audio.aac", acc, timestamp);
                        boolean audioUpload = S3Util.uploadFile(context, audioObjName, audioFile);
                        p.setAudio(audioObjName);

                        if (!audioUpload) {
                            //Audio upload job failed!
                            Log.d(TAG, String.format("Upload of '%s' failed! Aborting job upload!", audioObjName));
                            audioFile.delete();
                            imageFile.delete();
                            locFile.delete();
                            pointDir.delete();
                            return;
                        }

                        //Process image
                        String imageObjName = String.format("%s/%d/image.jpg", acc, timestamp);
                        boolean imageUpload = S3Util.uploadFile(context, imageObjName, imageFile);
                        if (!imageUpload) {
                            //Image upload job failed!
                            Log.d(TAG, String.format("Upload of '%s' failed! Aborting job upload!", audioObjName));
                            audioFile.delete();
                            imageFile.delete();
                            locFile.delete();
                            pointDir.delete();
                            return;
                        }

                        //Process location
                        Gson gson = new Gson();
                        String locJSON = Util.readFileToString(locFile);

                        try {
                            Location l = gson.fromJson(locJSON, Location.class);
                            p.setLocation(l);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "Location JSON failure, aborting.");
                            audioFile.delete();
                            imageFile.delete();
                            locFile.delete();
                            pointDir.delete();
                            return;
                        }

                        Util.submitPayload(context, p);
                    }

                }
            });

            thread.start();
        }
    }
}
