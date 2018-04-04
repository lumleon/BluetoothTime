package com.anrex.bluetooth.timestation.bluetooth;

/*
  Created by leonlum on 17/07/03.
 */

import android.bluetooth.BluetoothDevice;

public interface ScanResultsConsumer {

    public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi);
    public void scanningStarted();

    public void scanningStopped();
}