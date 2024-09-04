package me.topchetoeu.jscript.compilation.scope;

public abstract class VariableDescriptor {
    public final boolean readonly;
    public final String name;

    public abstract int index();

    public VariableDescriptor(String name, boolean readonly) {
        this.name = name;
        this.readonly = readonly;
    }

    public static VariableDescriptor of(String name, boolean readonly, int i) {
        return new VariableDescriptor(name, readonly) {
            @Override public int index() { return i; }
        };
    }
}
