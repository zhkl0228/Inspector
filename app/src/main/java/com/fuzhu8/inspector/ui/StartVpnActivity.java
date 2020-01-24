package com.fuzhu8.inspector.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.fuzhu8.inspector.content.InspectorBroadcastReceiver;
import com.fuzhu8.inspector.vpn.InspectVpnService;

import eu.faircode.netguard.ServiceSinkhole;

/**
 * start vpn activity
 * Created by zhkl0228 on 2018/1/11.
 */

public class StartVpnActivity extends Activity {

    private static final String TAG = StartVpnActivity.class.getSimpleName();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LauncherPreferenceFragment.VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, ServiceSinkhole.class);
            Bundle bundle = new Bundle();
            bundle.putString(InspectVpnService.PACKAGE_NAME_KEY, this.getPackageName());
            bundle.putInt(InspectVpnService.UID_KEY, extraBundle == null ? -1 : extraBundle.getInt(InspectVpnService.UID_KEY));
            bundle.putBoolean(InspectVpnService.DEBUG_KEY, false);
            if (socksHost != null && socksPort != 0) {
                bundle.putString(InspectVpnService.SOCKS_HOST_KEY, socksHost);
                bundle.putInt(InspectVpnService.SOCKS_PORT_KEY, socksPort);
            }
            bundle.putInt(InspectVpnService.EXTRA_UID_KEY, extraUid);
            IBinder binder = extraBundle == null ? null : extraBundle.getBinder(InspectVpnService.INSPECTOR_KEY);
            if (binder != null) {
                bundle.putBinder(InspectVpnService.INSPECTOR_KEY, binder);
            }
            bundle.putInt(InspectorBroadcastReceiver.PID_KEY, extraBundle == null ? -1 : extraBundle.getInt(InspectorBroadcastReceiver.PID_KEY));
            intent.putExtra(Bundle.class.getCanonicalName(), bundle);
            startService(intent);
        }

        finish();
    }

    private String socksHost;
    private int socksPort;
    private int extraUid;
    private Bundle extraBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.d(TAG, "Request start vpn: intent=" + intent);
        socksHost = intent.getStringExtra(InspectVpnService.SOCKS_HOST_KEY);
        socksPort = intent.getIntExtra(InspectVpnService.SOCKS_PORT_KEY, 0);
        extraUid = intent.getIntExtra(InspectVpnService.EXTRA_UID_KEY, 0);
        extraBundle = intent.getBundleExtra(Bundle.class.getCanonicalName());

        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, LauncherPreferenceFragment.VPN_REQUEST_CODE);
        } else {
            onActivityResult(LauncherPreferenceFragment.VPN_REQUEST_CODE, Activity.RESULT_OK, null);
        }
    }

}
