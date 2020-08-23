package com.fuzhu8.inspector.vpn;

import java.net.Socket;

public interface InspectorVpn {

    boolean protect(Socket socket);

    IPacketCapture getPacketCapture();

}
