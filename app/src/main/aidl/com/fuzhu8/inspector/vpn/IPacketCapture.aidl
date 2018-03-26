// IPacketCapture.aidl
package com.fuzhu8.inspector.vpn;

// Declare any non-default types here with import statements

interface IPacketCapture {

    /**
     * @return true表示redirectRules有更新
     */
   	boolean onPacket(in byte[] packetData, in String type, int datalink);

    /**
     * example: *:443->localhost:8888,*:8443->localhost:8888,120.35.*.*:8643->localhost:8888
     */
   	String getTcpRedirectRules();

}
