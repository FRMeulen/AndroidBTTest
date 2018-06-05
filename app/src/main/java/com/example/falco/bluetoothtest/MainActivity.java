//Package
package com.example.falco.bluetoothtest;

//Imports
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

//Class
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    //Buttons
    Button onOffButton; //Enable or disable bluetooth
    Button discoverButton;  //Discover unpaired devices nearby
    Button connectButton;   //Connect to selected device
    Button disconnectButton;    //Disconnect bluetooth

    //Text fields
    TextView btState;   //Text showing bluetooth state
    TextView btSelectedText;    //Text showing "Selected: "
    TextView btSelected;    //Text showing selected device

    //Bluetooth fields
    BluetoothConnectionService mBluetoothConnection;    //Bluetooth connection object
    BluetoothDevice mBTDevice;  //Bluetooth device object
    BluetoothAdapter mBluetoothAdapter; //Device bluetooth adapter

    //UUID
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");   //Default SerialPortService ID

    //Tag for logging
    private static final String TAG = "My Activity";    //Tag for logs

    //Device list
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();   //List of discovered devices
    public ArrayList<BluetoothDevice> mIgnoredDevices = new ArrayList<>();  //List of unwanted devices
    public DeviceListAdapter mDeviceListAdapter;    //Adapter for listed devices
    ListView deviceList;    //Listview for discovered devices

    //BroadcastReceiver for ACTION_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiverBTState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {    //If bluetooth adapter state changed
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);  //Get adapter state

                switch(state){
                    case BluetoothAdapter.STATE_OFF:    //If bluetooth is off
                        Log.d(TAG, "bluetoothAdapter: State Off");  //Log
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:    //If bluetooth is turning off
                        Log.d(TAG, "bluetoothAdapter: State Turning Off");  //Log
                        break;
                    case BluetoothAdapter.STATE_ON: //If bluetooth is on
                        Log.d(TAG, "bluetoothAdapter: State On");   //Log
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON: //If bluetooth is turning on
                        Log.d(TAG, "bluetoothAdapter: State Turning On");   //Log
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_SCAN_MODE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiverDiscoverabilityState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {    //If scan mode changed
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, mBluetoothAdapter.ERROR);   //Get scan mode

                switch(mode){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:   //If connectable and discoverable
                        Log.d(TAG, "bluetoothAdapter: Discoverability enabled");    //Log
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:    //If connectable but NOT discoverable
                        Log.d(TAG, "bluetoothAdapter: Discoverability disabled. Able to receive connections");  //Log
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:   //If not connectable and discoverable
                        Log.d(TAG, "bluetoothAdapter: Discoverability disabled. Not able to receive connections");  //Log
                        break;
                    case BluetoothAdapter.STATE_CONNECTING: //If connecting
                        Log.d(TAG, "bluetoothAdapter: Connecting...");  //Log
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:  //If connected
                        Log.d(TAG, "bluetoothAdapter: Connected."); //Log
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiverDeviceFound = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {  //If action found on device
                //Scan for new devices
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

                //Ignore duplicates
                if(mIgnoredDevices.contains(device)){
                    //Device will be ignored fully
                }
                else {
                    if(!mBTDevices.contains(device)){
                        if(device.getName() != null){
                            if(device.getName().equals("Music Hand Right")){
                                mBTDevices.add(device); //Add to connectable devices
                                mIgnoredDevices.add(device);    //Ignore when found again
                                Log.d(TAG, "onReceive: Listed " + device.getName() + " at " + device.getAddress() + ".");   //Log
                            }
                            else if(device.getName().equals("Music Hand Left")){
                                mBTDevices.add(device); //Add to connectable devices
                                mIgnoredDevices.add(device);    //Ignore when found again
                                Log.d(TAG, "onReceive: Listed " + device.getName() + " at " + device.getAddress() + ".");   //Log
                            }
                            else{
                                mIgnoredDevices.add(device);    //Ignore device
                                Log.d(TAG, "Unwanted device found, ignoring...");   //Log
                            }
                        }
                    }
                }
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);  //Create adapter for listed devices
                deviceList.setAdapter(mDeviceListAdapter);  //Add adapter to listView
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_BOND_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiverBondState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) { //If bond state changed
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //If already bonded
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "bondStateReceiver: BOND_BONDED");   //Log
                    mBTDevice = mDevice;    //Connect
                }

                //If in the process of bonding
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "bondStateReceiver: BOND_BONDING");  //Log
                }

                //If no bond exists
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "bondStateReceiver: BOND_NONE"); //Log
                }
            }
        }
    };

    //When app is ended
    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiverBTState);
        unregisterReceiver(mBroadcastReceiverDiscoverabilityState);
        unregisterReceiver(mBroadcastReceiverDeviceFound);
        unregisterReceiver(mBroadcastReceiverBondState);
    }

    //When app is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Adapters
        mBluetoothAdapter = mBluetoothAdapter.getDefaultAdapter();

        //Buttons
        onOffButton = findViewById(R.id.onOffButton);
        discoverButton = findViewById(R.id.listButton);
        deviceList = findViewById(R.id.deviceList);
        mBTDevices = new ArrayList<>();
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        //Text Fields
        btState = findViewById(R.id.bt_state);
        btSelectedText = findViewById(R.id.bt_selected_text);
        btSelected = findViewById(R.id.bt_selected);

        //Filter for bond state changed
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiverBondState, filter);

        //Listeners
        deviceList.setOnItemClickListener(MainActivity.this);   //Device list item click listener

        if(mBluetoothAdapter.isEnabled()){
            discoverButton.setVisibility(View.VISIBLE);
        }

        //On / Off button
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");   //Log
                if(!mBluetoothAdapter.isEnabled()){ //If bluetooth adapter is disabled upon click
                    discoverButton.setVisibility(View.VISIBLE); //Show discover button
                }
                else {  //If bluetooth adapter is enabled upon click
                    discoverButton.setVisibility(View.INVISIBLE);   //Hide discover button
                }
                enableDisableBluetooth();   //Switch adapter state
            }
        });

        //Discover button
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Discovering devices...");  //Log
                mBTDevices.clear(); //Empty device list to avoid duplicates
                connectButton.setVisibility(View.VISIBLE);  //Show connect button
                btSelectedText.setVisibility(View.VISIBLE); //Show "Selected: "
                btSelected.setVisibility(View.VISIBLE); //Show selected device
                deviceList.setVisibility(View.VISIBLE); //Show found devices
                discoverDevices();  //Search for bluetooth devices
            }
        });

        //Connect button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBTDevice != null){
                    startConnection();  //Start connection to selected device
                    btState.setText(mBTDevice.getName());   //Show connected device's name
                    btSelectedText.setVisibility(View.INVISIBLE);   //Hide "Selected: "
                    btSelected.setVisibility(View.INVISIBLE);   //Hide selection
                    deviceList.setVisibility(View.INVISIBLE);   //Hide device list
                    connectButton.setVisibility(View.INVISIBLE);
                    disconnectButton.setVisibility(View.VISIBLE);   //Show disconnect button
                }
                else{
                    Toast noneSelectedToast = Toast.makeText(getApplicationContext(), "No device selected!", Toast.LENGTH_LONG);
                    noneSelectedToast.show();
                }
            }
        });

        //Disconnect button
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killConnection();   //Kill connection

                //Reset to start of application
                if(mBluetoothAdapter.isEnabled()){
                    discoverButton.setVisibility(View.VISIBLE);
                }
                disconnectButton.setVisibility(View.INVISIBLE);
                mBTDevice = null;
                mBTDevices.clear();
                btSelected.setText("NONE");
                btState.setText("NONE");
            }
        });
    }

    public void startConnection(){
        startBluetoothConnection(mBTDevice, MY_UUID_INSECURE);  //Start connection
    }

    public void killConnection(){
        if(mBluetoothConnection != null){   //If connection exists
            mBluetoothConnection.killClient();  //Kill connection
            mBluetoothConnection = null;    //Delete connection object
        }
    }

    public void startBluetoothConnection(BluetoothDevice device, UUID uuid){
        mBluetoothConnection.startClient(device, uuid); //Start bluetooth connection
    }

    //Discovery
    public void discoverDevices(){
        Log.d(TAG, "discoverButton: Looking for unpaired devices...");  //Log

        if(mBluetoothAdapter.isDiscovering()){  //If discovery is on
            Log.d(TAG, "discoverButton: Canceling discovery."); //Log
            mBluetoothAdapter.cancelDiscovery();    //Cancel discovery

            checkBTPermissions();   //Check bluetooth permissions in manifest - REQUIRED

            Log.d(TAG, "discoverButton: Enabling discovery.");  //Log
            mBluetoothAdapter.startDiscovery(); //Restart discovery
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);    //Filter for device found
            registerReceiver(mBroadcastReceiverDeviceFound, discoverDevicesIntent); //Register receiver
        }

        if(!mBluetoothAdapter.isDiscovering()){ //If not discovering
            checkBTPermissions();   //Check bluetooth permissions in manifest - REQUIRED

            Log.d(TAG, "discoverButton: Enabling discovery.");  //Log
            mBluetoothAdapter.startDiscovery(); //Start discovery
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);    //Filter for device found
            registerReceiver(mBroadcastReceiverDeviceFound, discoverDevicesIntent); //Register receiver
        }
    }

    //Check bluetooth permissions in manifest
    @SuppressLint("NewApi")
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){   //If phone uses android Lollipop or up
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION"); //Check fine location permission
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");  //Check coarse location permission
            if (permissionCheck != 0) { //If they are granted
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");    //Log
        }
    }

    public void enableDisableBluetooth(){
        if(mBluetoothAdapter == null){  //If phone has no bluetooth adapter
            Log.d(TAG,"onOffButton: No bluetooth adapter.");    //Bluetooth not usable
        }

        if(!mBluetoothAdapter.isEnabled()){ //If adapter is not enabled
            Log.d(TAG, "onOffButton: enabling bluetooth."); //Enabling bluetooth
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  //Intent for bluetooth enable
            startActivity(enableBluetoothIntent);   //Start activity

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED); //Filter for adapter state changed
            registerReceiver(mBroadcastReceiverBTState, bluetoothIntent);   //Register receiver
        }

        if(mBluetoothAdapter.isEnabled()){  //If adapter is enabled
            Log.d(TAG, "onOffButton: disabling bluetooth.");    //Disabling bluetooth
            mBluetoothAdapter.disable();    //Disable adapter

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED); //Filter for adapter state changed
            registerReceiver(mBroadcastReceiverBTState, bluetoothIntent);   //Register receiver
        }
    }

    //Item click in ListView
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBluetoothAdapter.cancelDiscovery();    //Cancel discovery to save memory
        Log.d(TAG, "onItemClick: Item Clicked!");   //Log

        String deviceName = mBTDevices.get(position).getName(); //Get device name
        String deviceAddress = mBTDevices.get(position).getAddress();   //Get device address
        btSelected.setText(deviceName); //Show selected device name
        Log.d(TAG, "onItemClick: deviceName = " + deviceName);  //Log
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);    //Log

        //Create the bond
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){ //If device uses android Jelly Bean MR2 or up
            Log.d(TAG, "Trying to pair with " + deviceName);    //Log
            mBTDevices.get(position).createBond();  //Create bond

            mBTDevice = mBTDevices.get(position);   //Store connected device in variable
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);   //Start connection service
            //Starting a connection service starts an AcceptThread
        }
    }
}