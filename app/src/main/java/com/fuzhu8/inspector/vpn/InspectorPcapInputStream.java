package com.fuzhu8.inspector.vpn;

import android.util.Log;

import com.fuzhu8.tcpcap.PcapDLT;

import org.krakenapps.pcap.PcapInputStream;
import org.krakenapps.pcap.packet.PcapPacket;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * queued pcap input stream
 * Created by zhkl0228 on 2017/2/6.
 */

class InspectorPcapInputStream implements PcapInputStream {

    private final BlockingQueue<PcapPacket> queue = new LinkedBlockingQueue<>();

    private final int datalink;

    InspectorPcapInputStream() {
        this(PcapDLT.CONST_RAW_IP);
    }

    private InspectorPcapInputStream(int datalink) {
        super();
        this.datalink = datalink;
    }

    @Override
    public PcapPacket getPacket() throws IOException {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    void put(PcapPacket packet) {
        try {
            queue.put(packet);
        } catch (InterruptedException e) {
            Log.w(getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        queue.clear();
    }

    @Override
    public int datalink() {
        return datalink;
    }

}
