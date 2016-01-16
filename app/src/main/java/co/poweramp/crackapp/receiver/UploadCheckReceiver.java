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
import java.util.ArrayList;
import java.util.List;

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
    private static List<String> uploadInProgressList = new ArrayList<>();

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

            //Get files in cache
            final File[] cacheDir = context.getFilesDir().listFiles();

            //Upload cached files
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (final File pointDir : cacheDir) {
                        final File audioFile = new File(pointDir, "audio.aac"),
                                imageFile = new File(pointDir, "image.jpg"),
                                locFile = new File(pointDir, "location.json"),
                                doneFile = new File(pointDir, "donefile");


                        Log.d(TAG, String.format("Processing cached point '%s'", pointDir.getName()));


                        if (!(audioFile.exists() && imageFile.exists() && locFile.exists() && doneFile.exists())) {
                            Log.d(TAG, "Data point is incomplete!");

                            //Don't cleanup, data collection might be in progress
                            long pointDirAge = System.currentTimeMillis() - pointDir.lastModified();

                            //If older than 24 hours, assume impossible to complete and just delete.
                            if (pointDirAge >= 86400000) {
                                Util.deleteDir(pointDir);
                            }
                            continue;
                        }

                        uploadInProgressList.add(pointDir.getName());

                        final Payload p = new Payload();

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
                            Util.deleteDir(pointDir);
                            continue;
                        }

                        //Process image
                        String imageObjName = String.format("%s/%d/image.jpg", acc, timestamp);
                        boolean imageUpload = S3Util.uploadFile(context, imageObjName, imageFile);
                        p.setImage(imageObjName);
                        if (!imageUpload) {
                            //Image upload job failed!
                            Log.d(TAG, String.format("Upload of '%s' failed! Aborting job upload!", audioObjName));
                            Util.deleteDir(pointDir);
                            continue;
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
                            Util.deleteDir(pointDir);
                            continue;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Util.submitPayload(context, p, new Util.PayloadSubmitListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Payload submission success, removing cached data");
                                        Util.deleteDir(pointDir);
                                        uploadInProgressList.remove(pointDir.getName());
                                    }

                                    @Override
                                    public void onFailure() {
                                        Log.d(TAG, "Payload submission failed, leaving cached data untouched");
                                        uploadInProgressList.remove(pointDir.getName());
                                    }
                                });
                            }
                        });
                    }

                }
            });

            thread.start();
        } else {
            Log.d(TAG, "No Wi-Fi available, nothing done.");
        }
    }
}
