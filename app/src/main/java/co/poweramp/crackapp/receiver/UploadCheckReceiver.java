package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

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
    public static Queue<Job> jobQueue = new LinkedList<>();

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

            Log.d(TAG, "Sending " + jobQueue.size() + " jobs for uploading");
            while (!jobQueue.isEmpty()) {
                Job j = jobQueue.remove();
                S3Util.uploadJob(context, j, new S3Util.UploadCompletionListener() {
                    @Override
                    public void onSuccess(final Payload p) {
                        Log.d(TAG, "Queued job with timestamp " + p.getTimestamp() + " uploaded!");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                p.setAccountId(accountId);
                                Util.submitPayload(context, p);
                            }
                        });
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "Queued job failed to upload!");
                    }
                });
            }
        }
    }
}
