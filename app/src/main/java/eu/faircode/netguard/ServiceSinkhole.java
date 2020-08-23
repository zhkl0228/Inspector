package eu.faircode.netguard;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.content.InspectorBroadcastListener;
import com.fuzhu8.inspector.content.InspectorBroadcastReceiver;
import com.fuzhu8.inspector.vpn.IPacketCapture;
import com.fuzhu8.inspector.vpn.InspectVpnService;
import com.fuzhu8.inspector.vpn.InspectorVpn;
import com.fuzhu8.tcpcap.PcapDLT;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import cn.banny.utils.StringUtils;

/**
 * netguard vpn service
 * Created by zhkl0228 on 2017/3/21.
 */

public class ServiceSinkhole extends VpnService implements InspectorBroadcastListener, InspectorVpn {

    private static final int MSG_SERVICE_INTENT = 0;

    public enum Command {run, start, reload, stop, stats, set, householding, watchdog, packet}

    private final class CommandHandler extends Handler {
        CommandHandler(Looper looper) {
            super(looper);
        }

        public void queue(Intent intent) {
            Message msg = commandHandler.obtainMessage();
            msg.obj = intent;
            msg.what = MSG_SERVICE_INTENT;
            commandHandler.sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == MSG_SERVICE_INTENT) {
                    handleIntent((Intent) msg.obj);
                } else {
                    Log.e(TAG, "Unknown command message=" + msg.what);
                }
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }
        }

        private void handleIntent(Intent intent) {
            Command cmd = (Command) intent.getSerializableExtra(EXTRA_COMMAND);
            String reason = intent.getStringExtra(EXTRA_REASON);
            if (cmd != Command.packet) {
                Log.i(TAG, "Executing intent=" + intent + " command=" + cmd + " reason=" + reason +
                        " vpn=" + (vpn != null) + " user=" + (Process.myUid() / 100000));
            }

            try {
                switch (cmd) {
                    case run:
                    case stats:
                        break;

                    case start:
                        start();
                        break;

                    case reload:
                        reload();
                        break;

                    case stop:
                        stop();
                        break;

                    case householding:
                        householding();
                        break;

                    case watchdog:
                        watchdog();
                        break;

                    case packet:
                        byte[] packetData;
                        if (packetCapture != null && (packetData = intent.getByteArrayExtra(EXTRA_PACKET)) != null &&
                                packetCapture.onPacket(packetData, "Netguard", PcapDLT.CONST_RAW_IP)) {
                            redirectRules = TcpRedirectRule.parseTcpRedirectRules(packetCapture.getTcpRedirectRules());
                        }
                        break;

                    default:
                        Log.e(TAG, "Unknown command=" + cmd);
                }

            } catch (DeadObjectException e) {
                disableNetwork();
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }
        }

        private void start() {
            if (vpn == null) {
                Builder builder = getBuilder();
                vpn = startVPN(builder);
                if (vpn == null)
                    throw new IllegalStateException("start vpn failed.");

                startNative(vpn);
            }
        }

        private void reload() {
            Builder builder = getBuilder();

            Log.i(TAG, "Legacy restart");

            if (vpn != null) {
                stopNative(false);
                stopVPN(vpn);
                vpn = null;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            vpn = startVPN(builder);

            if (vpn == null) {
                throw new IllegalStateException("start vpn failed.");
            }

            startNative(vpn);
        }

        private void stop() {
            if (vpn != null) {
                stopNative(true);
                stopVPN(vpn);
                vpn = null;
            }
        }

        private void householding() {
        }

        private void watchdog() {
            if (vpn == null) {
                Log.e(TAG, "Service was killed");
                start();
            }
        }
    }

    private class Builder extends VpnService.Builder {
        private NetworkInfo networkInfo;
        private int mtu;
        private List<String> listAddress = new ArrayList<>();
        private List<String> listRoute = new ArrayList<>();
        private List<InetAddress> listDns = new ArrayList<>();

        private Builder() {
            super();
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = cm == null ? null : cm.getActiveNetworkInfo();
        }

        @Override
        public VpnService.Builder setMtu(int mtu) {
            this.mtu = mtu;
            super.setMtu(mtu);
            return this;
        }

        @Override
        public Builder addAddress(String address, int prefixLength) {
            listAddress.add(address + "/" + prefixLength);
            super.addAddress(address, prefixLength);
            return this;
        }

        @Override
        public Builder addRoute(String address, int prefixLength) {
            listRoute.add(address + "/" + prefixLength);
            super.addRoute(address, prefixLength);
            return this;
        }

        @Override
        public Builder addDnsServer(InetAddress address) {
            listDns.add(address);
            super.addDnsServer(address);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Builder)) {
                return false;
            }

            Builder other = (Builder) obj;

            if (this.networkInfo == null || other.networkInfo == null ||
                    this.networkInfo.getType() != other.networkInfo.getType())
                return false;

            if (this.mtu != other.mtu)
                return false;

            if (this.listAddress.size() != other.listAddress.size())
                return false;

            if (this.listRoute.size() != other.listRoute.size())
                return false;

            if (this.listDns.size() != other.listDns.size())
                return false;

            for (String address : this.listAddress)
                if (!other.listAddress.contains(address))
                    return false;

            for (String route : this.listRoute)
                if (!other.listRoute.contains(route))
                    return false;

            for (InetAddress dns : this.listDns)
                if (!other.listDns.contains(dns))
                    return false;

            return true;
        }
    }

    public static List<InetAddress> getDns(Context context) {
        List<InetAddress> listDns = new ArrayList<>();
        List<String> sysDns = Util.getDefaultDNS(context);

        // Get custom DNS servers
        Log.i(TAG, "DNS system=" + TextUtils.join(",", sysDns));

        // Use system DNS servers only when no two custom DNS servers specified
        if (listDns.size() <= 1)
            for (String def_dns : sysDns)
                try {
                    InetAddress ddns = InetAddress.getByName(def_dns);
                    if (!listDns.contains(ddns) &&
                            !(ddns.isLoopbackAddress() || ddns.isAnyLocalAddress()) &&
                            ddns instanceof Inet4Address)
                        listDns.add(ddns);
                } catch (Throwable ex) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }

        return listDns;
    }

    static {
        System.loadLibrary("netguard");
    }

    public static final String TAG = "ServiceSinkhole";

    private final BroadcastReceiver broadcastReceiver;

    private volatile Looper commandLooper;
    private volatile CommandHandler commandHandler;

    public ServiceSinkhole() {
        super();

        this.broadcastReceiver = new InspectorBroadcastReceiver(this);
    }

    private ParcelFileDescriptor startVPN(Builder builder) throws SecurityException {
        try {
            return builder.establish();
        } catch (SecurityException ex) {
            throw ex;
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            return null;
        }
    }

    private Builder getBuilder() {
        // Build VPN service
        Builder builder = new Builder();
        builder.setSession("Inspector");

        // VPN address
        String vpn4 = "10.1.10.1";
        Log.i(TAG, "vpn4=" + vpn4);
        builder.addAddress(vpn4, 32);

        String vpn6 = "fd00:1:fd00:1:fd00:1:fd00:1";
        Log.i(TAG, "vpn6=" + vpn6);
        builder.addAddress(vpn6, 128);

        // DNS address
        for (InetAddress dns : getDns(ServiceSinkhole.this)) {
            if (dns instanceof Inet4Address) {
                Log.i(TAG, "dns=" + dns);
                builder.addDnsServer(dns);
            }
        }

        // Subnet routing

        // Exclude IP ranges
        List<IPUtil.CIDR> listExclude = new ArrayList<>();
        listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

        // USB tethering 192.168.42.x
        // Wi-Fi tethering 192.168.43.x
        listExclude.add(new IPUtil.CIDR("192.168.42.0", 23));
        // Wi-Fi direct 192.168.49.x
        listExclude.add(new IPUtil.CIDR("192.168.49.0", 24));

        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni != null && ni.isUp() && !ni.isLoopback() &&
                        ni.getName() != null && !ni.getName().startsWith("tun"))
                    for (InterfaceAddress ia : ni.getInterfaceAddresses())
                        if (ia.getAddress() instanceof Inet4Address) {
                            IPUtil.CIDR local = new IPUtil.CIDR(ia.getAddress(), ia.getNetworkPrefixLength());
                            Log.i(TAG, "Excluding " + ni.getName() + " " + local);
                            listExclude.add(local);
                        }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }

        // https://en.wikipedia.org/wiki/Mobile_country_code
        Configuration config = getResources().getConfiguration();

        // T-Mobile Wi-Fi calling
        if (config.mcc == 310 && (config.mnc == 160 ||
                config.mnc == 200 ||
                config.mnc == 210 ||
                config.mnc == 220 ||
                config.mnc == 230 ||
                config.mnc == 240 ||
                config.mnc == 250 ||
                config.mnc == 260 ||
                config.mnc == 270 ||
                config.mnc == 310 ||
                config.mnc == 490 ||
                config.mnc == 660 ||
                config.mnc == 800)) {
            listExclude.add(new IPUtil.CIDR("66.94.2.0", 24));
            listExclude.add(new IPUtil.CIDR("66.94.6.0", 23));
            listExclude.add(new IPUtil.CIDR("66.94.8.0", 22));
            listExclude.add(new IPUtil.CIDR("208.54.0.0", 16));
        }

        // Verizon wireless calling
        if ((config.mcc == 310 &&
                (config.mnc == 4 ||
                        config.mnc == 5 ||
                        config.mnc == 6 ||
                        config.mnc == 10 ||
                        config.mnc == 12 ||
                        config.mnc == 13 ||
                        config.mnc == 350 ||
                        config.mnc == 590 ||
                        config.mnc == 820 ||
                        config.mnc == 890 ||
                        config.mnc == 910)) ||
                (config.mcc == 311 && (config.mnc == 12 ||
                        config.mnc == 110 ||
                        (config.mnc >= 270 && config.mnc <= 289) ||
                        config.mnc == 390 ||
                        (config.mnc >= 480 && config.mnc <= 489) ||
                        config.mnc == 590)) ||
                (config.mcc == 312 && (config.mnc == 770))) {
            listExclude.add(new IPUtil.CIDR("66.174.0.0", 16)); // 66.174.0.0 - 66.174.255.255
            listExclude.add(new IPUtil.CIDR("66.82.0.0", 15)); // 69.82.0.0 - 69.83.255.255
            listExclude.add(new IPUtil.CIDR("69.96.0.0", 13)); // 69.96.0.0 - 69.103.255.255
            listExclude.add(new IPUtil.CIDR("70.192.0.0", 11)); // 70.192.0.0 - 70.223.255.255
            listExclude.add(new IPUtil.CIDR("97.128.0.0", 9)); // 97.128.0.0 - 97.255.255.255
            listExclude.add(new IPUtil.CIDR("174.192.0.0", 9)); // 174.192.0.0 - 174.255.255.255
            listExclude.add(new IPUtil.CIDR("72.96.0.0", 9)); // 72.96.0.0 - 72.127.255.255
            listExclude.add(new IPUtil.CIDR("75.192.0.0", 9)); // 75.192.0.0 - 75.255.255.255
            listExclude.add(new IPUtil.CIDR("97.0.0.0", 10)); // 97.0.0.0 - 97.63.255.255
        }

        // Broadcast
        listExclude.add(new IPUtil.CIDR("224.0.0.0", 3));

        Collections.sort(listExclude);

        try {
            InetAddress start = InetAddress.getByName("0.0.0.0");
            for (IPUtil.CIDR exclude : listExclude) {
                Log.i(TAG, "Exclude " + exclude.getStart().getHostAddress() + "..." + exclude.getEnd().getHostAddress());
                for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                    try {
                        builder.addRoute(include.address, include.prefix);
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    }
                start = IPUtil.plus1(exclude.getEnd());
            }
            for (IPUtil.CIDR include : IPUtil.toCIDR("224.0.0.0", "255.255.255.255"))
                try {
                    builder.addRoute(include.address, include.prefix);
                } catch (Throwable ex) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                }
        } catch (UnknownHostException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }

        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        // MTU
        int mtu = jni_get_mtu();
        Log.i(TAG, "MTU=" + mtu);
        builder.setMtu(mtu);

        return builder;
    }

    private void startNative(final ParcelFileDescriptor vpn) {
        if (tunnelThread == null) {
            @SuppressLint("WorldReadableFiles")
            SharedPreferences pref = this.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
            boolean debug = this.debug || pref.getBoolean("pref_vpn_debug", false);
            Log.i(TAG, "Starting tunnel thread, debug=" + debug + ", context=0x" + Long.toHexString(jni_context) + ", obj=" + this + ", socksServer=" + socksServer + ", socksPort=" + socksPort);

            if (!StringUtils.isEmpty(socksServer) && socksPort > 1024) {
                jni_socks5(socksServer, socksPort, "", "");
            }
            jni_start(jni_context, debug ? Log.DEBUG : Log.ERROR);

            tunnelThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Running tunnel");
                    jni_run(jni_context, vpn.getFd(), false, 3);
                    Log.i(TAG, "Tunnel exited");
                    tunnelThread = null;
                }
            });
            tunnelThread.setPriority(Thread.MAX_PRIORITY);
            tunnelThread.start();

            Log.i(TAG, "Started tunnel thread");
        }
    }

    private void stopNative(boolean clear) {
        Log.i(TAG, "Stop native clear=" + clear);

        if (tunnelThread != null) {
            Log.i(TAG, "Stopping tunnel thread: context=0x" + Long.toHexString(jni_context) + ", obj=" + this);

            jni_stop(jni_context);

            Thread thread = tunnelThread;
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }
            tunnelThread = null;

            if (clear)
                jni_clear(jni_context);

            Log.i(TAG, "Stopped tunnel thread");
        }
    }

    private void stopVPN(ParcelFileDescriptor pfd) {
        Log.i(TAG, "Stopping");
        try {
            pfd.close();
        } catch (IOException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    private X509Certificate rootCert;
    private PrivateKey privateKey;

    @Override
    public void onCreate() {
        jni_context = jni_init(Build.VERSION.SDK_INT);
        Log.d(TAG, "onCreate context=0x" + Long.toHexString(jni_context) + ", obj=" + this);

        super.onCreate();

        HandlerThread commandThread = new HandlerThread("Netguard command", Process.THREAD_PRIORITY_FOREGROUND);
        commandThread.start();

        commandLooper = commandThread.getLooper();
        commandHandler = new CommandHandler(commandLooper);

        IntentFilter filter = new IntentFilter();
        filter.addAction(InspectorBroadcastListener.CONSOLE_CONNECTED);
        filter.addAction(InspectorBroadcastListener.CONSOLE_DISCONNECTED);
        filter.addAction(InspectorBroadcastListener.ACTIVITY_RESUME);
        filter.addAction(InspectorBroadcastListener.REQUEST_STOP_VPN);
        registerReceiver(broadcastReceiver, filter);

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream inputStream = getAssets().open("charles-ssl-proxying.p12")) {
                keyStore.load(inputStream, "charles".toCharArray());
            }
            rootCert = (X509Certificate) keyStore.getCertificate("charles");
            privateKey = (PrivateKey) keyStore.getKey("charles", null);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            Log.i(TAG, "initialize ssl context failed", e);
        }
    }

    private IPacketCapture packetCapture;
    private int uid, pid;
    private boolean debug;

    private String socksServer;
    private int socksPort;

    private long jni_context;
    private Thread tunnelThread;
    private ParcelFileDescriptor vpn;

    public static final String EXTRA_COMMAND = "Command";
    private static final String EXTRA_REASON = "Reason";
    private static final String EXTRA_PACKET = "Packet";

    private int extraUid;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received " + intent);
        Util.logExtras(intent);

        // Handle service restart
        if (intent == null) {
            Log.i(TAG, "Restart");

            // Recreate intent
            intent = new Intent(this, ServiceSinkhole.class);
            intent.putExtra(EXTRA_COMMAND, Command.start);
        }

        Bundle bundle = intent.getBundleExtra(Bundle.class.getCanonicalName());
        packetCapture = bundle == null ? null : IPacketCapture.Stub.asInterface(bundle.getBinder(InspectVpnService.INSPECTOR_KEY));
        uid = bundle == null ? -1 : bundle.getInt(InspectVpnService.UID_KEY);
        pid = bundle == null ? -1 : bundle.getInt(InspectorBroadcastReceiver.PID_KEY);
        debug = bundle != null && bundle.getBoolean(InspectVpnService.DEBUG_KEY);
        socksServer = bundle == null ? null : bundle.getString(InspectVpnService.SOCKS_HOST_KEY);
        socksPort = bundle == null ? 0 : bundle.getInt(InspectVpnService.SOCKS_PORT_KEY);
        extraUid = bundle == null ? 0 : bundle.getInt(InspectVpnService.EXTRA_UID_KEY);

        Command cmd = (Command) intent.getSerializableExtra(EXTRA_COMMAND);
        if (cmd == null) {
            intent.putExtra(EXTRA_COMMAND, Command.start);
        }
        String reason = intent.getStringExtra(EXTRA_REASON);
        Log.i(TAG, "Start intent=" + intent + " command=" + cmd + " reason=" + reason + " vpn=" + (vpn != null) + " user=" + (Process.myUid() / 100000) + ", packetCapture=" + packetCapture + ", uid=" + uid + ", pid=" + pid + ", extraUid=" + extraUid);

        commandHandler.queue(intent);
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        Log.i(TAG, "Revoke");

        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");

        commandLooper.quit();

        unregisterReceiver(broadcastReceiver);

        try {
            if (vpn != null) {
                stopNative(true);
                stopVPN(vpn);
                vpn = null;
            }
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }

        jni_done(jni_context);

        super.onDestroy();
    }

    private native long jni_init(int sdk);

    private native void jni_start(long context, int loglevel);

    private native void jni_run(long context, int tun, boolean fwd53, int rcode);

    private native void jni_stop(long context);

    private native void jni_clear(long context);

    private native int jni_get_mtu();

    @SuppressWarnings("unused")
    private native int[] jni_get_stats(long context);

    @SuppressWarnings("unused")
    private static native void jni_pcap(String name, int record_size, int file_size);

    @SuppressWarnings("unused")
    private native void jni_socks5(String addr, int port, String username, String password);

    private native void jni_done(long context);

    // Called from native code
    @SuppressWarnings("unused")
    private void nativeExit(String reason) {
        Log.w(TAG, "Native exit reason=" + reason);
    }

    // Called from native code
    @SuppressWarnings("unused")
    private void nativeError(int error, String message) {
        Log.w(TAG, "Native error " + error + ": " + message);
    }

    // Called from native code
    @SuppressWarnings("unused")
    private void logPacket(Packet packet) {
        // Log.d(TAG, "logPacket packet " + packet + ", data=" + packet.data);
    }

    // Called from native code
    @SuppressWarnings("unused")
    private void dnsResolved(ResourceRecord rr) {
        Log.i(TAG, "dnsResolved rr=" + rr);
    }

    // Called from native code
    @SuppressWarnings("unused")
    private boolean isDomainBlocked(String name) {
        Log.d(TAG, "check block domain name=" + name);
        return false;
    }

    private boolean isSupported(int protocol) {
        return (protocol == 1 /* ICMPv4 */ ||
                protocol == 59 /* ICMPv6 */ ||
                protocol == 6 /* TCP */ ||
                protocol == 17 /* UDP */);
    }

    private static final int SYSTEM_UID = 2000;

    // Called from native code
    @SuppressWarnings("unused")
    private Allowed isAddressAllowed(Packet packet) {
        packet.allowed = false;
        if (packet.uid < SYSTEM_UID && isSupported(packet.protocol)) {
            // Allow unknown system traffic
            packet.allowed = true;
            // Log.w(TAG, "Allowing unknown system " + packet);
        } else if (packet.uid == uid && isSupported(packet.protocol)) {
            packet.allowed = true;
            // Log.d(TAG, "Allowing inspector app " + packet);
        } else if(extraUid > 0 && packet.uid == extraUid && isSupported(packet.protocol)) {
            packet.allowed = true;
        }

        Allowed allowed = null;
        long start = System.currentTimeMillis();
        if (packet.allowed) {
            TcpRedirectRule[] rules = this.redirectRules;
            if (packet.protocol == 6 /* TCP */ && packet.uid == uid && rules != null) {
                for (TcpRedirectRule rule : rules) {
                    allowed = rule.createRedirect(packet);
                    if (allowed != null) {
                        break;
                    }
                }
            }

            if (packet.protocol == 6 && packet.version == 4 && packet.uid == uid && packet.isSSL()) { // ipv4
                allowed = mitm(packet);
            }

            if (allowed == null) {
                allowed = new Allowed();
            }
        }

        if (allowed != null) {
            if (packet.protocol != 6 /* TCP */ || !"".equals(packet.flags)) {
                logPacket(packet);
            }
        }

        Log.d(TAG, "isAddressAllowed allowed=" + allowed + ", packet: " + packet + ", offset=" + (System.currentTimeMillis() - start) + "ms");

        return allowed;
    }

    private Allowed mitm(Packet packet) {
        try {
            return SSLProxy.create(this, rootCert, privateKey, packet).startProxy();
        } catch (Exception e) {
            Log.d(TAG, "mitm failed: " + packet, e);
            return null;
        }
    }

    // Called from native code
    @SuppressWarnings("unused")
    private void accountUsage(Usage usage) {
        // Log.d(TAG, "accountUsage usage=" + usage);
    }

    // Called from native code
    @SuppressWarnings("unused")
    private void notifyPacket(int uid, byte[] packet) {
        if (uid == this.uid) {
            Intent intent = new Intent(this, ServiceSinkhole.class);
            intent.putExtra(EXTRA_COMMAND, Command.packet);
            intent.putExtra(EXTRA_PACKET, packet);
            commandHandler.queue(intent);
        }
    }

    @Override
    public void onConsoleConnected(int uid, String packageName, IPacketCapture packetCapture, int pid, String processName) {
        Log.d(TAG, "onConsoleConnected uid=" + uid + ", packageName=" + packageName + ", packetCapture=" + packetCapture + ", pid=" + pid + ", processName=" + processName);

        enableNetwork(uid, packetCapture, pid);
    }

    @Override
    public void onConsoleDisconnected(int uid, String packageName, int pid, String processName) {
        Log.d(TAG, "onConsoleDisconnected uid=" + uid + ", packageName=" + packageName + ", pid=" + pid + ", processName=" + processName);

        disableNetwork();
    }

    private TcpRedirectRule[] redirectRules;

    private void enableNetwork(int uid, IPacketCapture packetCapture, int pid) {
        this.uid = uid;
        this.packetCapture = packetCapture;
        this.pid = pid;
    }

    private void disableNetwork() {
        this.uid = -1;
        this.pid = -1;
        this.packetCapture = null;
    }

    @Override
    public void onActivityResume(int uid, String packageName, IPacketCapture packetCapture, int pid, String processName) {
        Log.d(TAG, "onActivityResume uid=" + uid + ", packageName=" + packageName + ", packetCapture=" + packetCapture + ", pid=" + pid + ", processName=" + processName);

        if (this.uid == uid && this.pid != pid && packetCapture == null) { // 不同进程
            return;
        }

        if (packetCapture != null) {
            enableNetwork(uid, packetCapture, pid);
        }
    }

    @Override
    public void onRequestStopVpn() {
        stop("onRequestStopVpn", this);
    }

    public static void run(String reason, Context context) {
        Intent intent = new Intent(context, ServiceSinkhole.class);
        intent.putExtra(EXTRA_COMMAND, Command.run);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void start(String reason, Context context) {
        Intent intent = new Intent(context, ServiceSinkhole.class);
        intent.putExtra(EXTRA_COMMAND, Command.start);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void reload(String reason, Context context) {
        Intent intent = new Intent(context, ServiceSinkhole.class);
        intent.putExtra(EXTRA_COMMAND, Command.reload);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void stop(String reason, Context context) {
        Intent intent = new Intent(context, ServiceSinkhole.class);
        intent.putExtra(EXTRA_COMMAND, Command.stop);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    @Override
    public IPacketCapture getPacketCapture() {
        return packetCapture;
    }
}
