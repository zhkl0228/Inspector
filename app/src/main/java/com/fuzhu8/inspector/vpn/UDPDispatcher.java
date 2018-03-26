package com.fuzhu8.inspector.vpn;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.ip.InternetProtocol;
import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.udp.UdpPacket;
import org.krakenapps.pcap.util.ChainBuffer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import cn.banny.auxiliary.Inspector;

/**
 * udp dispatcher
 * Created by zhkl0228 on 2017/1/19.
 */

public class UDPDispatcher extends AbstractSelectionKeyProcessor implements SelectionKeyProcessor {

    private final UdpPacket udp;
    private final ByteBuffer payload;
    private long openTimeInMillis;

    public UDPDispatcher(VpnProcessor vpnProcessor, UdpPacket udp, ByteBuffer payload) {
        super(vpnProcessor);

        this.udp = udp;
        this.payload = payload;
        this.openTimeInMillis = System.currentTimeMillis();
    }

    @Override
    public boolean processWritableKey(SelectionKey key) throws IOException {
        WritableByteChannel channel = (WritableByteChannel) key.channel();
        Log.d(getClass().getSimpleName(), "processWritableKey channel=" + channel);

        channel.write(payload);
        if (payload.hasRemaining()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
        return false;
    }

    @Override
    public boolean processReadableKey(SelectionKey key) throws IOException, VpnException {
        ReadableByteChannel channel = (ReadableByteChannel) key.channel();
        Log.d(getClass().getSimpleName(), "processReadableKey channel=" + channel);

        ByteBuffer buffer = vpnProcessor.sharedBuffer();
        int read = channel.read(buffer);
        if (read == -1) {
            Log.e(getClass().getSimpleName(), "processReadableKey", new EOFException("readableChannel EOF"));
        } else {
            byte[] copy = Arrays.copyOfRange(buffer.array(), 0, read);
            Log.d(getClass().getSimpleName(), Inspector.inspectString(copy, "readable data"));
            dispatch(copy);
        }
        key.cancel();
        IOUtils.closeQuietly(channel);
        return true;
    }

    @Override
    public void checkRegisteredKey(SelectionKey key, long currentTimeMillis) throws IOException {
        if (currentTimeMillis - openTimeInMillis >= TimeUnit.MINUTES.toMillis(1)) { // 1 minute
            SelectableChannel channel = key.channel();
            Log.d(getClass().getSimpleName(), "checkRegisteredKey timeout channel=" + channel);
            key.cancel();
            IOUtils.closeQuietly(channel);
        }
    }

    @Override
    public void exceptionRaised(SelectionKey key, IOException exception) throws IOException {
        SelectableChannel channel = key.channel();
        Log.d(getClass().getSimpleName(), "exceptionRaised channel=" + channel, exception);
        key.cancel();
        IOUtils.closeQuietly(channel);
    }

    private void dispatch(byte[] data) throws VpnException {
        UdpPacket.Builder builder = new UdpPacket.Builder();
        builder.data(new ChainBuffer(data));
        builder.dst(udp.getSource());
        builder.src(udp.getDestination());

        Ipv4Packet.Builder ipv4 = new Ipv4Packet.Builder();
        ipv4.id(generateIpPacketIdentify());
        ipv4.dst(udp.getSource().getAddress());
        ipv4.src(udp.getDestination().getAddress());
        ipv4.proto(InternetProtocol.UDP);
        ipv4.data(builder);

        dispatchIpv4(ipv4.build(), true);
    }
}
