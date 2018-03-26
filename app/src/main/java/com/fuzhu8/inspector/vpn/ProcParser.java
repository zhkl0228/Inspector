package com.fuzhu8.inspector.vpn;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.tcp.TcpDirection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * /proc/net/tcp or udp parser
 * Created by zhkl0228 on 2017/1/18.
 */

public class ProcParser {

    /**
     * parse /proc/net/tcp
     * @return proc stat
     */
    public static ProcStat parseTcp() {
        return new ProcStatGroup(parse(new File("/proc/net/tcp")), parse(new File("/proc/net/tcp6")));
    }

    /**
     * parse /proc/net/udp
     * @return proc stat
     */
    public static ProcStat parseUdp() {
        return new ProcStatGroup(parse(new File("/proc/net/udp")), parse(new File("/proc/net/udp6")));
    }

    private static class MyProcStat implements ProcStat {
        private final Map<PortPair, ProcResult> map = new HashMap<PortPair, ProcResult>();
        @Override
        public ProcResult matches(PortPair pair) {
            return map.get(pair);
        }
        private void addEntry(InetAddress local, int localPort, InetAddress remote, int remotePort, int uid) {
            PortPair toServer = new PortPair(localPort, remotePort);
            PortPair toClient = new PortPair(remotePort, localPort);
            map.put(toServer, new ProcResult(TcpDirection.ToServer, uid));
            map.put(toClient, new ProcResult(TcpDirection.ToClient, uid));
            // Log.d(ProcParser.class.getSimpleName(), "addEntry toServer=" + toServer + ", toClient=" + toClient + ", uid=" + uid);
        }
    }

    private static class ProcStatGroup implements ProcStat {
        private final ProcStat[] stats;
        public ProcStatGroup(ProcStat...stats) {
            super();
            this.stats = stats;
        }
        @Override
        public ProcResult matches(PortPair pair) {
            for (ProcStat stat : stats) {
                ProcResult result = stat.matches(pair);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }

    private static ProcStat parse(File statFile) {
        Reader reader = null;
        BufferedReader bufferedReader = null;
        try {
            reader = new FileReader(statFile);
            bufferedReader = new BufferedReader(reader);

            MyProcStat procStat = null;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (procStat == null) {
                    String[] headers = line.trim().split("\\s+");

                    if (headers.length <= 9 ||
                            !"local_address".equals(headers[1]) ||
                            (!"rem_address".equals(headers[2]) && !"remote_address".equals(headers[2])) ||
                            "!uid".equals(headers[9])) {
                        throw new IllegalStateException("stat header failed: " + line + " for file " + statFile);
                    }
                    procStat = new MyProcStat();
                    continue;
                }

                String[] tokens = line.trim().split("\\s+");
                String local = tokens[1];
                String remote = tokens[2];
                int uid = Integer.parseInt(tokens[7]);
                InetSocketAddress localSocket = parseInetSocketAddress(local);
                InetSocketAddress remoteSocket = parseInetSocketAddress(remote);
                // Log.d(ProcParser.class.getSimpleName(), "parse line=" + line + ", local=" + local + ", remote=" + remote + ", uid=" + uid);
                procStat.addEntry(localSocket.getAddress(), localSocket.getPort(), remoteSocket.getAddress(), remoteSocket.getPort(), uid);
            }
            return procStat;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
            IOUtils.closeQuietly(reader);
        }
    }

    private static InetSocketAddress parseInetSocketAddress(String socket) {
        int index = socket.indexOf(':');
        if (index == -1) {
            throw new IllegalStateException("parse socket failed: " + socket);
        }

        try {
            String addrStr = socket.substring(0, index);
            byte[] ipAddress = Hex.decodeHex(addrStr.toCharArray());
            if (ipAddress.length != 4 && ipAddress.length != 16) {
                throw new IllegalStateException("only support ipv4 and ipv6 address: " + socket);
            }

            int port = Integer.parseInt(socket.substring(index + 1), 16);

            // reverse array
            for(int i = 0; i < ipAddress.length / 2; i++) {
                byte temp = ipAddress[i];
                ipAddress[i] = ipAddress[ipAddress.length - i - 1];
                ipAddress[ipAddress.length - i - 1] = temp;
            }
            return new InetSocketAddress(InetAddress.getByAddress(ipAddress), port);
        } catch (UnknownHostException | DecoderException e) {
            throw new IllegalStateException(e);
        }
    }

}
