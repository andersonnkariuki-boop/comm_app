package com.comm.app;

import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.*;
import android.os.*;
import android.util.Log;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.hoho.android.usbserial.driver.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "SERIAL_APP";
    private static final String ACTION_USB_PERMISSION = "com.comm.app.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbSerialPort serialPort;
    private UsbDevice connectedDevice;

    private Thread serialThread;
    private volatile boolean running = false;

    private int baudRate = 115200;
    private boolean hexMode = false;

    // 🔥 NEW: line ending control
    private String lineEnding = "none"; // none | lf | cr | crlf

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        registerReceiver(usbReceiver,
                new IntentFilter(ACTION_USB_PERMISSION));

        bridge.getWebView().addJavascriptInterface(
                new SerialBridge(),
                "SerialAndroid"
        );

        detectDevices();
    }

    // ---------------- DEVICE DETECTION ----------------

    private void detectDevices() {
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            requestPermission(device);
        }
    }

    private void requestPermission(UsbDevice device) {
        PendingIntent pi = PendingIntent.getBroadcast(
                this, 0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );

        usbManager.requestPermission(device, pi);
    }

    // ---------------- USB RECEIVER ----------------

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (!ACTION_USB_PERMISSION.equals(intent.getAction()))
                return;

            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                connectedDevice = device;
                connect(device);
            }
        }
    };

    // ---------------- CONNECT ----------------

    private void connect(UsbDevice device) {
        try {
            List<UsbSerialDriver> drivers =
                    UsbSerialProber.getDefaultProber()
                            .findAllDrivers(usbManager);

            UsbSerialDriver driver = null;

            for (UsbSerialDriver d : drivers) {
                if (d.getDevice().getDeviceId() == device.getDeviceId()) {
                    driver = d;
                    break;
                }
            }

            if (driver == null) return;

            serialPort = driver.getPorts().get(0);

            serialPort.open(usbManager.openDevice(device));
            serialPort.setParameters(
                    baudRate,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
            );

            startReader();

        } catch (Exception e) {
            Log.e(TAG, "Connect error", e);
        }
    }

    // ---------------- RX ----------------

    private void startReader() {

        running = true;

        serialThread = new Thread(() -> {

            byte[] buf = new byte[1024];

            while (running) {
                try {

                    int len = serialPort.read(buf, 1000);
                    if (len <= 0) continue;

                    String chunk = new String(buf, 0, len, StandardCharsets.UTF_8);

                    // 🔥 CRLF NORMALIZATION (IMPORTANT)
                    chunk = chunk.replace("\r\n", "\n");

                    buffer.append(chunk);

                    String data = buffer.toString();
                    int index;

                    while ((index = data.indexOf("\n")) != -1) {

                        String line = data.substring(0, index);
                        data = data.substring(index + 1);

                        sendToUI(line.trim());
                    }

                    buffer.setLength(0);
                    buffer.append(data);

                } catch (Exception e) {
                    running = false;
                }
            }
        });

        serialThread.start();
    }

    // ---------------- TX ----------------

    public void sendSerial(String msg) {
        try {
            if (serialPort == null) return;

            String finalMsg = msg;

            switch (lineEnding) {
                case "lf": finalMsg += "\n"; break;
                case "cr": finalMsg += "\r"; break;
                case "crlf": finalMsg += "\r\n"; break;
            }

            byte[] data = hexMode
                    ? hexToBytes(finalMsg)
                    : finalMsg.getBytes(StandardCharsets.UTF_8);

            serialPort.write(data, 1000);

        } catch (Exception e) {
            Log.e(TAG, "TX error", e);
        }
    }

    // ---------------- UI EVENTS ----------------

    private void sendToUI(String data) {
        JSObject obj = new JSObject();
        obj.put("data", data);
        obj.put("timestamp", System.currentTimeMillis());

        getBridge().triggerWindowJSEvent("serialData", obj.toString());
    }

    private byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            out[i / 2] =
                    (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }
        return out;
    }

    // ---------------- BRIDGE ----------------

    public class SerialBridge {

        public void send(String msg) {
            new Handler(Looper.getMainLooper())
                    .post(() -> sendSerial(msg));
        }

        public void setBaud(int baud) {
            baudRate = baud;
        }

        public void setHex(boolean value) {
            hexMode = value;
        }

        public void setLineEnding(String mode) {
            lineEnding = mode;
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}