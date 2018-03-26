package com.fuzhu8.inspector.unicorn;

import unicorn.Unicorn;

/**
 * function replacement
 * Created by zhkl0228 on 2017/5/11.
 */

public interface Alt {

    boolean replaceFunction(Unicorn unicorn, long address);

    void forceReturn(Unicorn unicorn);

}
