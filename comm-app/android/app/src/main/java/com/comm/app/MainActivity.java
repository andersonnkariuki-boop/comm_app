package com.comm.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "SERIAL_APP";
    private static final String ACTION_USB_PERMISSION =
            "com.comm.app.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbSerialPort serialPort;

    private Thread serialThread;
    private volatile boolean serialRunning = false;

    private UsbDevice connectedDevice;

    private int baudRate = 115200;
    private boolean hexMode = false;

    private final StringBuilder rxBuffer = new StringBuilder();

    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        registerUsbReceiver();

        bridge.getWebView().addJavascriptInterface(
                new SerialBridge(),
                "SerialAndroid"
        );

        Log.d(TAG, "Serial bridge initialized");

        detectDevices();
    }

    // =====================================================
    // USB RECEIVER SAFE REGISTRATION
    // =====================================================

    private void registerUsbReceiver() {

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }
    }

    // =====================================================
    // DEVICE DETECTION
    // =====================================================

    private void detectDevices() {

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            sendStatusToUI("No USB devices found");
            return;
        }

        for (UsbDevice device : deviceList.values()) {
            sendStatusToUI("Device: " + device.getDeviceName());
            requestUsbPermission(device);
        }
    }

    // =====================================================
    // PERMISSION REQUEST
    // =====================================================

    private void requestUsbPermission(UsbDevice device) {

        PendingIntent intent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );

        usbManager.requestPermission(device, intent);
    }

    // =====================================================
    // USB RECEIVER
    // =====================================================

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (!ACTION_USB_PERMISSION.equals(intent.getAction()))
                return;

            UsbDevice device =
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            boolean granted =
                    intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED,
                            false
                    );

            if (granted && device != null) {

                connectedDevice = device;

                sendStatusToUI("USB Permission Granted");

                connectSerial(device, baudRate);

            } else {
                sendStatusToUI("USB Permission Denied");
            }
        }
    };

    // =====================================================
    // CONNECT SERIAL
    // =====================================================

    private void connectSerial(UsbDevice device, int baud) {

        try {

            List<UsbSerialDriver> drivers =
                    UsbSerialProber.getDefaultProber()
                            .findAllDrivers(usbManager);

            UsbSerialDriver selected = null;

            for (UsbSerialDriver d : drivers) {
                if (d.getDevice().getDeviceId() == device.getDeviceId()) {
                    selected = d;
                    break;
                }
            }

            if (selected == null) {
                sendStatusToUI("No serial driver found");
                return;
            }

            serialPort = selected.getPorts().get(0);

            if (!serialPort.isOpen()) {
                serialPort.open(usbManager.openDevice(selected.getDevice()));
            }

            serialPort.setParameters(
                    baud,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
            );

            sendStatusToUI("Connected @ " + baud);

            startSerialReader();

        } catch (Exception e) {
            sendStatusToUI("Connection error");
            Log.e(TAG, "Connect Error: " + e.getMessage());
        }
    }

    // =====================================================
    // SERIAL READER (STABLE STREAM HANDLING)
    // =====================================================

    private void startSerialReader() {

        serialRunning = true;

        serialThread = new Thread(() -> {

            byte[] buffer = new byte[1024];

            while (serialRunning) {

                try {

                    if (serialPort == null) continue;

                    int len = serialPort.read(buffer, 1000);

                    if (len > 0) {

                        String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);

                        rxBuffer.append(chunk);

                        String data = rxBuffer.toString();

                        int index;

                        while ((index = findLineBreak(data)) != -1) {

                            String line = data.substring(0, index);

                            data = data.substring(index + 1);

                            sendToUI(line.trim());
                        }

                        rxBuffer.setLength(0);
                        rxBuffer.append(data);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "RX Error: " + e.getMessage());
                    serialRunning = false;
                }
            }
        });

        serialThread.start();
    }

    private int findLineBreak(String data) {

        int n = data.indexOf("\n");
        if (n != -1) return n;

        int r = data.indexOf("\r");
        if (r != -1) return r;

        return -1;
    }

    // =====================================================
    // SEND SERIAL
    // =====================================================

    public void sendSerial(String message) {

        try {

            if (serialPort == null) {
                sendStatusToUI("No connection");
                return;
            }

            byte[] data = hexMode
                    ? hexStringToByteArray(message)
                    : message.getBytes(StandardCharsets.UTF_8);

            serialPort.write(data, 1000);

        } catch (Exception e) {
            sendStatusToUI("Write error");
            Log.e(TAG, e.getMessage());
        }
    }

    // =====================================================
    // UI EVENTS
    // =====================================================

    private void sendToUI(String data) {

        JSObject obj = new JSObject();
        obj.put("data", data);
        obj.put("timestamp", System.currentTimeMillis());

        getBridge().triggerWindowJSEvent(
                "serialData",
                obj.toString()
        );
    }

    private void sendStatusToUI(String status) {

        JSObject obj = new JSObject();
        obj.put("status", status);

        getBridge().triggerWindowJSEvent(
                "serialStatus",
                obj.toString()
        );
    }

    // =====================================================
    // HEX
    // =====================================================

    private byte[] hexStringToByteArray(String s) {

        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    // =====================================================
    // JS BRIDGE
    // =====================================================

    public class SerialBridge {

        public void send(String msg) {
            new Handler(Looper.getMainLooper())
                    .post(() -> sendSerial(msg));
        }

        public void connect(int baud) {
            baudRate = baud;

            if (connectedDevice != null) {
                connectSerial(connectedDevice, baudRate);
            }
        }

        public void setHex(boolean value) {
            hexMode = value;
        }

        public void disconnect() {
            serialRunning = false;

            try {
                if (serialPort != null) {
                    serialPort.close();
                    serialPort = null;
                }
            } catch (Exception ignored) {}
        }
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    public void onDestroy() {

        super.onDestroy();

        serialRunning = false;

        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}
    }
}