package com.example.falco.bluetoothtest;

import android.media.MediaPlayer;

public class SoundThread extends Thread {

    public void run(MediaPlayer mp) {
        mp.start();
        return;
    }

}