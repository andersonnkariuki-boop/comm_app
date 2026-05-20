package com.comm.app;

import android.hardware.usb.*;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;

import java.util.HashMap;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "SERIAL_APP";

    private UsbManager usbManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        detectDevices();
    }

    private void detectDevices() {

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            Log.d(TAG, "No USB devices found");
            return;
        }

        for (UsbDevice device : deviceList.values()) {

            Log.d(TAG, "DEVICE FOUND");
            Log.d(TAG, "Name: " + device.getDeviceName());
            Log.d(TAG, "VID: " + device.getVendorId());
            Log.d(TAG, "PID: " + device.getProductId());
        }
    }
}