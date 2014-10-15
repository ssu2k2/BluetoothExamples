package kr.co.mdigit.bluetoothexamples;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by yongsucho on 2014. 10. 15..
 */
public class ScanDevice extends Activity {
    private final String TAG = getClass().getSimpleName();

    private BluetoothAdapter bluetoothAdapter;

    private ProgressBar progressScan;

    private ListView        lvPaired;
    private BTListAdapter   laPaired;
    ArrayList<BluetoothDevice> alPairedInfo;

    private ListView        lvDiscovered;
    private BTListAdapter   laDiscovered;
    ArrayList<BluetoothDevice> alDiscoveredInfo;

    Button btnScan;
    boolean receiverRegistered = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanlist);
        initLayout();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        getPairedDevice();

    }
    void initLayout() {
        btnScan = (Button)findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alDiscoveredInfo.clear();
                laDiscovered.notifyDataSetChanged();
                startScanning();
            }
        });

        progressScan = (ProgressBar)findViewById(R.id.progressScanning);
        alPairedInfo = new ArrayList<BluetoothDevice>();
        alDiscoveredInfo = new ArrayList<BluetoothDevice>();

        lvPaired = (ListView)findViewById(R.id.lvPaired);
        laPaired = new BTListAdapter(this, R.layout.bt_cell , alPairedInfo);
        lvPaired.setAdapter(laPaired);

        lvDiscovered = (ListView)findViewById(R.id.lvDiscovered);
        laDiscovered = new BTListAdapter(this, R.layout.bt_cell , alDiscoveredInfo);
        laDiscovered.setScanningMode(true);
        lvDiscovered.setAdapter(laDiscovered);

    }

    void getPairedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        alPairedInfo.clear();
        laPaired.notifyDataSetChanged();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
                alPairedInfo.add(device);
            }
        }
        laPaired.notifyDataSetChanged();
    }
    void stopScanning() {
        bluetoothAdapter.cancelDiscovery();
        progressScan.setVisibility(View.GONE);
        btnScan.setEnabled(true);
    }
    void startScanning() {
        progressScan.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        bluetoothAdapter.startDiscovery();
        receiverRegistered = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
                laDiscovered.notifyDataSetChanged();
            }
        }, 30000);
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
                alDiscoveredInfo.add(device);
                laDiscovered.notifyDataSetChanged();

            } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){

                alDiscoveredInfo.clear();
                laDiscovered.notifyDataSetChanged();
                getPairedDevice();
            }

        }
    };

    @Override
    public void onBackPressed() {
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        if (receiverRegistered){
            try{
                unregisterReceiver(mReceiver);
            }catch(IllegalArgumentException ie) {
                Log.d(TAG, "Receiver not registered ");
            }
            receiverRegistered = false;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try {
            if (bluetoothAdapter.isDiscovering())
                bluetoothAdapter.cancelDiscovery();
            if (receiverRegistered)
                unregisterReceiver(mReceiver);
        }catch (IllegalArgumentException e) {
            Log.d(TAG, "Receiver not registered ");
        }
        super.onDestroy();
    }

    class BTHolder {
        TextView tvName;
        TextView tvAddress;
        Button   btnConnect;
    }
    class BTListAdapter extends ArrayAdapter<BluetoothDevice>{
        private final String TAG = getClass().getSimpleName();
        LayoutInflater inflater;
        int resId;
        boolean isScanningMode = false;
        public BTListAdapter(Context context , int resId, ArrayList<BluetoothDevice> list) {
            super(context, resId, list);
            inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
            this.resId = resId;
        }
        public void setScanningMode(boolean isSanning) {
            this.isScanningMode = isSanning;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            BTHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(resId, null);
                holder = new BTHolder();
                holder.tvName = (TextView)convertView.findViewById(R.id.tvName);
                holder.tvAddress = (TextView)convertView.findViewById(R.id.tvAddr);
                holder.btnConnect = (Button)convertView.findViewById(R.id.btnConnect);
                convertView.setTag(holder);
            } else {
                holder = (BTHolder)convertView.getTag();
            }
            final BluetoothDevice info = getItem(position);
            holder.tvName.setText(info.getName());
            holder.tvAddress.setText(info.getAddress());
            if(isScanningMode){
                holder.btnConnect.setText("Connect");
                holder.btnConnect.setVisibility(View.VISIBLE);
                holder.btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick :" + position);
                        stopScanning();
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
                        receiverRegistered = true;
                        info.createBond();
                    }
                });
            } else {
                holder.btnConnect.setText("Select");
                holder.btnConnect.setVisibility(View.VISIBLE);
                holder.btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = getIntent();
                        intent.putExtra(MyActivity.DEVICE_ADDRESS, info.getAddress());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
            return convertView;
        }
    }
}
