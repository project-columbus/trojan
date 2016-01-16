package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;

import java.util.LinkedList;
import java.util.Queue;

import co.poweramp.crackapp.Constants;
import co.poweramp.crackapp.R;
import co.poweramp.crackapp.S3Util;
import co.poweramp.crackapp.Util;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 22/11/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class SpyReceiver extends BroadcastReceiver {
    private final String TAG = "SpyReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
        final int accountId = sharedPref.getInt("accountId", -1);
        if (accountId == -1) {
            Log.d(TAG, "Account id is -1, ignored!");
            return;
        }
        final Handler handler = new Handler();
        Log.d(TAG, "Doing job");
        new Job(context);
    }
}
