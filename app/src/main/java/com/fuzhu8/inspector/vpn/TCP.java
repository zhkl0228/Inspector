package com.fuzhu8.inspector.vpn;

import java.nio.ByteBuffer;

public class TCP {
    private static final String TAG = "TCP";

    private final int sourcePort;
    private final int destinationPort;
    private final long seq;
    private final long ack;
    private final byte dataOffset;
    private final boolean NS;
    private final boolean CWR;
    private final boolean ECE;
    private final boolean URG;
    private final boolean ACK;
    private final boolean PSH;
    private final boolean RST;
    private final boolean SYN;
    private final boolean FIN;
    private final int windowSize;
    private final int checksum;
    private final int urgentPointer;

    private ByteBuffer packet;

    public TCP(ByteBuffer packet) {
        this.packet = packet;

        sourcePort = get16Bits();
        destinationPort = get16Bits();
        seq = get32Bits();
        ack = get32Bits();
        short aShort = get8Bits();
        dataOffset = (byte) (((aShort & 0xF0) >>> 4) * 4);
        NS = (aShort & 0x01) == 1;
        aShort = get8Bits();
        CWR = ((aShort >> 7) & 1) == 1;
        ECE = ((aShort >> 6) & 1) == 1;
        URG = ((aShort >> 5) & 1) == 1;
        ACK = ((aShort >> 4) & 1) == 1;
        PSH = ((aShort >> 3) & 1) == 1;
        RST = ((aShort >> 2) & 1) == 1;
        SYN = ((aShort >> 1) & 1) == 1;
        FIN = (aShort & 1) == 1;
        windowSize = get16Bits();
        checksum = get16Bits();
        urgentPointer = get16Bits();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(sport: ").append(sourcePort).append(", dport: ");
        sb.append(destinationPort).append(" ");
        if (SYN) sb.append("S");
        if (ACK) sb.append("A");
        if (PSH) sb.append("P");
        if (FIN) sb.append("F");
        if (RST) sb.append("R");
        // int(signed) is 32bit, so extend it to long(64bit) for printing
        long unsignedFormat = seq & 0xFFFF;
        sb.append(", seq: ").append(unsignedFormat);
        unsignedFormat = ack & 0xFFFF;
        sb.append(", ack: ").append(unsignedFormat);
        sb.append(")");
        return sb.toString();
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    private short get8Bits() {
        return (short) (packet.get() & 0xFF);
    }

    private int get16Bits() {
        return packet.getShort() & 0xFFFF;
    }

    private long get32Bits() {
        return packet.getInt() & 0xFFFFFFFFL;
    }
}
