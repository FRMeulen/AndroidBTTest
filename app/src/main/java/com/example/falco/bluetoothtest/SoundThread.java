package com.example.falco.bluetoothtest;

import android.media.MediaPlayer;

public class SoundThread extends Thread {

    public SoundThread(MediaPlayer mp){
        run(mp);
    }

    public void run(MediaPlayer mp) {
        mp.start();
        return;
    }

}