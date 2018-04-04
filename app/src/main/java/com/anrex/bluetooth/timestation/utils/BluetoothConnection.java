package com.anrex.bluetooth.timestation.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.anrex.bluetooth.timestation.Constants.TAG;

/**
 * Created by leonlum on 17/07/03.
 */


public class BluetoothConnection extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    byte[] buffer;

    // Unique UUID for this application, you may use different
    private static final UUID MY_UUID = UUID
       //    .fromString("00001101-0000-1000-8000-00805f9b34fb");
            .fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    public BluetoothConnection(BluetoothDevice device) {
        BluetoothSocket tmp = null;

       String dv =  device.toString();

       String tp =  Integer.toString(device.getBondState());

        Log.d("DeviceName", dv +" " + tp);
        // Get a BluetoothSocket for a connection with the given BluetoothDevice

        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

            Log.d("InSecure Socket", tmp.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        mmSocket = tmp;

        //now make the socket connection in separate thread to avoid FC
        Thread connectionThread  = new Thread(new Runnable() {

            @Override
            public void run() {
                // Always cancel discovery because it will slow down a connection
             //   mAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    if (mmSocket != null) {
                        mmSocket.connect();
                    }

                    Log.d( TAG, "connected to device");

                } catch (IOException e) {
                    //connection to device failed so close the socket

                    e.printStackTrace();
                    Log.d( TAG, "could not connect to device");

                    try {
                        mmSocket.close();
                        Log.d( TAG, "socket closed");
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        Log.d( TAG, "socket could not be closed");
                    }

                }
            }
        });

        connectionThread.start();

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = mmSocket.getInputStream();
            tmpOut = mmSocket.getOutputStream();

        Log.d("Bluetooth Stream","Connected" );

          //  buffer = new byte[0x02];
        } catch (IOException e) {
            e.printStackTrace();

            Log.d("Bluetooth Stream","NOT Connected" );

        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run(byte[] buffer) {

        // Keep listening to the InputStream while connected
        while (true) {
            try {
                //read the data from socket stream
                mmInStream.read(buffer);
                // Send the obtained bytes to the UI Activity
            } catch (IOException e) {
                //an exception here marks connection loss
                //send message to UI Activity
                break;
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            //write the data to socket stream
            mmOutStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}