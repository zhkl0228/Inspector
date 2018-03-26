package com.fuzhu8.inspector.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.fuzhu8.inspector.vpn.IPacketCapture;

/**
 * inspector broadcast receiver
 * Created by zhkl0228 on 2017/2/5.
 */

public class InspectorBroadcastReceiver extends BroadcastReceiver {

    private final InspectorBroadcastListener listener;

    public InspectorBroadcastReceiver(InspectorBroadcastListener listener) {
        super();

        this.listener = listener;
    }

    public static final String UID_KEY = "uid";
    public static final String PACKAGE_NAME_KEY = "packageName";
    public static final String PACKET_CAPTURE_KEY = "packetCapture";
    public static final String PID_KEY = "pid";
    public static final String PROCESS_NAME_KEY = "processName";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener == null) {
            return;
        }

        Bundle bundle = intent.getBundleExtra(Bundle.class.getCanonicalName());
        if (bundle == null) {
            bundle = new Bundle();
        }
        IBinder binder = bundle.getBinder(PACKET_CAPTURE_KEY);
        if (InspectorBroadcastListener.CONSOLE_CONNECTED.equals(intent.getAction())) {
            listener.onConsoleConnected(bundle.getInt(UID_KEY), bundle.getString(PACKAGE_NAME_KEY), binder == null ? null : IPacketCapture.Stub.asInterface(binder), bundle.getInt(PID_KEY), bundle.getString(PROCESS_NAME_KEY));
        } else if (InspectorBroadcastListener.CONSOLE_DISCONNECTED.equals(intent.getAction())) {
            listener.onConsoleDisconnected(bundle.getInt(UID_KEY), bundle.getString(PACKAGE_NAME_KEY), bundle.getInt(PID_KEY), bundle.getString(PROCESS_NAME_KEY));
        } else if (InspectorBroadcastListener.ACTIVITY_RESUME.equals(intent.getAction())) {
            listener.onActivityResume(bundle.getInt(UID_KEY), bundle.getString(PACKAGE_NAME_KEY), binder == null ? null : IPacketCapture.Stub.asInterface(binder), bundle.getInt(PID_KEY), bundle.getString(PROCESS_NAME_KEY));
        } else if (InspectorBroadcastListener.REQUEST_STOP_VPN.equals(intent.getAction())) {
            listener.onRequestStopVpn();
        }
    }
}
