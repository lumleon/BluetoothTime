package com.anrex.bluetooth.timestation.ui;

/*
  Created by leonlum on 17/06/28.
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.anrex.bluetooth.timestation.Constants;
import com.anrex.bluetooth.timestation.bluetooth.BleScanner;
import com.anrex.bluetooth.timestation.utils.BluetoothConnection;
import com.anrex.bluetooth.timestation.bluetooth.ScanResultsConsumer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, ScanResultsConsumer {


    /**
     * mac和服务uuid纯属测试，测试时请替换真实参数。
     */
    private static String MAC = "00:15:83:40:72:45";

    public String UUID_SERVICE = "00001801-0000-1000-8000-00805f9b34fb";

    private boolean ble_scanning = false;
    private Handler handler = new Handler();
    private ListAdapter ble_device_list_adapter;
    private BleScanner ble_scanner;
    private static final long SCAN_TIMEOUT = 5000;
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private boolean permissions_granted = false;
    private int device_count = 0;
    private Toast toast;

    private BluetoothConnection mBc;
    private byte[] buffer;

    static class ViewHolder {
        public TextView text;
        public TextView bdaddr;
    }

    //Stop Watch
    TextClock mTextClock;

    TextView editText, mTextTimer;
    Button start, pause, reset, lap;
    Button scanbtn, connectbtn, sendbtn;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;

    int Seconds, Minutes, MilliSeconds;

    ListView mListView;

    String[] ListElements = new String[]{};

    List<String> ListElementsArrayList;

    ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.anrex.bluetooth.timestation.R.layout.activity_main);

        setButtonText();
        ble_device_list_adapter = new ListAdapter();
        mListView = this.findViewById(com.anrex.bluetooth.timestation.R.id.deviceList);
        mListView.setAdapter(ble_device_list_adapter);
        ble_scanner = new BleScanner(this.getApplicationContext());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (ble_scanning) {
                    setScanState(false);
                    ble_scanner.stopScanning();
                }
                BluetoothDevice device = ble_device_list_adapter.getDevice(position);
//
//                mBc = new BluetoothConnection(device);
//
//                buffer =  hexStringToByteArray("0x02");
//
//                mBc.run(buffer);

                if (toast != null) {
                    toast.cancel();
                }
                Intent intent = new Intent(MainActivity.this, PeripheralControlActivity.class);
                intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
                intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());

                startActivity(intent);
            }
        });


        mTextClock = findViewById(com.anrex.bluetooth.timestation.R.id.textClock);
        mTextTimer = findViewById(com.anrex.bluetooth.timestation.R.id.textTimer);
        start = findViewById(com.anrex.bluetooth.timestation.R.id.button);
        pause = findViewById(com.anrex.bluetooth.timestation.R.id.button2);
        reset = findViewById(com.anrex.bluetooth.timestation.R.id.button3);
        lap = findViewById(com.anrex.bluetooth.timestation.R.id.button4);
        editText = findViewById(com.anrex.bluetooth.timestation.R.id.editText);


        scanbtn = findViewById(com.anrex.bluetooth.timestation.R.id.buttonscan);
        connectbtn = findViewById(com.anrex.bluetooth.timestation.R.id.buttonconnect);
        sendbtn = findViewById(com.anrex.bluetooth.timestation.R.id.buttonsend);

        scanbtn.setOnClickListener(this);
        connectbtn.setOnClickListener(this);
        sendbtn.setOnClickListener(this);

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                ListElementsArrayList
        );


        mListView.setAdapter(adapter);

        handler = new Handler();

        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);

                reset.setEnabled(false);

            }
        });


        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;

                handler.removeCallbacks(runnable);

                reset.setEnabled(true);

            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MillisecondTime = 0L;
                StartTime = 0L;
                TimeBuff = 0L;
                UpdateTime = 0L;
                Seconds = 0;
                Minutes = 0;
                MilliSeconds = 0;

                mTextTimer.setText("00:00:000");

                ListElementsArrayList.clear();

                adapter.notifyDataSetChanged();
            }
        });

        lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                StringBuilder sb = new StringBuilder();

                sb.append(mTextTimer.getText().toString());
                sb.append("-");
                sb.append(mTextClock.getText().toString());
                long time = System.currentTimeMillis();

                sb.append("-");
                sb.append(time);

                // Make a new Date object. It will be initialized to the
                // current time.
                Date now = new Date();

                // Print the result of toString()
                String dateString = now.toString();
                System.out.println(" 1. " + dateString);

                // Make a SimpleDateFormat for toString()'s output. This
                // has short (text) date, a space, short (text) month, a space,
                // 2-digit date, a space, hour (0-23), minute, second, a space,
                // short timezone, a final space, and a long year.
                SimpleDateFormat format =
                        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

                // See if we can parse the output of Date.toString()
                try {
                    Date parsed = format.parse(dateString);
                    System.out.println(" 2. " + parsed.toString());
                } catch (ParseException pe) {
                    System.out.println("ERROR: Cannot parse \"" + dateString + "\"");
                }

                // Print the result of formatting the now Date to see if the result
                // is the same as the output of toString()
                System.out.println(" 3. " + format.format(now));
                ListElementsArrayList.add(sb.toString());

                adapter.notifyDataSetChanged();

            }
        });

    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void setButtonText() {
        String text = "";
        text = Constants.FIND;
        final String button_text = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) MainActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.buttonscan)).setText(button_text);
            }
        });

    }

    private void setScanState(boolean value) {
        ble_scanning = value;
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.buttonscan)).setText(value ? Constants.STOP_SCANNING : Constants.FIND);
    }


    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            mTextTimer.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };


    @Override
    public void onClick(View view) {


        switch (view.getId()) {

            case com.anrex.bluetooth.timestation.R.id.buttonscan:
                onScan();
                break;

            case com.anrex.bluetooth.timestation.R.id.buttonconnect:


                break;
            case com.anrex.bluetooth.timestation.R.id.buttonsend:


                //  sendData("12345678");

                break;
        }
    }

    public void onScan() {
        if (!ble_scanner.isScanning()) {
            device_count = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissions_granted = false;
                    requestLocationPermission();
                } else {
                    Log.i(Constants.TAG, "Location permission has already been granted. Starting Scanning....");
                    permissions_granted = true;
                }
            } else {
// the ACCESS_COARSE_LOCATION permission did not exist before M so.... permissions_granted = true;
                permissions_granted = true;
            }
            startScanning();
        } else {
            ble_scanner.stopScanning();
        }
    }

    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            Log.i(Constants.TAG, "Received response for location permission request.");
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Location permission has been granted
                Log.i(Constants.TAG, "Location permission has now been granted. Scanning.....");
                permissions_granted = true;

                if (ble_scanner.isScanning()) {
                    startScanning();
                }
            } else {
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void startScanning() {
        if (permissions_granted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ble_device_list_adapter.clear();
                    ble_device_list_adapter.notifyDataSetChanged();
                }
            });
            simpleToast(Constants.SCANNING, 2000);
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        } else {
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }

    @Override
    public void candidateBleDevice(final BluetoothDevice device, byte[] scan_record, int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ble_device_list_adapter.addDevice(device);
                ble_device_list_adapter.notifyDataSetChanged();
                device_count++;
            }
        });
    }

    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    public void scanningStopped() {

        if (toast != null) {
            toast.cancel();
        }
        setScanState(false);
    }


    private class ListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> ble_devices;

        public ListAdapter() {
            super();
            ble_devices = new ArrayList<BluetoothDevice>();

        }

        public void addDevice(BluetoothDevice device) {
            if (!ble_devices.contains(device)) {
                ble_devices.add(device);
            }
        }

        public boolean contains(BluetoothDevice device) {
            return ble_devices.contains(device);
        }

        public BluetoothDevice getDevice(int position) {
            return ble_devices.get(position);
        }

        public void clear() {
            ble_devices.clear();
        }

        @Override
        public int getCount() {
            return ble_devices.size();
        }

        @Override
        public Object getItem(int i) {
            return ble_devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = MainActivity.this.getLayoutInflater().inflate(com.anrex.bluetooth.timestation.R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(com.anrex.bluetooth.timestation.R.id.textView);
                viewHolder.bdaddr = (TextView) view.findViewById(com.anrex.bluetooth.timestation.R.id.bdaddr);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = ble_devices.get(i);


            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText("unknown device");
            }
            viewHolder.bdaddr.setText(device.getAddress());
            return view;
        }
    }
}
