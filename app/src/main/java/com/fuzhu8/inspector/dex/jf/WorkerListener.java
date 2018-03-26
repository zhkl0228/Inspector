package com.fuzhu8.inspector.dex.jf;

/**
 * worker listener
 * Created by zhkl0228 on 2018/3/24.
 */
interface WorkerListener {

    void notifyBegin(String msg);

    void notifyException(Exception e);
}
