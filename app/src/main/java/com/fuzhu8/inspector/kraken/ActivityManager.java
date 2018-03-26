package com.fuzhu8.inspector.kraken;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;

/**
 * activity manager for command line
 * Created by zhkl0228 on 2018/1/18.
 */

public interface ActivityManager {

    void registerReceiver(IIntentReceiver intentReceiver, IntentFilter intentFilter) throws RemoteException;

    void unregisterReceiver(android.content.IIntentReceiver intentReceiver) throws RemoteException;

    void sendBroadcast(Intent intent) throws RemoteException;

}
