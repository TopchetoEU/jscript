package me.topchetoeu.jscript.compilation.scope;

import java.util.function.Supplier;

public final class Variable {
    private Supplier<VariableIndex> indexSupplier;
    private boolean frozen;

    public final boolean readonly;
    public final String name;

    public final VariableIndex index() {
        if (!frozen) throw new IllegalStateException("Tried to access the index of a variable before it was finalized");
        return indexSupplier.get();
    }

    public final void freeze() {
        this.frozen = true;
    }

    public final Variable setIndexSupplier(Supplier<VariableIndex> index) {
        this.indexSupplier = index;
        return this;
    }
    public final Supplier<VariableIndex> indexSupplier() {
        return indexSupplier;
    }

    public final Variable clone() {
        return new Variable(name, readonly).setIndexSupplier(indexSupplier);
    }

    public Variable(String name, boolean readonly) {
        this.name = name;
        this.readonly = readonly;
    }

    public static Variable of(String name, boolean readonly, VariableIndex index) {
        return new Variable(name, readonly).setIndexSupplier(() -> index);
    }
}
