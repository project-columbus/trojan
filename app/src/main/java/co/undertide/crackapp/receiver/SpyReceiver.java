package co.undertide.crackapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 22/11/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class SpyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "HEHEYE", Toast.LENGTH_LONG).show();
    }
}
