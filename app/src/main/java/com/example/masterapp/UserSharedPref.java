package com.example.masterapp;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSharedPref {
    public static final String rememberDeviceSet = "rememberDeviceSet";

    public static SharedPreferences initializeDeviceSetSharedPref(Context context){
        return context.getSharedPreferences(rememberDeviceSet,Context.MODE_PRIVATE);
    }

}
