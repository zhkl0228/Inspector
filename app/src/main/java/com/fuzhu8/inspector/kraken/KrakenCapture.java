package com.fuzhu8.inspector.kraken;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.PcapInputStream;
import org.krakenapps.pcap.live.PcapDeviceManager;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;

public class KrakenCapture implements Runnable {

    private static final String TAG = KrakenCapture.class.getSimpleName();

    public static final String KRAKEN_CAPTURE_ON_PACKET = KrakenCapture.class.getName() + ".on_packet";
    public static final String PACKET_KEY = "packet";
    public static final String DLT_KEY = "dlt";

    private final ActivityManager activityManager;

    private KrakenCapture(IActivityManager activityManager) {
        super();
        this.activityManager = ActivityManagerFactory.newActivityManager(activityManager);
    }

    @Override
    public void run() {
        PcapInputStream pcap = null;
        try {
            pcap = PcapDeviceManager.open("any", Integer.MAX_VALUE);
            int datalink = pcap.datalink();

            while (activityManager != null) {
                PcapPacket packet = pcap.getPacket();
                Buffer buffer = packet.getPacketData();
                byte[] bb = new byte[buffer.readableBytes()];
                buffer.gets(bb);
                sendPacket(bb, datalink);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, throwable);
        } finally {
            Log.d(TAG, "exit kraken capture");

            if (pcap != null) {
                IOUtils.closeQuietly(pcap);
            }
        }
    }

    private void sendPacket(byte[] packet, int datalink) throws RemoteException {
        Intent intent = new Intent(KRAKEN_CAPTURE_ON_PACKET);
        Bundle bundle = new Bundle();
        bundle.putByteArray(PACKET_KEY, packet);
        bundle.putInt(DLT_KEY, datalink);
        intent.putExtras(bundle);
        activityManager.sendBroadcast(intent);
        // Log.d(TAG, "sendPacket: intent=" + intent + ", datalink=" + datalink);
    }

    public static void main(String[] args) {
        IBinder service = ServiceManager.getService(Context.ACTIVITY_SERVICE);
        IActivityManager activityManager = ActivityManagerNative.asInterface(service);

        Thread thread = new Thread(new KrakenCapture(activityManager));
        thread.start();
    }
}
