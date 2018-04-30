//Package
package com.example.falco.bluetoothtest;

//Imports
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//Class
public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";    //Log Tag
    private static final String appName = "MYAPP";  //App name
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");   //UUID

    //Threads
    private AcceptThread mAcceptThread; //Accept thread - making server socket and waiting for accept
    private ConnectThread mConnectThread;   //Connect thread - connecting to device once socket is accepted
    private ConnectedThread mConnectedThread;   //Connected thread - reading / writing data once connection is secure

    private BluetoothDevice mmDevice;   //Connected device
    private UUID deviceUUID;    //UUID of device
    private ProgressDialog mProgressDialog; //Dialog during connection

    private final BluetoothAdapter mBluetoothAdapter;   //Phone bluetooth adapter

    private final Context mContext; //Context

    private String dataRead = "";   //Data read from bluetooth module

    private TextView dataReceived;  //View to show data received

    //Constructor
    public BluetoothConnectionService (Context context) {
        mContext = context; //Set context
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();   //Get phone bluetooth adapter
        start();    //Start
    }

    //Accept Thread
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket; //Server socket

        //Constructor
        public AcceptThread(){
            BluetoothServerSocket tmp = null;   //Server socket is null at first

            try{    //Try to listen
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);  //Listen for Rfcomm
                Log.i(TAG, "AcceptThread: Setting up server using " + MY_UUID_INSECURE);    //Log
            }
            catch(IOException e){   //Catch exceptions
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage()); //Log
            }

            mmServerSocket = tmp;   //If successful set found socket to permanent variable
        }

        //Run on creation
        public void run(){
            Log.i(TAG, "AcceptThread: started");    //Log
            BluetoothSocket socket = null;  //Store socket in ConnectThread

            try{    //Try to accept
                Log.d(TAG, "run: Rfcomm server socket start");  //Log
                socket = mmServerSocket.accept();   //Accept socket
                Log.d(TAG, "run: Rfcomm server socket accepted connection");    //Log
                Log.i(TAG, "Killing AcceptThread!");    //Log
                return; //Kill thread
            }catch(IOException e){  //Catch exceptions
                Log.e(TAG, "run: IOException: " + e.getMessage());  //Log
            }
        }

        //Cancel thread
        public void cancel(){
            Log.d(TAG, "cancel: Canceling AcceptThread");   //Log
            try{    //Try to exit
                mmServerSocket.close(); //Close socket
            } catch (IOException e){    //Catch exceptions
                Log.e(TAG, "cancel: Closing of AcceptThread ServerSocket failed " + e.getMessage());    //Log
            }
        }
    }

    //Connect thread
    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;   //Socket stored in ConnectThread

        //Constructor
        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.i(TAG, "ConnectThread: started");   //Log
            mmDevice = device;  //Set device
            deviceUUID = uuid;  //Set UUID
        }

        //Run on creation
        public void run(){
            BluetoothSocket tmp = null; //Socket is null at first

            try{    //Create Rfcomm socket
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID " + MY_UUID_INSECURE);  //Log
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);   //Create Rfcomm socket
            }catch(IOException e){  //Catch exceptions
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());   //Log
            }

            mmSocket = tmp; //Store socket in permanent variable
            mBluetoothAdapter.cancelDiscovery();    //Cancel discovery
            try {   //Try to connect
                mmSocket.connect(); //Connect
                Log.d(TAG, "ConnectThread: Connection successful!");    //Log
            } catch (IOException e) {   //Catch exceptions
                try {   //Try to close
                    mmSocket.close();   //Close
                } catch (IOException e1) {  //Catch exceptions
                    Log.e(TAG, "ConnectThread: Unable to close connection in socket");  //Log
                }
                Log.e(TAG, "ConnectThread: Could not connect to InsecureRfcommSocket " + e.getMessage());   //Log
            }

            connected(mmSocket);    //Start connected thread
            Log.i(TAG, "Killing ConnectThread!");   //Log
            return; //Kill thread
        }

        //Cancel thread
        public void cancel(){
            Log.d(TAG, "cancel: Canceling ConnectThread");   //Cancel
            try{    //Try to close socket
                mmSocket.close();   //Close socket
            } catch (IOException e){    //Catch exceptions
                Log.e(TAG, "cancel: Closing of ConnectThread Socket failed " + e.getMessage()); //Log
            }
        }
    }

    //Start method on Service creation
    public synchronized void start(){
        Log.d(TAG, "BluetoothConnectionService: started");  //Log

        if(mConnectThread != null){ //If a ConnectThread exists
            mConnectThread.cancel();    //Cancel it
            mConnectThread = null;  //Delete it
        }

        if(mAcceptThread == null){  //If no AcceptThread exists
            mAcceptThread = new AcceptThread(); //Make a new one
            mAcceptThread.start();  //Start it
        }
    }

    //Kill method
    public void killClient(){
        if(mConnectedThread != null){   //If ConnectedThread exists
            mConnectedThread.cancel();  //Cancel it
            mConnectedThread = null;    //Delete it
        }
    }

    //Start client
    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: started"); //Log

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);    //Show progress to connecting
        mConnectThread = new ConnectThread(device, uuid);   //Create ConnectThread
        mConnectThread.start(); //Start ConnectThread
    }

    //Connected thread
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket; //Socket
        private final InputStream mmInStream;   //Input stream
        private final OutputStream mmOutStream; //Output stream

        //Constructor
        public ConnectedThread (BluetoothSocket socket){
            Log.i(TAG, "ConnectedThread: starting");    //Log
            mmSocket = socket;  //Set socket
            InputStream tmpIn = null;   //Inputstream is null at first
            OutputStream tmpOut = null; //Outputstream is null at first
            mProgressDialog.dismiss();  //Clear progress dialog

            try {   //Try to get input stream
                tmpIn = mmSocket.getInputStream();  //Get input stream
            } catch (IOException e) {   //Catch exceptions
                Log.e(TAG, "ConnectedThread: Failed to get input stream");  //Log
            }
            try {   //Try to get output stream
                tmpOut = mmSocket.getOutputStream();    //Get output stream
            } catch (IOException e) {   //Catch exceptions
                Log.e(TAG, "ConnectedThread: Failed to get output stream"); //Log
            }

            mmInStream = tmpIn; //Store input stream in permanent variable
            mmOutStream = tmpOut;   //Store output stream in permanent variable
        }

        //Run on creation
        public void run(){
            byte[] buffer = new byte[1024]; //Buffer byte array
            int bytes;  //Length of read bytes

            while(true){    //Infinite loop
                try {   //Try to read input
                    if(mmInStream.available()> 0){  //If input is found
                        Log.d(TAG, "Reading...");
                        bytes = mmInStream.read(buffer);    //Set length
                        String incomingMessage = new String(buffer, 0, bytes);  //Form string from bytes
                        Log.d(TAG, "InputStream: " + incomingMessage);  //Log
                        dataRead = incomingMessage; //Store read message
                    }
                    else{   //If no input is available
                        SystemClock.sleep(100); //Sleep for 100 ms
                    }
                } catch (IOException e) {   //Catch exceptions
                    Log.e(TAG, "run: Error reading input stream");  //Log
                    break;  //Break loop
                }
            }
            Log.i(TAG, "Killing ConnectedThread!"); //Log
            return; //Kill thread
        }

        //Write to bluetooth
        public void write(byte[] bytes){
            try {   //Try to write
                Log.d(TAG, "write: Writing text...");   //Log
                mmOutStream.write(bytes);   //Write
                Log.d(TAG, "write: Done writing");  //Log
            } catch (IOException e) {   //Catch exceptions
                Log.e(TAG, "write: Error writing to outputStream"); //Log
            }
        }

        //Cancel thread
        public void cancel(){
            try{    //Try to close socket
                mmSocket.close();   //Close socket
            } catch(IOException e){}    //Catch exceptions
        }
    }

    //Start connection
    private void connected(BluetoothSocket mmSocket){
        Log.d(TAG, "connected: starting");  //Log

        mConnectedThread = new ConnectedThread(mmSocket);   //Create ConnectedThread
        mConnectedThread.start();   //Start ConnectedThread
    }

    //Write method accessor
    public void write(byte[] out){
        mConnectedThread.write(out);    //Call write method in ConnectedThread
    }

    //Get read data
    public String getDataRead(){
        return dataRead;    //Return read data
    }
}