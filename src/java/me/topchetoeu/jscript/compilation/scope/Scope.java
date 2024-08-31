package me.topchetoeu.jscript.compilation.scope;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public abstract class Scope {
    public final Scope parent;
    private boolean active = true;
    private Scope child;

    protected final SyntaxException alreadyDefinedErr(Location loc, String name) {
        return new SyntaxException(loc, String.format("Identifier '%s' has already been declared", name));
    }

    /**
     * Defines an ES5-style variable
     * @returns The index supplier of the variable if it is a local, or null if it is a global
     * @throws SyntaxException If an ES2015-style variable with the same name exists anywhere from the current function to the current scope
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public abstract VariableDescriptor define(String name, boolean readonly, Location loc);
    /**
     * Defines an ES2015-style variable
     * @param readonly True if const, false if let
     * @return The index supplier of the variable
     * @throws SyntaxException If any variable with the same name exists in the current scope
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public abstract VariableDescriptor defineStrict(String name, boolean readonly, Location loc);
    /**
     * Gets the index supplier of the given variable name, or null if it is a global
     * 
     * @param capture Used to signal to the scope that the variable is going to be captured.
     * Not passing this could lead to a local variable being optimized out as an ES5-style variable,
     * which could break the semantics of a capture
     */
    public abstract VariableDescriptor get(String name, boolean capture);
    /**
     * Gets the index offset from this scope to its children
     */
    public abstract int offset();

    public boolean end() { 
        if (!active) return false;

        this.active = false;
        if (this.parent != null) {
            assert this.parent.child == this;
            this.parent.child = this;
        }

        return true;
    }

    public final boolean active() { return active; }
    public final Scope child() { return child; }

    public Scope() {
        this.parent = null;
    }
    public Scope(Scope parent) {
        if (!parent.active) throw new RuntimeException("Parent is not active");
        if (parent.child != null) throw new RuntimeException("Parent has an active child");

        this.parent = parent;
        this.parent.child = this;
    }
}
