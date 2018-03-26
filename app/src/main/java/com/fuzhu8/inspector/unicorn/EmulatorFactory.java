package com.fuzhu8.inspector.unicorn;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.unicorn.arm.DefaultARMEmulator;

/**
 * emulator factory
 * Created by zhkl0228 on 2017/5/2.
 */

public class EmulatorFactory {

    /**
     * @return arm emulator
     */
    public static Emulator createARMEmulator(Inspector inspector) {
        return new DefaultARMEmulator(inspector, false);
    }

    /**
     * @return thumb emulator
     */
    public static Emulator createThumbEmulator(Inspector inspector) {
        return new DefaultARMEmulator(inspector, true);
    }

}
