package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractInspector;

class AddressCommand implements Command {

    private final String host;

    AddressCommand(String host) {
        this.host = host;
    }

    @Override
    public void execute(StringBuffer lua, Inspector inspector, ModuleContext context) {
        AbstractInspector ai = (AbstractInspector) inspector;
        ai.setLastConnectedHost(host);
    }

}
