package com.fuzhu8.inspector.dex.jf;

import com.fuzhu8.inspector.Inspector;

/**
 * default worker listener implementation
 * Created by zhkl0228 on 2018/3/24.
 */
class $WorkerListener implements WorkerListener {
    private final int total;
    private final Inspector inspector;
    private int current;

    $WorkerListener(int total, Inspector inspector) {
        this.total = total;
        this.inspector = inspector;
    }

    @Override
    public synchronized void notifyBegin(String msg) {
        if (current++ < total) {
            int percent = (current * 100) / total;
            String p = (percent < 10) ? (" " + percent + '%') : (percent < 100) ? ("" + percent + '%') : ("" + percent);
            if (inspector != null) {
                inspector.println("[" + p + "]" + msg);
            }
        }
    }

    @Override
    public void notifyException(Exception e) {
        if (inspector != null) {
            inspector.println(e);
        }
    }
}
