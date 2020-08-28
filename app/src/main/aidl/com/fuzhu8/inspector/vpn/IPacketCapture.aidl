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

   	void onSSLProxyEstablish(in String clientIp, in String serverIp, int clientPort, int serverPort);
   	void onSSLProxyTX(in String clientIp, in String serverIp, int clientPort, int serverPort, in byte[] data);
   	void onSSLProxyRX(in String clientIp, in String serverIp, int clientPort, int serverPort, in byte[] data);
   	void onSSLProxyFinish(in String clientIp, in String serverIp, int clientPort, int serverPort);

}
