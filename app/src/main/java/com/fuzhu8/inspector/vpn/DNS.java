package com.fuzhu8.inspector.vpn;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DNS {
    private static final String TAG = "DNS";

    private int transactionID;
    private boolean response;
    private short opCode;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean nonAuthenticatedData;
    private int noOfQuestion;
    private int noOfAnswer;
    private int noOfAuthority;
    private int noOfAdditional;
    private List<Question> queries;
    private List<Answer> answers;

    private ByteBuffer packet;

    /**
     * Get the value in ByteBuffer deciding whether its position is moved or not
     */
    private boolean indexMode;
    private int indexPacket;

    class Question {
        private String hostname;
        private int type;
        private int queryClass;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getQueryClass() {
            return queryClass;
        }

        public void setQueryClass(int queryClass) {
            this.queryClass = queryClass;
        }
    }

    class Answer extends Question {
        private long ttl;
        private int length;
        private InetAddress address;
        private String canonicalName;

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public InetAddress getAddress() {
            return address;
        }

        public void setAddress(InetAddress address) {
            this.address = address;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public void setCanonicalName(String canonicalName) {
            this.canonicalName = canonicalName;
        }
    }

    public DNS(ByteBuffer packet) {
        this.packet = packet;

        transactionID = get16Bits();

        int flags = get16Bits();
        response = ((flags >> 15) & 1) == 1;
        opCode = (short) ((flags & 0x7800) >>> 11);
        truncated = ((flags >> 9) & 1) == 1;
        recursionDesired = ((flags >> 8) & 1) == 1;
        nonAuthenticatedData = ((flags >> 4) & 1) == 1;

        noOfQuestion = get16Bits();
        noOfAnswer = get16Bits();
        noOfAuthority = get16Bits();
        noOfAdditional = get16Bits();

        queries = new ArrayList<Question>();

        for (int i = 0; i < noOfQuestion; i++) {
            Question temp = new Question();
            temp.hostname = getHostname();
            temp.type = get16Bits();
            temp.queryClass = get16Bits();
            queries.add(temp);
        }

        answers = new ArrayList<Answer>();

        if (noOfAnswer > 0) {
            for (int i = 0; i < noOfAnswer; i++) {
                Answer temp = new Answer();
                temp.setHostname(getHostname());
                temp.setType(get16Bits());
                temp.setQueryClass(get16Bits());
                temp.setTtl(get32Bits());
                temp.setLength(get16Bits());
                final int type = temp.getType();
                switch (type) {
                    case 1: // Host Address
                        try {
                            temp.address = getIPAddress();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, e.toString());
                        }
                        break;
                    case 5: // CNAME
                        temp.canonicalName = getHostname();
                        break;
                    case 28: // IPv6 Address
                        try {
                            temp.address = getIPv6Address();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, e.toString());
                        }
                }
                answers.add(temp);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DNS{");

        for (Question query : queries) {
            final int type = query.getType();
            sb.append(query.hostname + "(Type: " + type + ")");
        }
        sb.append(" ");

        for (Answer answer : answers) {
            final int type = answer.getType();
            if (type == 1 || type == 28) {
                sb.append(answer.address.getHostAddress() + " ");
            }
            else {
                sb.append("Type: " + type + ", ");
            }
        }
        sb.append("}");

        return sb.toString();
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

    private String getHostname() {
        short c;
        int len = -1;

        indexMode = false;
        StringBuilder sb = new StringBuilder();

        do {
            c = get8BitsStream();
            if (len == -1 || len == 0) {
                if (c == 0) break;
                else {
                    if (isPointer(c)) {
                        short ch = get8BitsStream();
                        int pointer = ((c & 0x3F) << 8) + ch;
                        indexPacket = 20 + 8 + pointer;
                        if (!indexMode) indexMode = true;
                    } else {
                        if (len == 0) sb.append(".");
                        len = c;
                    }
                }
            } else {
                sb.append((char)c);
                len--;
            }
        } while (true);

        return sb.toString();
    }

    private short get8BitsStream() {
        short ch;

        if (indexMode) {
            ch = packet.get(indexPacket);
            indexPacket++;
        } else {
            ch = get8Bits();
        }

        return ch;
    }

    private boolean isPointer(short value) {
        return (((value & 0xFF) >> 6) & 3) == 3;
    }

    private InetAddress getIPAddress() throws UnknownHostException {
        byte[] addr = new byte[4];
        for (int i = 0; i < addr.length; i++)
            addr[i] = (byte) (packet.get() & 0xFF);

        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new UnknownHostException("IP address is not valid.");
        }
    }

    private InetAddress getIPv6Address() throws UnknownHostException {
        byte[] addr = new byte[16];
        for (int i = 0; i < addr.length; i++)
            addr[i] = (byte) (packet.get() & 0xFF);

        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new UnknownHostException("IP address is not valid.");
        }
    }

}
