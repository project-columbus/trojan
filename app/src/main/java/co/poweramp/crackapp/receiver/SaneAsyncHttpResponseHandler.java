package co.poweramp.crackapp.receiver;

import com.loopj.android.http.AsyncHttpResponseHandler;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 5/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public abstract class SaneAsyncHttpResponseHandler extends AsyncHttpResponseHandler {
    @Override
    public boolean getUseSynchronousMode() {
        return false;
    }
}
