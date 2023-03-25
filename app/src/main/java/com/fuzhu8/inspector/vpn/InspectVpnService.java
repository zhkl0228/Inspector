package com.fuzhu8.inspector.vpn;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

/**
 * vpn service for inspector
 * Created by zhkl0228 on 2017/1/18.
 */

@SuppressLint("Registered")
public class InspectVpnService extends VpnService {

    public static final String INSPECTOR_KEY = "inspector";
    public static final String INSPECTOR_NAME = "AppSniffer";
    public static final String UID_KEY = "uid";
    public static final String EXTRA_UID_KEY = "extraUid";
    public static final String PACKAGE_NAME_KEY = "packageName";
    public static final String DEBUG_KEY = "debug";

    public static final String SOCKS_HOST_KEY = "socksHost";
    public static final String SOCKS_PORT_KEY = "socksPort";

    public static final String TEST_VPN_HOST_KEY = "testVpnHost";
    public static final String TEST_VPN_PORT_KEY = "testVPnPort";

    private VpnWorker worker;

    @Override
    public void onRevoke() {
        super.onRevoke();

        stopSelf();
        if (worker != null) {
            worker.stop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (worker != null && worker.isRunning()) {
            return super.onStartCommand(intent, flags, startId);
        }

        Bundle bundle = intent.getBundleExtra(Bundle.class.getCanonicalName());
        worker = new VpnWorker(IPacketCapture.Stub.asInterface(bundle.getBinder(INSPECTOR_KEY)), this, new VpnService.Builder());
        worker.start();
        return super.onStartCommand(intent, flags, startId);
    }
}
