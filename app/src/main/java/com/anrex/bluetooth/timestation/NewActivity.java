package com.anrex.bluetooth.timestation;

import android.bluetooth.BluetoothDevice;

import com.anrex.bluetooth.timestation.bluetooth.ScanResultsConsumer;

public class NewActivity implements ScanResultsConsumer {


    @Override
    public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi) {

    }

    @Override
    public void scanningStarted() {

    }

    @Override
    public void scanningStopped() {

    }
}