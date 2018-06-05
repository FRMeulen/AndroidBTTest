package com.example.falco.bluetoothtest;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class ParseThread extends Thread {

    private String dataRead;
    private SoundThread player;
    private final Context mContext;
    private static final String TAG = "ParseThread";

    public ParseThread(Context context){
        mContext = context;
    }

    public void run() {
        Log.i(TAG, "Created ParseThread");
        while(true){

        }
    }

    public void parse(String messageIn){
        if(messageIn.length() == 7){
            dataRead = messageIn.substring(0, 5);
            Log.d(TAG, "PlaySound called with "+dataRead);
            playSound(dataRead);
            dataRead = "";
        }

        else if (messageIn.length() < 7){
            dataRead = dataRead + messageIn;
            Log.d(TAG, "New length: "+dataRead.length());
        }

        else if(messageIn.length() > 7){
            dataRead = "";
            Log.d(TAG, "Message too long, ignoring...");
        }
    }

    private void playSound(String sound){
        Log.i(TAG, "PlaySound: "+dataRead);
        Log.i(TAG, "PlaySound: length: "+dataRead.length());
        if(sound.equals("mr:01")){
            Log.d(TAG, "Maracas sound one detected!");
            MediaPlayer mp = MediaPlayer.create(mContext, R.raw.maraca_1);
            player = new SoundThread(mp);
        }
    }
}
