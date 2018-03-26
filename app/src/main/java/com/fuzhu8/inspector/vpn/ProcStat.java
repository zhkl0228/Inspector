package com.fuzhu8.inspector.vpn;

import org.krakenapps.pcap.decoder.tcp.TcpSessionKey;

/**
 * /proc/net/... stat
 * Created by zhkl0228 on 2017/1/18.
 */

public interface ProcStat {

    ProcResult matches(PortPair pair);

}
