package com.fuzhu8.inspector.vpn;

import android.util.Log;

import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.util.Buffer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Random;

/**
 * abstract SelectionKey processor
 * Created by zhkl0228 on 2017/1/19.
 */

public abstract class AbstractSelectionKeyProcessor implements SelectionKeyProcessor {

    private final Random random = new Random();
    protected final VpnProcessor vpnProcessor;

    protected AbstractSelectionKeyProcessor(VpnProcessor vpnProcessor) {
        super();

        this.vpnProcessor = vpnProcessor;
    }

    @Override
    public boolean processConnectableKey(SelectionKey key) throws IOException, VpnException {
        Log.d(getClass().getSimpleName(), "processConnectableKey channel=" + key.channel());
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean processAcceptableKey(SelectionKey key) throws IOException {
        Log.d(getClass().getSimpleName(), "processAcceptableKey channel=" + key.channel());
        throw new UnsupportedOperationException();
    }

    protected final void dispatchIpv4(Ipv4Packet ipv4, boolean forward) throws VpnException {
        Buffer result = ipv4.getBuffer();
        byte[] packet = new byte[result.readableBytes()];
        result.gets(packet);

        vpnProcessor.dispatch(packet, forward);
    }

    protected final int generateIpPacketIdentify() {
        return random.nextInt(0x7fff);
    }

}
