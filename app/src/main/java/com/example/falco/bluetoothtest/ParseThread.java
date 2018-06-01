package com.example.falco.bluetoothtest;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class ParseThread extends Thread {

    String dataRead;
    private static final String TAG = "ParseThread";

    public void run(String rawInput, Context mContext) {
        dataRead = rawInput;

        Log.d(TAG, "Parsethread created with string: " + dataRead);
        Log.d(TAG, "String has length: " + dataRead.length());

        //Prepare string for use
        if(dataRead.length() == 7){
            dataRead = dataRead.substring(0, 5);
            Log.d(TAG, "ParseThread: "+dataRead);
            if(dataRead.length() == 5){ //Filter cut off messages
                if(dataRead.equals("mr:01")){   //Filter for
                    Log.d(TAG, "Maracas sound one detected");
                    MediaPlayer mp = MediaPlayer.create(mContext, R.raw.maraca_1);
                    SoundThread temp = new SoundThread();
                    temp.run(mp);
                }
            }
        }

        else{
            dataRead = "";
        }
    }
}
