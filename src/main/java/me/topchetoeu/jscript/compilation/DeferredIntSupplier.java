package me.topchetoeu.jscript.compilation;

import java.util.function.IntSupplier;

public final class DeferredIntSupplier implements IntSupplier {
    private int value;
    private boolean set;

    public void set(int val) {
        if (set) throw new RuntimeException("A deferred int supplier may be set only once");
        value = val;
        set = true;
    }

    @Override public int getAsInt() {
        if (!set) throw new RuntimeException("Deferred int supplier accessed too early");
        return value;
    }
}