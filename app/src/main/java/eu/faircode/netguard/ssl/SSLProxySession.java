package eu.faircode.netguard.ssl;

import org.krakenapps.pcap.Protocol;
import org.krakenapps.pcap.decoder.tcp.TcpSession;
import org.krakenapps.pcap.decoder.tcp.TcpSessionImpl;
import org.krakenapps.pcap.decoder.tcp.TcpSessionKey;
import org.krakenapps.pcap.decoder.tcp.TcpState;

public class SSLProxySession extends TcpSessionImpl implements TcpSession {

    public SSLProxySession(TcpSessionKey key) {
        super(null);

        this.setKey(key);
    }

    @Override
    public Protocol getProtocol() {
        return Protocol.SSL;
    }

    @Override
    public TcpState getClientState() {
        return TcpState.ESTABLISHED;
    }

    @Override
    public TcpState getServerState() {
        return TcpState.ESTABLISHED;
    }

}
