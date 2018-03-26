package com.fuzhu8.inspector.kraken;

import android.app.Activity;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * activity manager factory
 * Created by zhkl0228 on 2018/1/18.
 */

class ActivityManagerFactory {

    private static class MyActivityManager implements ActivityManager {

        private final IActivityManager activityManager;
        private final Method registerReceiver, unregisterReceiver, broadcastIntent;

        MyActivityManager(IActivityManager activityManager) {
            super();
            this.activityManager = activityManager;

            try {
                registerReceiver = IActivityManager.class.getMethod("registerReceiver", IApplicationThread.class, String.class, IIntentReceiver.class, IntentFilter.class, String.class, int.class);
                unregisterReceiver = IActivityManager.class.getMethod("unregisterReceiver", IIntentReceiver.class);
                broadcastIntent = IActivityManager.class.getMethod("broadcastIntent", IApplicationThread.class, Intent.class, String.class, IIntentReceiver.class, int.class, String.class, Bundle.class, String.class, int.class, boolean.class, boolean.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void sendBroadcast(Intent intent) throws RemoteException {
            try {
                broadcastIntent.invoke(activityManager, null, intent, null, null, Activity.RESULT_OK, null, null, null, -1, false, true, 0);
            } catch (IllegalAccessException | InvocationTargetException e) {
                if (e.getCause() instanceof RemoteException) {
                    throw (RemoteException) e.getCause();
                }

                throw new IllegalStateException(e);
            }
        }

        @Override
        public void registerReceiver(IIntentReceiver intentReceiver, IntentFilter intentFilter) throws RemoteException {
            try {
                registerReceiver.invoke(activityManager, null, null, intentReceiver, intentFilter, null, 0);
            } catch (IllegalAccessException | InvocationTargetException e) {
                if (e.getCause() instanceof RemoteException) {
                    throw (RemoteException) e.getCause();
                }

                throw new IllegalStateException(e);
            }
        }

        @Override
        public void unregisterReceiver(IIntentReceiver intentReceiver) throws RemoteException {
            try {
                unregisterReceiver.invoke(activityManager, intentReceiver);
            } catch (IllegalAccessException | InvocationTargetException e) {
                if (e.getCause() instanceof RemoteException) {
                    throw (RemoteException) e.getCause();
                }

                throw new IllegalStateException(e);
            }
        }

    }

    static ActivityManager newActivityManager(IActivityManager activityManager) {
        return new MyActivityManager(activityManager);
    }

}
