package com.fuzhu8.inspector.vpn;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.io.InputStreamCache;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.tcpcap.PcapDLT;
import com.fuzhu8.tcpcap.handler.DefaultSessionHandler;
import com.fuzhu8.tcpcap.handler.SessionHandler;
import com.fuzhu8.tcpcap.sniffer.ExceptionHandler;
import com.fuzhu8.tcpcap.sniffer.KrakenPcapSniffer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.tcp.TcpProcessor;
import org.krakenapps.pcap.decoder.tcp.TcpSessionKey;
import org.krakenapps.pcap.decoder.tcp.TcpSessionKeyImpl;
import org.krakenapps.pcap.file.PcapFileOutputStream;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.ChainBuffer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import eu.faircode.netguard.ssl.SSLProxySession;

/**
 * inspector packet capture
 * Created by zhkl0228 on 2017/2/6.
 */

public class InspectorPacketCapture extends IPacketCapture.Stub implements FileFilter, ExceptionHandler {

    private final Inspector inspector;
    private final File dataDir;
    private final String processName;

    private File pcapFile;
    private PcapFileOutputStream pcapFileOutputStream;

    public InspectorPacketCapture(Inspector inspector, File dataDir, String processName) {
        super();

        this.inspector = inspector;
        this.dataDir = dataDir;
        this.processName = processName;

        try {
            flushFile(PcapDLT.CONST_RAW_IP);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String tcpRedirectRules;

    public void setTcpRedirectRules(String tcpRedirectRules) {
        this.tcpRedirectRules = tcpRedirectRules;

        this.tcpRedirectRulesUpdated = true;
    }

    @Override
    public String getTcpRedirectRules() {
        this.tcpRedirectRulesUpdated = false;

        return tcpRedirectRules;
    }

    @Override
    public boolean accept(File pathname) {
        return "pcap".equalsIgnoreCase(FilenameUtils.getExtension(pathname.getName())) && pathname.getName().startsWith(processName);
    }

    private int lastDatalink;

    private synchronized void flushFile(int datalink) throws IOException {
        lastDatalink = datalink;

        File[] pcaps = dataDir.listFiles(this);
        if (pcaps != null) {
            for (File pcap : pcaps) {
                if (!pcap.delete()) {
                    pcap.deleteOnExit();
                }
            }
        }

        pcapFile = File.createTempFile(processName, ".pcap", dataDir);
        FileUtils.deleteQuietly(pcapFile);
        pcapFileOutputStream = new PcapFileOutputStream(pcapFile, datalink);
    }

    private PcapPacket createPcapPacket(final byte[] packet, int datalink) {
        long currentTimeMillis = System.currentTimeMillis();
        int tsSec = (int) (currentTimeMillis / 1000);
        int tsUsec = (int) (currentTimeMillis % 1000);
        PacketHeader header = new PacketHeader(tsSec, tsUsec, packet.length, packet.length);
        return new PcapPacket(header, new ChainBuffer(packet)).setDatalink(datalink);
    }

    private boolean tcpRedirectRulesUpdated = true;

    @Override
    public synchronized boolean onPacket(final byte[] packetData, String type, int datalink) {
        if (lastDatalink != datalink) {
            try {
                flushFile(datalink);
            } catch (IOException e) {
                inspector.println(e);
            }
        }

        if (queues != null) {
            for (PcapSniffer sniffer : queues) {
                sniffer.pcap.put(createPcapPacket(packetData, datalink));
            }
        }

        try {
            if (inspector.isDebug()) {
                inspector.inspect(packetData, "onPacket type=" + type);
            }
            pcapFileOutputStream.write(createPcapPacket(packetData, datalink));
        } catch (IOException e) {
            inspector.printStackTrace(e);
        }
        return tcpRedirectRulesUpdated;
    }

    @Override
    public synchronized void onSSLProxyEstablish(String clientIp, String serverIp, int clientPort, int serverPort) {
        try {
            TcpSessionKey key = new TcpSessionKeyImpl(InetAddress.getByName(clientIp), InetAddress.getByName(serverIp), clientPort, serverPort);

            if (queues != null) {
                for (PcapSniffer sniffer : queues) {
                    sniffer.tcpProcessor.onEstablish(new SSLProxySession(key));
                }
            }

            if (inspector.isDebug()) {
                inspector.println("onSSLProxyEstablish key=" + key);
            }
        } catch (Exception e) {
            inspector.println(e);
        }
    }

    @Override
    public synchronized void onSSLProxyTX(String clientIp, String serverIp, int clientPort, int serverPort, byte[] data) {
        try {
            TcpSessionKey key = new TcpSessionKeyImpl(InetAddress.getByName(clientIp), InetAddress.getByName(serverIp), clientPort, serverPort);

            if (queues != null) {
                for (PcapSniffer sniffer : queues) {
                    sniffer.tcpProcessor.handleTx(key, new ChainBuffer(data));
                }
            }

            if (inspector.isDebug()) {
                inspector.inspect(data, "onSSLProxyTX key=" + key);
            }
        } catch (Exception e) {
            inspector.println(e);
        }
    }

    @Override
    public synchronized void onSSLProxyRX(String clientIp, String serverIp, int clientPort, int serverPort, byte[] data) {
        try {
            TcpSessionKey key = new TcpSessionKeyImpl(InetAddress.getByName(clientIp), InetAddress.getByName(serverIp), clientPort, serverPort);

            if (queues != null) {
                for (PcapSniffer sniffer : queues) {
                    sniffer.tcpProcessor.handleRx(key, new ChainBuffer(data));
                }
            }

            if (inspector.isDebug()) {
                inspector.inspect(data, "onSSLProxyRX key=" + key);
            }
        } catch (Exception e) {
            inspector.println(e);
        }
    }

    @Override
    public synchronized void onSSLProxyFinish(String clientIp, String serverIp, int clientPort, int serverPort) {
        try {
            TcpSessionKey key = new TcpSessionKeyImpl(InetAddress.getByName(clientIp), InetAddress.getByName(serverIp), clientPort, serverPort);

            if (queues != null) {
                for (PcapSniffer sniffer : queues) {
                    sniffer.tcpProcessor.onFinish(key);
                }
            }

            if (inspector.isDebug()) {
                inspector.println("onSSLProxyFinish key=" + key);
            }
        } catch (Exception e) {
            inspector.println(e);
        }
    }

    public synchronized void flush() throws IOException {
        IOUtils.closeQuietly(pcapFileOutputStream);

        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(pcapFile.toPath());
            inspector.writeToConsole(new InputStreamCache(processName + ".pcap", inputStream, (int) pcapFile.length()));
        } finally {
            IOUtils.closeQuietly(inputStream);
            FileUtils.deleteQuietly(pcapFile);

            flushFile(PcapDLT.CONST_RAW_IP);
        }
    }

    private static class PcapSniffer {
        final InspectorPcapInputStream pcap;
        final TcpProcessor tcpProcessor;
        PcapSniffer(InspectorPcapInputStream pcap, TcpProcessor tcpProcessor) {
            this.pcap = pcap;
            this.tcpProcessor = tcpProcessor;
        }
    }

    private List<PcapSniffer> queues;

    public synchronized void checkSniffer(List<Plugin> plugins) {
        if (queues != null) {
            return;
        }

        if (plugins == null || plugins.isEmpty()) {
            queues = new ArrayList<>(1);
            InspectorPcapInputStream pcap = new InspectorPcapInputStream();
            KrakenPcapSniffer sniffer = new KrakenPcapSniffer(pcap, new DefaultSessionHandler(inspector), null);
            sniffer.setExceptionHandler(this);
            Thread thread = new Thread(sniffer, "Default_Sniffer");
            thread.start();
            queues.add(new PcapSniffer(pcap, sniffer.getHttpDecoder()));
            return;
        }

        queues = new ArrayList<>();
        for (Plugin plugin : plugins) {
            SessionHandler handler = plugin.createSessionHandler();
            if (handler != null) {
                InspectorPcapInputStream pcap = new InspectorPcapInputStream();
                KrakenPcapSniffer sniffer = new KrakenPcapSniffer(pcap, handler, null);
                sniffer.setExceptionHandler(this);
                Thread thread = new Thread(sniffer, plugin + "_Sniffer");
                thread.start();
                queues.add(new PcapSniffer(pcap, sniffer.getHttpDecoder()));
            }
        }
    }

    @Override
    public boolean handleException(Exception exception) {
        inspector.println(exception);
        return true;
    }
}
