package com.example.falco.bluetoothtest;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    private ProgressDialog mProgressDialog;

    private final BluetoothAdapter mBluetoothAdapter;

    private final Context mContext;

    private String userInput = "";

    public BluetoothConnectionService (Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.i(TAG, "AcceptThread: Setting up server using " + MY_UUID_INSECURE);
            }
            catch(IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            mmServerSocket = tmp;
        }

        public void run(){
            Log.i(TAG, "run: AcceptThread running");
            BluetoothSocket socket = null;

            try{
                Log.d(TAG, "run: Rfcomm server socket start");
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: Rfcomm server socket accepted connection");
            }catch(IOException e){
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }

            if(socket != null){
                connected(socket);
            }
            Log.i(TAG, "END AcceptThread");
        }

        public void cancel(){
            Log.d(TAG, "cancel: Canceling AcceptThread");
            try{
                mmServerSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: Closing of AcceptThread ServerSocket failed " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;

            try{
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID " + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            }catch(IOException e){
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d(TAG, "ConnectThread: Connection successful!");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "ConnectThread: Unable to close connection in socket");
                }
                Log.e(TAG, "ConnectThread: Could not connect to InsecureRfcommSocket " + e.getMessage());
            }

            connected(mmSocket);
        }

        public void cancel(){
            Log.d(TAG, "cancel: Canceling AcceptThread");
            try{
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: Closing of AcceptThread ServerSocket failed " + e.getMessage());
            }
        }
    }

    public synchronized void start(){
        Log.d(TAG, "BluetoothConnectionService: started");

        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mAcceptThread == null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public void killClient(){
        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: started");

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread (BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: starting");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mProgressDialog.dismiss();

            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Failed to get input stream");
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Failed to get output stream");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            while(true){
                try {
                    read();
                } catch (IOException e) {
                    Log.e(TAG, "run: Error reading input stream");
                    break;
                }
            }
        }

        public void read() throws IOException{
            byte[] buffer = new byte[1024];
            int bytes;

            bytes = mmInStream.read(buffer);
            String incomingMessage = new String(buffer, 0, bytes);
            Log.d(TAG, "InputStream: " + incomingMessage);
            userInput = incomingMessage;
        }

        public void write(byte[] bytes){
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to outputStream");
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            } catch(IOException e){}
        }
    }

    private void connected(BluetoothSocket mmSocket){
        Log.d(TAG, "connected: starting");

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out){
        mConnectedThread.write(out);
    }

    public void read(){
        try {
            mConnectedThread.read();
            Log.d(TAG, "BluetoothConnection: read: Reading input");
        } catch (IOException e) {
            Log.e(TAG, "BluetoothConnection: read: Error reading inputStream");
        }
    }

    public String getUserInput(){
        return userInput;
    }
}