package kr.co.mdigit.bluetoothexamples.Service;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import kr.co.mdigit.bluetoothexamples.MyActivity;
import kr.co.mdigit.bluetoothexamples.ScanDevice;

/**
 * Created by yongsucho on 2014. 10. 15..
 */
public class BluetoothService extends Service {
    private final String TAG = getClass().getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private Activity activity;
    private Handler mHandler;

    public BluetoothService(Activity activity , Handler handler) {
        this.activity = activity;
        this.mHandler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        DeviceUuidFactory(activity);
    }

    /**
     * Bluetooth 지원 여부 확인용
     * @return
     */
    public boolean getDeviceStatus() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth is not available");
            return false;
        } else {
            Log.d(TAG, "Bluetooth is available");
            return true;
        }
    }
    public void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            scanDevice();
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, MyActivity.ENABLE_BLUETOOTH);
        }
    }

    public void scanDevice() {
        Log.d(TAG, "Scan Device");
        Intent intent = new Intent(activity, ScanDevice.class);
        activity.startActivityForResult(intent, MyActivity.SCAN_DEVICE);
    }
    BluetoothDevice btSelectedDevice = null;
    boolean setDevice(String address) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
                if (device.getAddress().equals(address)){
                    btSelectedDevice = device;
                    return true;
                }
            }
        }
        return false;
    }
    public void getDeviceInfo(String data) {
        if (setDevice(data)) {
            Log.d(TAG, "Device Selected OK!!");

            connectThread = new ConnectThread(btSelectedDevice);
            connectThread.run();

        } else {
            Log.d(TAG, "Cannot Find Device!!");
        }
    }
    protected static final String PREFS_FILE = "device_id.xml";
    protected static final String PREFS_DEVICE_ID = "device_id";

    protected static UUID uuid;

    public void DeviceUuidFactory(Context context) {

        if (uuid == null) {
            if (uuid == null) {
                final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
                final String id = prefs.getString(PREFS_DEVICE_ID, null);

                if (id != null) {
                    // Use the ids previously computed and stored in the prefs file
                    uuid = UUID.fromString(id);

                } else {

                    final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                    // Use the Android ID unless it's broken, in which case fallback on deviceId,
                    // unless it's not available, then fallback on a random number which we store
                    // to a prefs file
                    try {
                        if (!"9774d56d682e549c".equals(androidId)) {
                            uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                        } else {
                            final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                            uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }

                    // Write the value out to the prefs file
                    prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).commit();
                }
            }
        }
    }
    ConnectedThread connectedThread = null;
    ConnectThread connectThread = null;
    private void managedConnectedSocket(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.run();
    }

    public void writeSocket(byte[] bytes) {
        if (connectedThread != null){
            connectedThread.write(bytes);
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectThread(BluetoothDevice device) {
            btDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch(IOException ie) {
                Log.d(TAG, "Create Socket Error!!");
            }
            btSocket = tmp;
        }
        public void run(){
            bluetoothAdapter.cancelDiscovery();
            try{
                if (btSocket != null) {
                    btSocket.connect();
                }
            }catch (IOException ie) {
                try{
                    btSocket.close();
                } catch (IOException e) {

                }
                return;
            }
            managedConnectedSocket(btSocket);
        }
        public void cancel() {
            try{
                btSocket.close();
            } catch(IOException ie) {

            }
        }
    }
    class ConnectedThread extends Thread {
        BluetoothSocket socket;
        InputStream inStream = null;
        OutputStream outStream = null;
        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            try{
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
            }catch (IOException ie) {
                Log.d(TAG, "Get Stream Error!!");
            }
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;

            while(true) {
                try{
                    bytes = inStream.read(buffer);
                    mHandler.obtainMessage(MyActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }catch(IOException ie){
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try{
                outStream.write(bytes);
            }catch(IOException ie) {

            }
        }
        public void cancel(){
            try{
                inStream.close();
                outStream.close();
                socket.close();
            } catch (IOException ie){

            }
        }
    }
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

}
