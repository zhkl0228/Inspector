package com.fuzhu8.inspector.vpn;

import org.krakenapps.pcap.decoder.ip.InternetProtocol;
import org.krakenapps.pcap.decoder.ip.Ipv4Packet;
import org.krakenapps.pcap.decoder.tcp.TcpPacket;
import org.krakenapps.pcap.util.Buffer;

import java.net.InetSocketAddress;

/**
 * build tcp packet and dispatch
 * Created by zhkl0228 on 2017/1/21.
 */

public class TcpPacketBuilder {

    private final Ipv4Packet.Builder ipv4;
    private final TcpPacket.Builder builder;
    private final AbstractSelectionKeyProcessor selectionKeyProcessor;

    public TcpPacketBuilder(InetSocketAddress src, InetSocketAddress dest, AbstractSelectionKeyProcessor selectionKeyProcessor) {
        super();

        this.selectionKeyProcessor = selectionKeyProcessor;

        TcpPacket.Builder builder = new TcpPacket.Builder();
        builder.src(src);
        builder.dst(dest);
        builder.window(0x7fff);
        this.builder = builder;

        Ipv4Packet.Builder ipv4 = new Ipv4Packet.Builder();
        ipv4.id(selectionKeyProcessor.generateIpPacketIdentify());
        ipv4.dst(dest.getAddress());
        ipv4.src(src.getAddress());
        ipv4.proto(InternetProtocol.TCP);
        this.ipv4 = ipv4;
    }

    void dispatch(boolean forward) throws VpnException {
        ipv4.data(builder);
        selectionKeyProcessor.dispatchIpv4(ipv4.build(), forward);
    }

    public TcpPacketBuilder data(Buffer data) {
        builder.data(data);
        return this;
    }

    public TcpPacketBuilder ack(int ack) {
        builder.ack(ack);
        return this;
    }

    public TcpPacketBuilder seq(int seq) {
        builder.seq(seq);
        return this;
    }

    public TcpPacketBuilder psh() {
        builder.psh();
        return this;
    }

    public TcpPacketBuilder urg() {
        builder.urg();
        return this;
    }

    public TcpPacketBuilder rst() {
        builder.rst();
        return this;
    }

    public TcpPacketBuilder fin() {
        builder.fin();
        return this;
    }

    public TcpPacketBuilder ack() {
        builder.ack();
        return this;
    }

    public TcpPacketBuilder syn() {
        builder.syn();
        return this;
    }
}
