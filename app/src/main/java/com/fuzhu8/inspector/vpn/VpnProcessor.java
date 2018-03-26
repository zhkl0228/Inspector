package com.fuzhu8.inspector.vpn;

import android.os.RemoteException;
import android.util.Log;

import com.fuzhu8.tcpcap.PcapDLT;

import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.ethernet.EthernetFrame;
import org.krakenapps.pcap.decoder.ip.IpDecoder;
import org.krakenapps.pcap.file.PcapFileOutputStream;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;
import org.krakenapps.pcap.util.ChainBuffer;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * process vpn
 * Created by zhkl0228 on 2017/1/20.
 */

public class VpnProcessor implements Closeable {

    private final IpDecoder ip;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final PcapFileOutputStream pcapFile;
    private final byte[] buf;

    private final IPacketCapture packetCapture;

    public VpnProcessor(IpDecoder ip, FileDescriptor fd, PcapFileOutputStream pcapFile, int bufferSize, IPacketCapture packetCapture) {
        super();
        this.ip = ip;

        inputStream = new FileInputStream(fd);
        outputStream = new FileOutputStream(fd);
        this.pcapFile = pcapFile;
        this.buf = new byte[bufferSize];
        this.packetCapture = packetCapture;
    }

    ByteBuffer sharedBuffer() {
        return ByteBuffer.wrap(buf);
    }

    private PcapPacket createPcapPacket(byte[] packet) {
        long currentTimeMillis = System.currentTimeMillis();
        int tsSec = (int) (currentTimeMillis / 1000);
        int tsUsec = (int) (currentTimeMillis % 1000);
        PacketHeader header = new PacketHeader(tsSec, tsUsec, packet.length, packet.length);
        return new PcapPacket(header, new ChainBuffer(packet));
    }

    boolean processInput() throws VpnException {
        try {
            int read = inputStream.read(buf);
            if (read == -1) {
                throw new VpnException("EOF");
            }

            if (read < 1) {
                return false;
            }

            byte[] copy = Arrays.copyOfRange(buf, 0, read);
            // Log.d(getClass().getSimpleName(), Inspector.inspectString(copy, "processInput"));
            Buffer data = new ChainBuffer(copy);
            data.mark();
            byte b1 = data.get();
            byte version = (byte) ((b1 & 0xF0) >> 4);
            if (version != 4) {
                Log.e(getClass().getSimpleName(), "Unsupported raw ip version: " + version);
                return true;
            }
            data.reset();

            if (pcapFile != null) {
                pcapFile.write(createPcapPacket(copy));
            }
            ip.process(new EthernetFrame(null, data));
            if (packetCapture != null) {
                packetCapture.onPacket(copy, "processInput", PcapDLT.CONST_RAW_IP);
            }
            return true;
        } catch (IOException e) {
            throw new VpnException(e);
        } catch (RemoteException e) {
            Log.d(getClass().getSimpleName(), e.getMessage(), e);
            return true;
        }
    }

    void dispatch(byte[] packet, boolean forward) throws VpnException {
        try {
            if (forward) {
                outputStream.write(packet);
            }
            if (pcapFile != null) {
                pcapFile.write(createPcapPacket(packet));
            }
            ip.process(new EthernetFrame(null, new ChainBuffer(packet)));

            if (packetCapture != null) {
                packetCapture.onPacket(packet, "dispatch", PcapDLT.CONST_RAW_IP);
            }
        } catch (IOException e) {
            throw new VpnException(e);
        } catch (RemoteException e) {
            Log.d(getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
    }
}
