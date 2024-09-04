package me.topchetoeu.jscript.compilation.scope;

import java.util.function.IntSupplier;

public final class Variable {
    private IntSupplier indexSupplier;
    private boolean frozen;

    public final boolean readonly;
    public final String name;

    public final int index() {
        if (!frozen) throw new IllegalStateException("Tried to access the index of a variable before it was finalized");
        return indexSupplier.getAsInt();
    }

    public final void freeze() {
        this.frozen = true;
    }

    public final Variable setIndexSupplier(IntSupplier index) {
        this.indexSupplier = index;
        return this;
    }
    public final IntSupplier indexSupplier() {
        return indexSupplier;
    }

    public final Variable clone() {
        return new Variable(name, readonly).setIndexSupplier(indexSupplier);
    }

    public Variable(String name, boolean readonly) {
        this.name = name;
        this.readonly = readonly;
    }

    public static Variable of(String name, boolean readonly, int i) {
        return new Variable(name, readonly).setIndexSupplier(() -> i);
    }
}
