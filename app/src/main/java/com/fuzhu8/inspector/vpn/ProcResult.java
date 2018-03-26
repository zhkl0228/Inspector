package com.fuzhu8.inspector.vpn;

import org.krakenapps.pcap.decoder.tcp.TcpDirection;

/**
 * /proc/net/... result
 * Created by zhkl0228 on 2017/1/18.
 */

public class ProcResult {

    private final TcpDirection direction;
    private final int uid;

    public ProcResult(TcpDirection direction, int uid) {
        super();

        this.direction = direction;
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    public TcpDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "ProcResult{" +
                "direction=" + direction +
                ", uid=" + uid +
                '}';
    }
}
