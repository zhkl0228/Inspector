package com.fuzhu8.inspector.vpn;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.krakenapps.pcap.decoder.tcp.TcpDirection;
import org.krakenapps.pcap.decoder.tcp.TcpSegment;
import org.krakenapps.pcap.decoder.tcp.TcpSession;
import org.krakenapps.pcap.decoder.tcp.TcpSessionKey;
import org.krakenapps.pcap.util.Buffer;
import org.krakenapps.pcap.util.ChainBuffer;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

/**
 * tcp dispatcher
 * Created by zhkl0228 on 2017/1/20.
 */

public class TCPDispatcher extends AbstractSelectionKeyProcessor implements SelectionKeyProcessor {

    private static final int BUFFER_SIZE = 0xffff;

    private final TcpSessionKey sessionKey;
    private final ByteBuffer writeBuffer;
    private final ByteBuffer readBuffer;

    private int clientSeq, serverSeq;

    public TCPDispatcher(VpnProcessor vpnProcessor, TcpSession session, TcpSegment sync) {
        super(vpnProcessor);

        this.sessionKey = session.getKey();

        this.clientSeq = generateIpPacketIdentify();
        this.serverSeq = sync.getSeq();

        this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer.position(BUFFER_SIZE);

        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.readBuffer.position(BUFFER_SIZE);
    }

    private TcpPacketBuilder buildTcp() {
        return buildTcp(sessionKey.getServerAddress(), sessionKey.getClientAddress());
    }

    private TcpPacketBuilder buildTcp(InetSocketAddress src, InetSocketAddress dest) {
        return new TcpPacketBuilder(src, dest, this);
    }

    @Override
    public boolean processWritableKey(SelectionKey key) throws IOException, VpnException {
        Log.d(getClass().getSimpleName(), "processWritableKey sessionKey=" + sessionKey);

        SocketChannel channel = (SocketChannel) key.channel();
        int write = channel.write(writeBuffer);
        if (write == -1) {
            throw new EOFException("processWritableKey sessionKey=" + sessionKey);
        }
        serverSeq += write;

        if (writeBuffer.hasRemaining()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

            TcpPacketBuilder builder = buildTcp();
            builder.seq(clientSeq);
            builder.ack(serverSeq);
            builder.ack();
            builder.dispatch(true);
        }
        return false;
    }

    @Override
    public boolean processReadableKey(SelectionKey key) throws IOException, VpnException {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            readBuffer.compact();

            if (!readBuffer.hasRemaining()) {
                throw new BufferUnderflowException();
            }

            int read = channel.read(readBuffer);
            if (read == -1) {
                throw new EOFException("processReadableKey sessionKey=" + sessionKey);
            }
        } finally {
            readBuffer.flip();
        }

        Log.d(getClass().getSimpleName(), "processReadableKey bufferSize=" + readBuffer.remaining());
        dispatchReadBuffer();

        return false;
    }

    private void dispatchReadBuffer() throws VpnException {
        int window = 0x7FFF;
        while (window >= 0x2000 && readBuffer.hasRemaining()) {
            int mtu = readBuffer.remaining() >= 0x2000 ? 0x2000 : readBuffer.remaining();
            byte[] data = new byte[mtu];
            readBuffer.get(data);

            TcpPacketBuilder builder = buildTcp();
            builder.seq(clientSeq);
            builder.ack(serverSeq);
            builder.ack();
            if (!readBuffer.hasRemaining()) {
                builder.psh();
            }
            builder.data(new ChainBuffer(data));
            builder.dispatch(true);

            clientSeq += data.length;
            window -= data.length;
        }
    }

    @Override
    public boolean processConnectableKey(SelectionKey key) throws IOException, VpnException {
        SocketChannel channel = (SocketChannel) key.channel();
        Log.d(getClass().getSimpleName(), "processConnectableKey clientSeq=" + clientSeq + ", serverSeq=" + serverSeq + ", sessionKey=" + sessionKey);

        if (channel.finishConnect()) {
            TcpPacketBuilder builder = buildTcp();
            builder.seq(clientSeq++);
            builder.ack(++serverSeq);
            builder.syn().ack();
            builder.dispatch(true);

            key.interestOps(SelectionKey.OP_READ);
            return false;
        } else {
            throw new IOException("Connect failed: sessionKey=" + sessionKey);
        }
    }

    void handleTx(Buffer buffer) {
        Log.d(getClass().getSimpleName(), "handleTx sessionKey=" + sessionKey + ", readableBytes=" + buffer.readableBytes());

        byte[] data = new byte[buffer.readableBytes()];
        buffer.gets(data);

        writeBuffer.compact();
        writeBuffer.put(data);
        writeBuffer.flip();
    }

    private class Fin implements Comparable<Fin> {
        final TcpSegment fin;
        final boolean forward;
        final long time;
        public Fin(TcpSegment fin, boolean forward) {
            this.fin = fin;
            this.forward = forward;
            time = System.nanoTime();
        }
        @Override
        public int compareTo(Fin another) {
            return (int) (time - another.time);
        }
        void ack() throws VpnException {
            TcpPacketBuilder builder = buildTcp(fin.getDestination(), fin.getSource());
            builder.seq(fin.getAck());
            builder.ack(fin.getSeq() + 1);
            builder.ack();
            builder.dispatch(forward);
        }
    }

    private Fin toServerFin, toClientFin;

    boolean handleFin(TcpSegment segment, TcpDirection direction) {
        if (direction == TcpDirection.ToClient) {
            toClientFin = new Fin(segment, false);
        } else {
            toServerFin = new Fin(segment, true);
        }

        try {
            if (toServerFin != null && toClientFin != null) {
                Fin[] fins = new Fin[]{toServerFin, toClientFin};
                Arrays.sort(fins);
                for (Fin fin : fins) {
                    fin.ack();
                }
                return true;
            }
        } catch (VpnException e) {
            Log.w(getClass().getSimpleName(), "handleFin", e);
        }
        return false;
    }

    @Override
    public void checkRegisteredKey(SelectionKey key, long currentTimeMillis) throws IOException, VpnException {
        if (reset) {
            exceptionRaised(key, new IOException("Reset"));
            return;
        }

        if (readBuffer.hasRemaining()) {
            dispatchReadBuffer();
        }

        if (writeBuffer.hasRemaining()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            return;
        }

        if (toServerFin != null) {
            exceptionRaised(key, new EOFException("FIN"));
            return;
        }
    }

    private boolean reset;

    void onReset() {
        reset = true;
    }

    @Override
    public void exceptionRaised(SelectionKey key, IOException exception) throws IOException, VpnException {
        key.cancel();
        IOUtils.closeQuietly(key.channel());

        if (EOFException.class.isInstance(exception)) { // EOF
            Log.d(getClass().getSimpleName(), "exceptionRaised EOF sessionKey=" + sessionKey);

            if (toClientFin == null) {
                TcpPacketBuilder builder = buildTcp();
                builder.seq(clientSeq);
                builder.ack(serverSeq);
                builder.fin().ack();
                builder.dispatch(true);
            }
        } else {
            Log.d(getClass().getSimpleName(), "exceptionRaised sessionKey=" + sessionKey, exception);

            TcpPacketBuilder builder = buildTcp();
            builder.seq(clientSeq);
            builder.ack(serverSeq);
            builder.rst().ack();
            builder.dispatch(true);
        }
    }

}
