package com.fuzhu8.inspector.vpn;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * SelectionKey processor
 * Created by zhkl0228 on 2017/1/19.
 */

public interface SelectionKeyProcessor {

    boolean processWritableKey(SelectionKey key) throws IOException, VpnException;

    boolean processReadableKey(SelectionKey key) throws IOException, VpnException;

    boolean processConnectableKey(SelectionKey key) throws IOException, VpnException;

    boolean processAcceptableKey(SelectionKey key) throws IOException, VpnException;

    void checkRegisteredKey(SelectionKey key, long currentTimeMillis) throws IOException, VpnException;

    void exceptionRaised(SelectionKey key, IOException exception) throws IOException, VpnException;

}
