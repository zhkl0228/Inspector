package com.fuzhu8.inspector.vpn;

/**
 * vpn forward exception
 * Created by zhkl0228 on 2017/1/20.
 */

public class VpnException extends Exception {

    public VpnException(Throwable throwable) {
        super(throwable);
    }

    public VpnException(String detailMessage) {
        super(detailMessage);
    }

}
