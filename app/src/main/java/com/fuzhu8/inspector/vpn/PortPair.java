package com.fuzhu8.inspector.vpn;

/**
 * port pair
 * Created by zhkl0228 on 2017/1/18.
 */

public class PortPair {

    private final int local;
    private final int remote;

    public PortPair(int local, int remote) {
        super();

        this.local = local;
        this.remote = remote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortPair portPair = (PortPair) o;

        if (local != portPair.local) return false;
        return remote == portPair.remote;

    }

    @Override
    public int hashCode() {
        int result = local;
        result = 31 * result + remote;
        return result;
    }

    @Override
    public String toString() {
        return "PortPair{" +
                "local=" + local +
                ", remote=" + remote +
                '}';
    }
}
