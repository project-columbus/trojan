package co.poweramp.crackapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
    public static void submitPayload(Context context, Payload p, final PayloadSubmitListener listener) {
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
                listener.onSuccess();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "Failed to send payload to server: " + statusCode);
                String resp = new String(responseBody);
                Log.d(TAG, "Server response: " + resp);
                listener.onFailure();
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
            for (Account a : getAccounts(context)) {
                if (a.name.contains("@")) {
                    return a.name;
                }
            }
        }
        return null;
    }

    public static boolean deleteDir(File dir)
    {
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++)
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }
        // The directory is now empty or this is a file so delete it
        return dir.delete();
    }

    public static String readFileToString(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sb.append(System.lineSeparator());
                } else {
                    sb.append("\n");
                }
                line = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface PayloadSubmitListener {
        void onSuccess();
        void onFailure();
    }
}
