package com.fuzhu8.inspector.vpn;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv4 {
    private static final String TAG = "IPv4";
    public static final int TCP = 6;
    public static final int UDP = 17;

    private final Byte version;
    private final Byte iHL;
    private final Byte dscp;
    private final Byte ecn;
    private final Integer totalLength;
    private final Integer id;
    private final Boolean reserved;
    private final Boolean df;
    private final Boolean mf;
    private final Integer fragmentOffset;
    private final Short ttl;
    private final Short protocol;
    private final Integer headerChecksum;
    private InetAddress source;
    private InetAddress destination;
    private TCP tcp;
    private UDP udp;
    private final ByteBuffer packet;

    public IPv4(ByteBuffer packet) {
        this.packet = packet;
        short aShort = (short) (packet.get() & 0xFF);
        version = (byte) ((aShort >> 4) & 0xF);
        // iHL unit is number of 32bit words, so multiplied by 4
        iHL = (byte) ((aShort & 0x0F) * 4);
        aShort = (short) (packet.get() & 0xFF);
        dscp = (byte) ((aShort >> 2) & 0x3F);
        ecn = (byte) (aShort & 0x03);
        totalLength = get16Bits();

        id = get16Bits();
        int aInteger = get16Bits();
        reserved = ((aInteger >> 15) & 1) == 1;
        df = ((aInteger >> 14) & 1) == 1;
        mf = ((aInteger >> 13) & 1) == 1;
        // fragment offset unit is 8 byte blocks
        fragmentOffset = (aInteger & 0x1FFF) * 8;

        ttl = get8Bits();
        protocol = get8Bits();
        headerChecksum = get16Bits();

        try {
            source = getIPAddress();
            destination = getIPAddress();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.toString());
        }

        if (protocol == TCP) {
            tcp = new TCP(packet);
        } else if (protocol == UDP) {
            udp = new UDP(packet);
        }
    }

    public String toString() {

        StringBuilder string = new StringBuilder("S: " + source.toString());

        string.append(", D: ").append(destination.toString());

        string.append(", ");
        if (protocol == 6) string.append("TCP");
        else if (protocol == 17) string.append("UDP");
        else string.append("Unknown(").append(protocol).append(")");

        string.append(", hdr: ").append(iHL);

        if (protocol == 6) string.append(tcp.toString());
        else if (protocol == 17) string.append(udp.toString());

        return string.toString();
    }

    private short get8Bits() {
        return (short) (packet.get() & 0xFF);
    }

    private int get16Bits() {
        return packet.getShort() & 0xFFFF;
    }

    private InetAddress getIPAddress() throws UnknownHostException {
        byte[] fourBytes = new byte[4];
        for (int i = 0; i < fourBytes.length; i++)
            fourBytes[i] = (byte) (packet.get() & 0xFF);

        try {
            return InetAddress.getByAddress(fourBytes);
        } catch (UnknownHostException e) {
            throw new UnknownHostException("IP address is not valid.");
        }
    }

    public InetAddress getDestination() {
        return destination;
    }

    public Short getProtocol() {
        return protocol;
    }

    public TCP getTcp() {
        return tcp;
    }

    public UDP getUdp() {
        return udp;
    }

    public ByteBuffer getPacket() {
        return packet;
    }
}
