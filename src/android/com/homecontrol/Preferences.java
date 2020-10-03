package com.homecontrol;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    public static final String SHARED_PREF_NAME = "CORDIC_PREFERENCE";
    public static final String WSURL = "wsURL";
    private static Preferences mInstance;
    private static Context mCtx;

    public Preferences(Context context) {
        mCtx = context;
    }

    public static synchronized Preferences getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Preferences(context);
        }
        return mInstance;
    }


    //this method will store the user data in shared preferences
    public void update(String KEY, String VALUE) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY, VALUE);
        editor.apply();
    }

    //this method will checker whether user is already logged in or not
    public boolean isLoggedIn(String key) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null) != null;
    }

    //this method will give the logged in user
    public String getdata(String key) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getString(key, null);
    }

    //this method will logout the user
    public void logout() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();


    }

}