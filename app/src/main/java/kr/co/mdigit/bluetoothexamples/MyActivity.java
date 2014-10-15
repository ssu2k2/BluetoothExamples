package kr.co.mdigit.bluetoothexamples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import kr.co.mdigit.bluetoothexamples.Service.BluetoothService;


public class MyActivity extends Activity implements View.OnClickListener{

    private  final String TAG = getClass().getSimpleName();
    private BluetoothService bluetoothService = null;

    public static final int ENABLE_BLUETOOTH    = 0x0100;
    public static final int SCAN_DEVICE         = 0x0101;

    public static final String DEVICE_ADDRESS   = "DEVICE_ADDRESS";

    public static final int MESSAGE_READ = 0x0200;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    int size = msg.arg1;
                    byte[] bytes = (byte[])msg.obj;

                    Log.d(TAG, "Get Message Size:" + size);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initLayout();

        if (bluetoothService == null) {
            bluetoothService = new BluetoothService(this, mHandler);
        }
    }
    private void initLayout() {
        ((Button)findViewById(R.id.btnConnect)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnConnect:
                if (bluetoothService != null) {
                    if(bluetoothService.getDeviceStatus()) {
                        bluetoothService.enableBluetooth();
                    }
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                bluetoothService.scanDevice();
            } else {
                Log.d(TAG, "Bluetooth is not enabled");
            }
        } else if (requestCode == SCAN_DEVICE) {
            if (resultCode == RESULT_OK) {
                String address = data.getStringExtra(DEVICE_ADDRESS);
                bluetoothService.getDeviceInfo(address);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
