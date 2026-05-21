package com.comm.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BridgeActivity {

    // =====================================================
    // CONSTANTS
    // =====================================================

    private static final String TAG = "SERIAL_APP";

    private static final String ACTION_USB_PERMISSION =
            "com.comm.app.USB_PERMISSION";

    // =====================================================
    // SERIAL VARIABLES
    // =====================================================

    private UsbManager usbManager;

    private UsbSerialPort serialPort;

    private Thread serialThread;

    private volatile boolean serialRunning = false;

    private UsbDevice connectedDevice;

    private int baudRate = 115200;

    private boolean hexMode = false;

    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        usbManager =
                (UsbManager) getSystemService(Context.USB_SERVICE);

        // REGISTER USB RECEIVER
        registerReceiver(
                usbReceiver,
                new IntentFilter(ACTION_USB_PERMISSION)
        );

        // ADD JS BRIDGE
        bridge.getWebView().addJavascriptInterface(
                new SerialBridge(),
                "SerialAndroid"
        );

        Log.d(TAG, "SerialAndroid bridge injected");

        detectDevices();
    }

    // =====================================================
    // DEVICE DETECTION
    // =====================================================

    private void detectDevices() {

        HashMap<String, UsbDevice> deviceList =
                usbManager.getDeviceList();

        if(deviceList.isEmpty()) {

            Log.d(TAG, "No USB devices found");

            sendStatusToUI("No USB devices found");

            return;
        }

        for(UsbDevice device : deviceList.values()) {

            Log.d(
                    TAG,
                    "DEVICE FOUND: "
                            + device.getDeviceName()
            );

            sendStatusToUI(
                    "Device detected: "
                            + device.getDeviceName()
            );

            requestUsbPermission(device);
        }
    }

    // =====================================================
    // USB PERMISSION
    // =====================================================

    private void requestUsbPermission(UsbDevice device) {

        PendingIntent permissionIntent =
                PendingIntent.getBroadcast(
                        this,
                        0,
                        new Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE
                );

        usbManager.requestPermission(
                device,
                permissionIntent
        );
    }

    // =====================================================
    // USB RECEIVER
    // =====================================================

    private final BroadcastReceiver usbReceiver =
            new BroadcastReceiver() {

        @Override
        public void onReceive(
                Context context,
                Intent intent
        ) {

            if(!ACTION_USB_PERMISSION.equals(
                    intent.getAction()))
                return;

            synchronized(this) {

                UsbDevice device =
                        intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE
                        );

                boolean granted =
                        intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED,
                                false
                        );

                if(granted && device != null) {

                    Log.d(
                            TAG,
                            "USB Permission Granted"
                    );

                    connectedDevice = device;

                    connectSerial(device, baudRate);

                } else {

                    Log.d(
                            TAG,
                            "USB Permission Denied"
                    );

                    sendStatusToUI(
                            "USB Permission Denied"
                    );
                }
            }
        }
    };

    // =====================================================
    // CONNECT SERIAL
    // =====================================================

    private void connectSerial(
            UsbDevice device,
            int baud
    ) {

        try {

            List<UsbSerialDriver> drivers =
                    UsbSerialProber.getDefaultProber()
                            .findAllDrivers(usbManager);

            UsbSerialDriver selectedDriver = null;

            for(UsbSerialDriver d : drivers) {

                if(d.getDevice().getDeviceId()
                        == device.getDeviceId()) {

                    selectedDriver = d;

                    break;
                }
            }

            if(selectedDriver == null) {

                sendStatusToUI(
                        "No matching serial driver"
                );

                return;
            }

            serialPort =
                    selectedDriver.getPorts().get(0);

            if(usbManager.openDevice(
                    selectedDriver.getDevice()) == null) {

                sendStatusToUI(
                        "Failed opening device"
                );

                return;
            }

            serialPort.open(
                    usbManager.openDevice(
                            selectedDriver.getDevice()
                    )
            );

            serialPort.setParameters(
                    baud,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
            );

            sendStatusToUI(
                    "Connected @ " + baud
            );

            Log.d(
                    TAG,
                    "Serial Connected @ " + baud
            );

            startSerialReader();

        } catch(Exception e) {

            Log.e(
                    TAG,
                    "Connection Error: "
                            + e.getMessage()
            );

            sendStatusToUI(
                    "Connection Error"
            );
        }
    }

    // =====================================================
    // SERIAL READER
    // =====================================================

    private void startSerialReader() {

        serialRunning = true;

        serialThread = new Thread(() -> {

            byte[] buffer = new byte[1024];

            while(serialRunning) {

                try {

                    int len =
                            serialPort.read(buffer, 1000);

                    if(len > 0) {

                        String data =
                                new String(
                                        buffer,
                                        0,
                                        len,
                                        StandardCharsets.UTF_8
                                );

                        Log.d(TAG, "RX: " + data);

                        sendToUI(data);
                    }

                } catch(IOException e) {

                    Log.e(
                            TAG,
                            "Read Error: "
                                    + e.getMessage()
                    );

                    serialRunning = false;

                    sendStatusToUI(
                            "Serial Read Error"
                    );
                }
            }
        });

        serialThread.start();
    }

    // =====================================================
    // SEND SERIAL
    // =====================================================

    public void sendSerial(String message) {

        try {

            if(serialPort == null) {

                sendStatusToUI(
                        "No serial connection"
                );

                return;
            }

            byte[] data;

            if(hexMode) {

                data =
                        hexStringToByteArray(message);

            } else {

                data =
                        message.getBytes(
                                StandardCharsets.UTF_8
                        );
            }

            serialPort.write(data, 1000);

            Log.d(TAG, "TX: " + message);

        } catch(Exception e) {

            Log.e(
                    TAG,
                    "Write Error: "
                            + e.getMessage()
            );

            sendStatusToUI(
                    "Write Error"
            );
        }
    }

    // =====================================================
    // SEND RX TO UI
    // =====================================================

    private void sendToUI(String data) {

        JSObject obj = new JSObject();

        obj.put("data", data);

        obj.put(
                "timestamp",
                System.currentTimeMillis()
        );

        getBridge().triggerWindowJSEvent(
                "serialData",
                obj.toString()
        );
    }

    // =====================================================
    // SEND STATUS TO UI
    // =====================================================

    private void sendStatusToUI(String status) {

        JSObject obj = new JSObject();

        obj.put("status", status);

        getBridge().triggerWindowJSEvent(
                "serialStatus",
                obj.toString()
        );
    }

    // =====================================================
    // HEX SUPPORT
    // =====================================================

    private byte[] hexStringToByteArray(String s) {

        int len = s.length();

        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {

            data[i / 2] =
                    (byte)((Character.digit(
                            s.charAt(i),
                            16) << 4)
                            +
                            Character.digit(
                                    s.charAt(i + 1),
                                    16));
        }

        return data;
    }

    // =====================================================
    // DISCONNECT
    // =====================================================

    private void disconnectSerial() {

        serialRunning = false;

        try {

            if(serialPort != null) {

                serialPort.close();

                serialPort = null;
            }

            sendStatusToUI(
                    "Serial Disconnected"
            );

            Log.d(
                    TAG,
                    "Serial Disconnected"
            );

        } catch(IOException e) {

            Log.e(
                    TAG,
                    "Disconnect Error: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // JAVASCRIPT BRIDGE
    // =====================================================

    public class SerialBridge {

        @JavascriptInterface
        public void send(String message) {

            new Handler(
                    Looper.getMainLooper()
            ).post(() -> sendSerial(message));
        }

        @JavascriptInterface
        public void connect(int baud) {

            baudRate = baud;

            if(connectedDevice != null) {

                connectSerial(
                        connectedDevice,
                        baudRate
                );
            }
        }

        @JavascriptInterface
        public void disconnect() {

            disconnectSerial();
        }
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    @Override
    public void onDestroy() {

        super.onDestroy();

        disconnectSerial();

        try {

            unregisterReceiver(usbReceiver);

        } catch(Exception ignored) {}
    }
}