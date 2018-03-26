package com.fuzhu8.inspector.plugin;

import com.fuzhu8.inspector.ClientConnectListener;
import com.fuzhu8.tcpcap.handler.SessionHandler;

import android.app.PendingIntent;
import android.content.Context;

/**
 * @author zhkl0228
 *
 */
public interface Plugin extends ClientConnectListener {
	
	/**
	 * 初始化Context
	 */
	void initialize(Context context);
	
	String toString();
	
	void notifySendTextMessage(String destinationAddress,
			String scAddress, String text, PendingIntent sentIntent,
			PendingIntent deliveryIntent, String label);
	
	void notifySendDataMessage(String destinationAddress, String scAddress, short destinationPort, byte[] data,
			PendingIntent sentIntent, PendingIntent deliveryIntent);

	String getHelpContent();

	SessionHandler createSessionHandler();

}
