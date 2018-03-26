package com.fuzhu8.inspector.vpn;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.fuzhu8.tcpcap.PcapDLT;

import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.ip.InternetProtocol;
import org.krakenapps.pcap.decoder.ip.IpDecoder;
import org.krakenapps.pcap.decoder.tcp.TcpDecoder;
import org.krakenapps.pcap.decoder.tcp.TcpDirection;
import org.krakenapps.pcap.decoder.tcp.TcpPortProtocolMapper;
import org.krakenapps.pcap.decoder.tcp.TcpProcessor;
import org.krakenapps.pcap.decoder.tcp.TcpSegment;
import org.krakenapps.pcap.decoder.tcp.TcpSegmentCallback;
import org.krakenapps.pcap.decoder.tcp.TcpSession;
import org.krakenapps.pcap.decoder.tcp.TcpSessionKey;
import org.krakenapps.pcap.decoder.udp.UdpDecoder;
import org.krakenapps.pcap.decoder.udp.UdpPacket;
import org.krakenapps.pcap.decoder.udp.UdpPortProtocolMapper;
import org.krakenapps.pcap.decoder.udp.UdpProcessor;
import org.krakenapps.pcap.file.PcapFileOutputStream;
import org.krakenapps.pcap.util.Buffer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * vpn worker
 * Created by zhkl0228 on 2017/1/18.
 */

public class VpnWorker implements Runnable, TcpProcessor, UdpProcessor, TcpSegmentCallback {

    private final IPacketCapture packetCapture;
    private final VpnService vpnService;
    private final VpnService.Builder builder;

    public VpnWorker(IPacketCapture packetCapture, VpnService vpnService, VpnService.Builder builder) {
        super();
        this.packetCapture = packetCapture;
        this.vpnService = vpnService;
        this.builder = builder;
    }

    private ProcStat tcp, udp;

    private ProcResult matchesTcp(PortPair pair) {
        ProcResult result = tcp == null ? null : tcp.matches(pair);
        if (result == null) {
            tcp = ProcParser.parseTcp();
            result = tcp.matches(pair);
        }
        return result;
    }

    private ProcResult matchesUdp(PortPair pair) {
        ProcResult result = udp == null ? null : udp.matches(pair);
        if (result == null) {
            udp = ProcParser.parseUdp();
            result = udp.matches(pair);
        }
        return result;
    }

    private Thread thread;

    void start() {
       canStop = false;

        thread = new Thread(this, getClass().getSimpleName());
        thread.start();
    }
    void stop() {
        canStop = true;

        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Log.w(getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    boolean isRunning() {
        return running;
    }

    private boolean running;
    private boolean canStop;

    private Selector selector;
    private VpnProcessor vpnProcessor;

    private static final int MTU = 0x8000;

    @Override
    public void run() {
        ParcelFileDescriptor vpn = null;
        PcapFileOutputStream pcapFile = null;
        try {
            IpDecoder ip = new IpDecoder();
            TcpDecoder tcp = new TcpDecoder(new TcpPortProtocolMapper(this));
            tcp.registerSegmentCallback(this);
            UdpDecoder udp = new UdpDecoder(new UdpPortProtocolMapper());
            udp.registerUdpProcessor(this);
            ip.register(InternetProtocol.TCP, tcp);
            ip.register(InternetProtocol.UDP, udp);

            running = true;
            selector = Selector.open();
            builder.setSession(InspectVpnService.INSPECTOR_NAME);
            builder.addAddress("10.8.0.1", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.setMtu(MTU);
            vpn = builder.establish();

            File file = new File(vpnService.getFilesDir(), "debug.pcap");
            file.delete();
            pcapFile = new PcapFileOutputStream(file, PcapDLT.CONST_RAW_IP);
            vpnProcessor = new VpnProcessor(ip, vpn.getFileDescriptor(), pcapFile, MTU, packetCapture);
            processVpn();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(pcapFile);
            IOUtils.closeQuietly(vpnProcessor);
            IOUtils.closeQuietly(vpn);
            IOUtils.closeQuietly(selector);
            running = false;
        }
    }

    private final Map<TcpSessionKey, TCPDispatcher> sessionMap = new HashMap<TcpSessionKey, TCPDispatcher>();

    @Override
    public void onReceive(TcpSession session, TcpSegment segment) {
        session.setAttribute(TcpSegment.class.getSimpleName(), segment);
    }

    @Override
    public void postProcess(TcpSession session, TcpSegment segment) {
        if (segment.isFin()) {
            PortPair pair = new PortPair(segment.getSourcePort(), segment.getDestinationPort());
            ProcResult result = matchesTcp(pair);

            Log.d(getClass().getSimpleName(), "postProcess sessionKey=" + session.getKey() + ", segment=" + segment + ", clientState=" + session.getClientState() + ", serverState=" + session.getServerState() + ", result=" + result);
            TCPDispatcher tcp = sessionMap.get(session.getKey());
            if (tcp != null && result != null && tcp.handleFin(segment, result.getDirection())) {
                sessionMap.remove(session.getKey());
            }
        }
    }

    @Override
    public void onEstablish(TcpSession session) {
        TcpSessionKey sessionKey = session.getKey();
        PortPair pair = new PortPair(sessionKey.getClientPort(), sessionKey.getServerPort());
        ProcResult result = matchesTcp(pair);
        if (result == null) {
            Log.w(getClass().getSimpleName(), "onEstablish sessionKey=" + sessionKey);
            return;
        }

        TcpSegment segment = session.getAttribute(TcpSegment.class.getSimpleName(), TcpSegment.class);

        if (result.getDirection() != TcpDirection.ToServer) {
            return;
        }

        SocketChannel channel = null;
        try {
            Log.d(getClass().getSimpleName(), "onEstablish sessionKey=" + sessionKey + ", result=" + result + ", segment=" + segment + ", sessionSize=" + sessionMap.size());

            channel = SocketChannel.open();
            channel.configureBlocking(false);
            vpnService.protect(channel.socket());
            channel.connect(new InetSocketAddress(sessionKey.getServerIp(), sessionKey.getServerPort()));
            TCPDispatcher tcp = new TCPDispatcher(vpnProcessor, session, segment);
            channel.register(selector, SelectionKey.OP_CONNECT, tcp);
            sessionMap.put(sessionKey, tcp);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "onEstablish key=" + sessionKey, e);
            IOUtils.closeQuietly(channel);
        }
    }

    @Override
    public void handleTx(TcpSessionKey session, Buffer data) {
        TCPDispatcher tcp = sessionMap.get(session);
        if (tcp == null) {
            Log.w(getClass().getSimpleName(), "handleTx session is null: key=" + session + ", data=" + data);
            return;
        }

        tcp.handleTx(data);
    }

    @Override
    public void handleRx(TcpSessionKey session, Buffer data) {
        Log.d(getClass().getSimpleName(), "handleRx key=" + session + ", dataSize=" + data.readableBytes());
    }

    @Override
    public void onReset(TcpSessionKey key) {
        Log.d(getClass().getSimpleName(), "onReset key=" + key);

        TCPDispatcher tcp = sessionMap.get(key);
        if (tcp != null) {
            tcp.onReset();
        }
        sessionMap.remove(key);
    }

    @Override
    public void onFinish(TcpSessionKey key) {
        Log.d(getClass().getSimpleName(), "onFinish key=" + key);

        sessionMap.remove(key);
    }

    @Override
    public void process(UdpPacket p) {
        Buffer buffer = p.getData();
        byte[] data = new byte[buffer.readableBytes()];
        buffer.gets(data);

        PortPair pair = new PortPair(p.getSourcePort(), p.getDestinationPort());
        ProcResult result = matchesUdp(pair);
        if (result == null) {
            Log.w(getClass().getSimpleName(), "process udp=" + p + ", length=" + data.length);
            return;
        }

        if (result.getDirection() != TcpDirection.ToServer) {
            return;
        }

        DatagramChannel channel = null;
        try {
            Log.d(getClass().getSimpleName(), "open udp channel pair=" + pair + ", keys=" + selector.keys().size());

            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            vpnService.protect(channel.socket());
            channel.connect(p.getDestination());
            channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, new UDPDispatcher(vpnProcessor, p, ByteBuffer.wrap(data)));
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "process udp", e);
            IOUtils.closeQuietly(channel);
        }
    }

    private void processVpn() throws IOException, RemoteException, InterruptedException, VpnException {
        while (!canStop) {
            if (vpnProcessor.processInput()) {
                continue;
            }

            int count = selector.select(10);
            if (count < 1) {
                long currentTimeMillis = System.currentTimeMillis();
                checkRegisteredKeys(currentTimeMillis);
                continue;
            }

            Set<SelectionKey> keys = selector.selectedKeys();
            for(Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext(); ) {
                SelectionKey key = iterator.next();
                SelectionKeyProcessor processor = (SelectionKeyProcessor) key.attachment();

                try {
                    if (!key.isValid()) {
                        key.cancel();
                        continue;
                    }

                    processKey(key, processor);
                } catch(IOException e) {
                    Log.d(getClass().getSimpleName(), e.getMessage(), e);
                    processor.exceptionRaised(key, e);
                } finally {
                    iterator.remove();
                }
            }
        }
    }

    private void checkRegisteredKeys(long currentTimeMillis) throws IOException, VpnException {
        for (SelectionKey key : selector.keys()) {
            SelectionKeyProcessor processor = (SelectionKeyProcessor) key.attachment();
            processor.checkRegisteredKey(key, currentTimeMillis);
        }
    }

    private void processKey(SelectionKey key, SelectionKeyProcessor processor) throws IOException, VpnException {
        if (key.isWritable() && processor.processWritableKey(key)) {
            return;
        }

        if (key.isReadable() && processor.processReadableKey(key)) {
            return;
        }

        if (key.isConnectable() && processor.processConnectableKey(key)) {
            return;
        }

        if (key.isAcceptable() && processor.processAcceptableKey(key)) {
            return;
        }
    }

}
