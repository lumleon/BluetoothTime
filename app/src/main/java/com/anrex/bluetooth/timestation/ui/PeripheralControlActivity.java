package com.anrex.bluetooth.timestation.ui;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.anrex.bluetooth.timestation.audio.AlarmManager;
import com.anrex.bluetooth.timestation.Constants;
import com.anrex.bluetooth.timestation.bluetooth.BleAdapterService;
import com.litesuits.bluetooth.utils.HexUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by leonlum on 17/07/03.
 */

public class PeripheralControlActivity extends Activity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    private String device_name;
    private String device_address;
    private Timer mTimer;
    private boolean sound_alarm_on_disconnect = false;
    private int alert_level;
    private boolean back_requested = false;
    private boolean share_with_server = false;
    private Switch share_switch;
    private BleAdapterService bluetooth_le_adapter;



    byte[] buffer;

     private final ServiceConnection service_connection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName componentName, IBinder service) {
             bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
             bluetooth_le_adapter.setActivityHandler(message_handler); }
         @Override
         public void onServiceDisconnected(ComponentName componentName) {
             bluetooth_le_adapter = null;
         }
     };


    private Handler message_handler = new Handler() {
        @Override
    public void handleMessage(Message msg) {
        Bundle bundle;
        String service_uuid = "0000FFE0-0000-1000-8000-00805f9b34fb";
        String characteristic_uuid = "0000FFE1-0000-1000-8000-00805f9b34fb";
        byte[] b = null;
        // message handling logic
        switch (msg.what) {
            case BleAdapterService.MESSAGE:
                bundle = msg.getData();
                String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                showMsg(text);
                break;

            case BleAdapterService.GATT_CONNECTED:
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.connectButton)).setEnabled(false);
                // we're connected
                showMsg("CONNECTED");

                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.noiseButton))
                        .setEnabled(true);
                share_switch.setEnabled(true);


                // enable the LOW/MID/HIGH alert level selection buttons
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setEnabled(true);
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setEnabled(true);
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setEnabled(true);

                //stop alarm on connected
                AlarmManager am = AlarmManager.getInstance();
                Log.d(Constants.TAG, "alarmIsSounding=" + am.alarmIsSounding());
                if (am.alarmIsSounding()) {
                Log.d(Constants.TAG, "Stopping alarm");
                am.stopAlarm();
            }

                bluetooth_le_adapter.discoverServices();
                break;
            case BleAdapterService.GATT_DISCONNECT:
                ((Button) PeripheralControlActivity.this
                    .findViewById(com.anrex.bluetooth.timestation.R.id.connectButton)).setEnabled(true);
                // we're disconnected
                showMsg("DISCONNECTED");

                share_switch.setEnabled(true);

                // hide the rssi distance colored rectangle
                 ((LinearLayout) PeripheralControlActivity.this
                                .findViewById(com.anrex.bluetooth.timestation.R.id.rectangle))
                                .setVisibility(View.INVISIBLE);
                // disable the LOW/MID/HIGH alert level selection buttons
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setEnabled(false);
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setEnabled(false);
                ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setEnabled(false);

                // stop the rssi reading timer
                stopTimer();

                if (alert_level > 0) {
                    AlarmManager.getInstance().soundAlarm(getResources().openRawResourceFd(com.anrex.bluetooth.timestation.R.raw.alarm)); }

                if (back_requested) {
                    PeripheralControlActivity.this.finish();
                }

                break;
            case BleAdapterService.GATT_SERVICES_DISCOVERED:

                // validate services and if ok....
             List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                boolean link_loss_present=false;
                boolean immediate_alert_present=false;
                boolean tx_power_present=false;
                boolean proximity_monitoring_present=false;
                for (BluetoothGattService svc : slist) {
                    Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                    if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                        link_loss_present = true;
                        continue; }
                    if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.IMMEDIATE_ALERT_SERVICE_UUID)) {
                        immediate_alert_present = true;
                        continue; }
                    if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.TX_POWER_SERVICE_UUID)) {
                        tx_power_present = true;
                        continue; }
                    if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID)) {
                        proximity_monitoring_present = true;
                        continue; }
                }
                if (link_loss_present && immediate_alert_present && tx_power_present && proximity_monitoring_present) {
                    showMsg("Device has expected services");

            // show the rssi distance colored rectangle
                    ((LinearLayout) PeripheralControlActivity.this
                                                    .findViewById(com.anrex.bluetooth.timestation.R.id.rectangle))
                                                    .setVisibility(View.VISIBLE);
            // enable the LOW/MID/HIGH alert level selection buttons
                    ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setEnabled(true);
                    ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setEnabled(true);
                    ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setEnabled(true);

                    bluetooth_le_adapter.readCharacteristic(
                            BleAdapterService.LINK_LOSS_SERVICE_UUID,
                            BleAdapterService.ALERT_LEVEL_CHARACTERISTIC);
                } else {
                    // show the rssi distance colored rectangle
                    ((LinearLayout) PeripheralControlActivity.this
                            .findViewById(com.anrex.bluetooth.timestation.R.id.rectangle))
                            .setVisibility(View.VISIBLE);
                    startReadRssiTimer();
                    showMsg("Device does not have expected GATT services");

                }
                break;
            case BleAdapterService.GATT_CHARACTERISTIC_READ:
                bundle = msg.getData();
                Log.d(Constants.TAG, "Service=" + bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase()
                        + " Characteristic=" + bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());
                if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase()
                        .equals(BleAdapterService.ALERT_LEVEL_CHARACTERISTIC)
                        && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase()
                        .equals(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    if (b.length > 0) {
                        PeripheralControlActivity.this.setAlertLevel((int) b[0]);
                        // show the rssi distance colored rectangle
                         ((LinearLayout) PeripheralControlActivity.this
                            .findViewById(com.anrex.bluetooth.timestation.R.id.rectangle))
                                 .setVisibility(View.VISIBLE);
                        // start off the rssi reading timer
                         startReadRssiTimer();
                    }
                }
                break;
            case BleAdapterService.GATT_REMOTE_RSSI:
                bundle = msg.getData();
                int rssi = bundle.getInt(BleAdapterService.PARCEL_RSSI);
                PeripheralControlActivity.this.updateRssi(rssi);
                break;
            case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                bundle = msg.getData();
                if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                        .toUpperCase().equals(BleAdapterService.ALERT_LEVEL_CHARACTERISTIC)
                        && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase()
                        .equals(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);

                    if (b.length > 0) {
                        PeripheralControlActivity.this.setAlertLevel((int) b[0]);
                    }
                }
                break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.anrex.bluetooth.timestation.R.layout.activity_peripheral_control);

        // read intent data
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(EXTRA_NAME);
        device_address = intent.getStringExtra(EXTRA_ID);

        // show the device name
        ((TextView) this.findViewById(com.anrex.bluetooth.timestation.R.id.nameTextView)).setText("Device : "+device_name+" ["+device_address+"]");

        // hide the coloured rectangle used to show green/amber/red rssi distance
        ((LinearLayout) this.findViewById(com.anrex.bluetooth.timestation.R.id.rectangle)).setVisibility(View.INVISIBLE);

        // enable the noise button
        ((Button) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.noiseButton)).setEnabled(true);

         // disable the LOW/MID/HIGH alert level selection buttons

        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setEnabled(false);
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setEnabled(false);
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setEnabled(false);

        share_switch = (Switch) this.findViewById(com.anrex.bluetooth.timestation.R.id.switch1);
        share_switch.setEnabled(false);

        share_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // we'll complete this later
                if (bluetooth_le_adapter != null) {
                    share_with_server = isChecked;
                    if (!isChecked && bluetooth_le_adapter.isConnected()) {
                        showMsg("Switched off sharing proximity data");
                    // write 0,0 to cause Arduino to switch off all LEDs
                    if (bluetooth_le_adapter.writeCharacteristic(
                        BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID, BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC, new byte[] { 0 , 0 })) {
                        } else {
                            showMsg("Failed to inform Arduino sharing has been disabled");
                        }
                    }
                }

            }
        });
// connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, service_connection, BIND_AUTO_CREATE);
        showMsg("READY");
    }

    private void startReadRssiTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
        @Override
        public void run() {
            bluetooth_le_adapter.readRemoteRssi();
                }
            }, 0, 2000);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void updateRssi(int rssi) {
        ((TextView) findViewById(com.anrex.bluetooth.timestation.R.id.rssiTextView)).setText("RSSI = " + Integer.toString(rssi));
        LinearLayout layout = ((LinearLayout) PeripheralControlActivity.this.findViewById(com.anrex.bluetooth.timestation.R.id.rectangle));
        byte proximity_band = 3;
        if (rssi < -80) {
            layout.setBackgroundColor(0xFFFF0000);
            writeGPs();
        } else if (rssi < -50) {
            layout.setBackgroundColor(0xFFFF8A01);
            proximity_band = 2;
        } else {
            layout.setBackgroundColor(0xFF00FF00);
            proximity_band = 1;
        }
        layout.invalidate();

        if (share_with_server) {
            if (bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID, BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
                    new byte[] { proximity_band, (byte) rssi })) {
                showMsg("proximity data shared: proximity_band:" + proximity_band + ",rssi:" + rssi);
            } else {
                showMsg("Failed to share proximity data");
            }
        }


    }

    private void writeGPs(){

        byte gpstime =  (byte)123456789;
        byte receivedtime = Byte.valueOf("receivedtime");

         if (bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID, BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
                new byte[] {gpstime})) {
            showMsg("sent GPS Time: " + gpstime );
        } else {
            showMsg("Failed to share proximity data");
        }

    }



    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg); runOnUiThread(new Runnable() {
        @Override
        public void run() {
            ((TextView) findViewById(com.anrex.bluetooth.timestation.R.id.msgTextView)).setText(msg);
        }
    });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        unbindService(service_connection);
        bluetooth_le_adapter = null;
    }

    public void onConnect(View view) {
        showMsg("onConnect");
        if (bluetooth_le_adapter != null) {
            if (bluetooth_le_adapter.connect(device_address)) {
                ((Button) PeripheralControlActivity.this
                    .findViewById(com.anrex.bluetooth.timestation.R.id.connectButton)).setEnabled(false);
            } else {
                showMsg("onConnect: failed to connect");
            }
        } else {
            showMsg("onConnect: bluetooth_le_adapter=null"); }
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed"); back_requested = true;
        if (bluetooth_le_adapter.isConnected()) {
            try { bluetooth_le_adapter.disconnect();
            } catch (Exception e) {
            }
        } else {
            finish(); }
    }

    private void setAlertLevel(int alert_level) {
        this.alert_level = alert_level;
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setTextColor(Color.parseColor("#000000"));
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setTextColor(Color.parseColor("#000000"));
        ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setTextColor(Color.parseColor("#000000"));
        switch (alert_level) {
            case 0:
                ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.lowButton)).setTextColor(Color.parseColor("#FF0000"));
                break;
            case 1:
                ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.midButton)).setTextColor(Color.parseColor("#FF0000"));
                break;
            case 2:
                ((Button) this.findViewById(com.anrex.bluetooth.timestation.R.id.highButton)).setTextColor(Color.parseColor("#FF0000"));
         }
    }

    public void onLow(View view) {

        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                BleAdapterService.ALERT_LEVEL_CHARACTERISTIC,
                Constants.ALERT_LEVEL_LOW );




    }
    public void onMid(View view) {
        bluetooth_le_adapter.writeCharacteristic(
                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                BleAdapterService.ALERT_LEVEL_CHARACTERISTIC,
                Constants.ALERT_LEVEL_MID
        );
    }
    public void onHigh(View view) {
        bluetooth_le_adapter.writeCharacteristic(

                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                BleAdapterService.ALERT_LEVEL_CHARACTERISTIC,
                Constants.ALERT_LEVEL_HIGH );

    }
    public void onNoise(View view) {

            byte [] al = new byte[1];
            al[0] = (byte) alert_level; bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.IMMEDIATE_ALERT_SERVICE_UUID,
                    BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, al );
        }
    }


