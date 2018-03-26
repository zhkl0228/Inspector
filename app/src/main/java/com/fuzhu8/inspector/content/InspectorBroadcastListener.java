package com.fuzhu8.inspector.content;

import com.fuzhu8.inspector.vpn.IPacketCapture;

/**
 * inspector broadcast listener
 * Created by zhkl0228 on 2017/2/5.
 */

public interface InspectorBroadcastListener {

    String CONSOLE_CONNECTED = InspectorBroadcastListener.class.getCanonicalName() + ".ConsoleConnected";

    String CONSOLE_DISCONNECTED = InspectorBroadcastListener.class.getCanonicalName() + ".ConsoleDisconnected";

    String ACTIVITY_RESUME = InspectorBroadcastListener.class.getCanonicalName() + ".ActivityResume";

    String REQUEST_STOP_VPN = InspectorBroadcastListener.class.getCanonicalName() + ".RequestStopVpn";

    void onConsoleConnected(int uid, String packageName, IPacketCapture packetCapture, int pid, String processName);

    void onConsoleDisconnected(int uid, String packageName, int pid, String processName);

    void onActivityResume(int uid, String packageName, IPacketCapture packetCapture, int pid, String processName);

    void onRequestStopVpn();

}
