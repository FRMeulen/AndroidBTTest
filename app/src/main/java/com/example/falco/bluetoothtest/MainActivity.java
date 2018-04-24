package com.example.falco.bluetoothtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    //Buttons
    Button onOffButton; //Enable or disable bluetooth
    Button discoverabilityButton;   //Enable device discoverability for 300 seconds
    Button discoverButton;  //Discover unpaired devices nearby
    Button connectButton;   //Connect to selected device
    Button sendButton;  //Send data in sendText to connected device
    Button disconnectButton;    //Disconnect bluetooth

    TextView btState;   //Text showing bluetooth state
    TextView dataReceived;  //Text showing data received
    TextView btSelectedText;    //Text showing "Selected: "
    TextView btSelected;    //Text showing selected device
    EditText sendText;  //Field to enter text to send

    BluetoothConnectionService mBluetoothConnection;    //Bluetooth connection object
    BluetoothDevice mBTDevice;  //Bluetooth device object

    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");   //Default SerialPortService ID

    BluetoothAdapter mBluetoothAdapter; //Device bluetooth adapter
    private static final String TAG = "My Activity";    //Tag for logs

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();   //List of discovered devices
    public DeviceListAdapter mDeviceListAdapter;    //Adapter for listed devices
    ListView deviceList;    //Listview for discovered devices

    //BroadcastReceiver for ACTION_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiverBTState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {    //If bluetooth adapter state changed
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);  //Get adapter state

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "bluetoothAdapter: State Off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "bluetoothAdapter: State Turning Off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "bluetoothAdapter: State On");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "bluetoothAdapter: State Turning On");
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
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "bluetoothAdapter: Discoverability enabled");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "bluetoothAdapter: Discoverability disabled. Able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "bluetoothAdapter: Discoverability disabled. Not able to receive connections");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "bluetoothAdapter: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "bluetoothAdapter: Connected.");
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiverDeviceFound = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                //Scan for new devices
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);

                if(!mBTDevices.contains(device)){
                    mBTDevices.add(device);
                }

                Log.d(TAG, "onReceive: Listed " + device.getName() + " at " + device.getAddress() + ".");
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                deviceList.setAdapter(mDeviceListAdapter);
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_BOND_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiverBondState = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //If already bonded
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "bondStateReceiver: BOND_BONDED");
                    mBTDevice = mDevice;
                }

                //If in the process of bonding
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "bondStateReceiver: BOND_BONDING");
                }

                //If no bond exists
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "bondStateReceiver: BOND_NONE");
                }
            }
        }
    };

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiverBTState);
        unregisterReceiver(mBroadcastReceiverDiscoverabilityState);
        unregisterReceiver(mBroadcastReceiverDeviceFound);
        unregisterReceiver(mBroadcastReceiverBondState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = mBluetoothAdapter.getDefaultAdapter();

        onOffButton = findViewById(R.id.onOffButton);
        discoverabilityButton = findViewById(R.id.discoverabilityButton);
        discoverButton = findViewById(R.id.listButton);
        deviceList = findViewById(R.id.deviceList);
        mBTDevices = new ArrayList<>();
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        btState = findViewById(R.id.bt_state);
        dataReceived = findViewById(R.id.data_received);
        sendText = findViewById(R.id.sendText);
        btSelectedText = findViewById(R.id.bt_selected_text);
        btSelected = findViewById(R.id.bt_selected);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiverBondState, filter);

        deviceList.setOnItemClickListener(MainActivity.this);

        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBluetooth();
            }
        });

        discoverabilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Enabling discoverability.");
                toggleDiscoverability();
            }
        });

        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Discovering devices...");
                mBTDevices.clear();
                btSelectedText.setText("Selected: ");
                btSelected.setText("NONE");
                discoverDevices();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
                btState.setText(mBTDevice.getName());
                btSelectedText.setText("");
                btSelected.setText("");
                sendText.setVisibility(View.VISIBLE);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = sendText.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
                mBluetoothConnection.read();
                dataReceived.setText(mBluetoothConnection.getUserInput());
                sendText.setText("");
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                killConnection();
            }
        });
    }

    public void startConnection(){
        startBluetoothConnection(mBTDevice, MY_UUID_INSECURE);
    }

    public void killConnection(){
        if(mBluetoothConnection != null){
            mBluetoothConnection.killClient();
            mBluetoothConnection = null;
        }
    }

    public void startBluetoothConnection(BluetoothDevice device, UUID uuid){
        mBluetoothConnection.startClient(device, uuid);
    }

    public void discoverDevices(){
        Log.d(TAG, "discoverButton: Looking for unpaired devices...");

        if(mBluetoothAdapter.isDiscovering()){
            Log.d(TAG, "discoverButton: Canceling discovery.");
            mBluetoothAdapter.cancelDiscovery();

            //Check bluetooth permissions in manifest
            checkBTPermissions();

            Log.d(TAG, "discoverButton: Enabling discovery.");
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiverDeviceFound, discoverDevicesIntent);
        }

        if(!mBluetoothAdapter.isDiscovering()){
            //Check bluetooth permissions in manifest
            checkBTPermissions();

            Log.d(TAG, "discoverButton: Enabling discovery.");
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiverDeviceFound, discoverDevicesIntent);
        }
    }

    @SuppressLint("NewApi")
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    public void enableDisableBluetooth(){
        if(mBluetoothAdapter == null){
            Log.d(TAG,"onOffButton: No bluetooth adapter.");
        }

        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "onOffButton: enabling bluetooth.");
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetoothIntent);

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiverBTState, bluetoothIntent);
        }

        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "onOffButton: disabling bluetooth.");
            mBluetoothAdapter.disable();

            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiverBTState, bluetoothIntent);
        }
    }

    public void toggleDiscoverability() {
        Log.d(TAG, "discoverabilityButton: Making device discoverable for 300 seconds.");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiverDiscoverabilityState, intentFilter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Cancel discovery to save memory
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: Item Clicked!");

        String deviceName = mBTDevices.get(position).getName();
        String deviceAddress = mBTDevices.get(position).getAddress();
        btSelected.setText(deviceName);
        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //Create the bond
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(position).createBond();

            mBTDevice = mBTDevices.get(position);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }
}