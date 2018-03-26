package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.tcpcap.handler.DefaultSessionHandler;
import com.fuzhu8.tcpcap.handler.SessionHandler;
import com.fuzhu8.tcpcap.sniffer.ExceptionHandler;
import com.fuzhu8.tcpcap.sniffer.KrakenPcapSniffer;

import org.krakenapps.pcap.PcapInputStream;
import org.krakenapps.pcap.file.PcapFileInputStream;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * pcap command
 * Created by zhkl0228 on 2017/5/13.
 */

class PcapCommand implements Command {

    private byte[] pcapData;

    PcapCommand(byte[] pcapData) {
        this.pcapData = pcapData;
    }

    @Override
    public void execute(StringBuffer lua, final Inspector inspector, final ModuleContext context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Plugin> plugins = context.getPlugins();

                    if (plugins == null || plugins.isEmpty()) {
                        doSniffer(new DefaultSessionHandler(inspector), inspector);
                        return;
                    }

                    for (Plugin plugin : plugins) {
                        SessionHandler handler = plugin.createSessionHandler();
                        if (handler != null) {
                            doSniffer(handler, inspector);
                        }
                    }
                } catch (Exception e) {
                    inspector.println(e);
                }
            }
        }).start();
    }

    private void doSniffer(SessionHandler sessionHandler, final Inspector inspector) throws Exception {
        PcapInputStream pcap = new PcapFileInputStream(new ByteArrayInputStream(pcapData));
        KrakenPcapSniffer sniffer = new KrakenPcapSniffer(pcap, sessionHandler, null);
        sniffer.setExceptionHandler(new ExceptionHandler() {
            @Override
            public boolean handleException(Exception exception) {
                inspector.println(exception);
                return true;
            }
        });
        sniffer.startSniffer();
        sniffer.stopSniffer();
        inspector.println("Execute pcap file successfully!");
    }
}
