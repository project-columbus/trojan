package co.poweramp.crackapp.receiver;

import android.accounts.Account;
import android.location.Location;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 5/1/16.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class Payload {
    private int accountId;
    private long timestamp;
    private String audio, image;
    private Account[] accounts;
    private Location location;

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Account[] getAccounts() {
        return accounts;
    }

    public void setAccounts(Account[] accounts) {
        this.accounts = accounts;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
