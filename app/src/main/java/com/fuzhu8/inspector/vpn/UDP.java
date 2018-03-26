package com.fuzhu8.inspector.vpn;

import java.nio.ByteBuffer;

public class UDP {
    private static final String TAG = "UDP";

    private final int sourcePort;
    private final int destinationPort;
    private final int length;
    private final int checksum;

    private DNS dns;
    private final ByteBuffer packet;

    public UDP(ByteBuffer packet) {
        this.packet = packet;

        sourcePort = get16Bits();
        destinationPort = get16Bits();
        length = get16Bits();
        checksum = get16Bits();

        if (isDNS()) {
            dns = new DNS(packet);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(sport: ").append(sourcePort);
        sb.append(", dport: ").append(destinationPort).append(")");
        if (sourcePort == 53 || destinationPort == 53)
            sb.append(dns.toString());

        return sb.toString();
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    private int get16Bits() {
        return packet.getShort() & 0xFFFF;
    }

    boolean isDNS() {
        return sourcePort == 53 || destinationPort == 53;
    }

    public DNS getDns() {
        return dns;
    }
}
