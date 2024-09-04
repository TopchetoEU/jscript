package me.topchetoeu.jscript.compilation.scope;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class Scope {
    protected final VariableList variables = new VariableList(this::parentOffset);

    private boolean ended = false;
    private boolean finished = false;
    private Scope child;
    private List<Scope> prevChildren = new LinkedList<>();

    public final Scope parent;
    public final HashSet<Variable> captured = new HashSet<>();

    /**
     * Wether or not the scope is going to be entered multiple times.
     * If set to true, captured variables will be kept as allocations, otherwise will be converted to locals
     */
    public boolean singleEntry = true;

    private final int parentOffset() {
        if (parent != null) return parent.offset();
        else return 0;
    }

    protected final SyntaxException alreadyDefinedErr(Location loc, String name) {
        return new SyntaxException(loc, String.format("Identifier '%s' has already been declared", name));
    }

    /**
     * Throws if the scope is ended
     */
    protected final void checkNotEnded() {
        if (ended) throw new IllegalStateException("Cannot define in an ended scope");
    }

    /**
     * Defines an ES5-style variable
     * 
     * @returns The index supplier of the variable if it is a local, or null if it is a global
     * @throws SyntaxException If an ES2015-style variable with the same name exists anywhere from the current function to the current scope
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public Variable define(Variable var, Location loc) {
        checkNotEnded();
        if (variables.has(var.name)) throw alreadyDefinedErr(loc, var.name);
        if (parent != null) return parent.define(var, loc);

        return null;
    }

    /**
     * Defines an ES2015-style variable
     * @param readonly True if const, false if let
     * @return The index supplier of the variable
     * @throws SyntaxException If any variable with the same name exists in the current scope
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public Variable defineStrict(Variable var, Location loc) {
        checkNotEnded();
        if (variables.has(var.name)) throw alreadyDefinedErr(loc, var.name);

        variables.add(var);
        return var.setIndexSupplier(() -> variables.indexOfKey(var.name));
    }
    /**
     * Gets the index supplier of the given variable name, or null if it is a global
     * 
     * @param capture If true, the variable is being captured by a function
     */
    public Variable get(String name, boolean capture) {
        var res = variables.get(name);
        if (res != null) return res;
        if (parent != null) return parent.get(name, capture);

        return null;
    }
    /**
     * Checks if the given variable name is accessible
     * 
     * @param capture If true, will check beyond this function's scope
     */
    public boolean has(String name, boolean capture) {
        if (variables.has(name)) return true;
        if (parent != null) return parent.has(name, capture);

        return false;
    }
    /**
     * Gets the index offset from this scope to its children
     */
    public int offset() {
        if (parent != null) return parent.offset() + variables.size();
        else return variables.size();
    }

    /**
     * Adds this variable to the current function's locals record. Capturable indicates whether or not the variable
     * should still be capturable, or be put in an array (still not implemented)
     * 
     * @return Whether or not the request was actually fuliflled
     */
    public boolean flattenVariable(Variable variable, boolean capturable) {
        if (parent == null) return false;
        return parent.flattenVariable(variable, capturable);
    }

    public int localsCount() { return 0; }
    public int capturesCount() { return 0; }
    public int allocCount() { return variables.size(); }

    /**
     * Ends this scope. This will make it possible for another child to take its place
     */
    public boolean end() {
        if (ended) return false;

        this.ended = true;

        for (var v : variables.all()) {
            if (captured.contains(v)) {
                if (singleEntry) this.flattenVariable(v, true);
            }
            else {
                this.flattenVariable(v, false);
            }
        }

        if (this.parent != null) {
            assert this.parent.child == this;
            this.parent.child = null;
        }

        return true;
    }

    /**
     * Finalizes this scope. The scope will become immutable after this call
     * @return
     */
    public boolean finish() {
        if (finished) return false;
        if (parent != null && !parent.finished) throw new IllegalStateException("Tried to finish a child before the parent was finished");

        this.variables.freeze();
        this.finished = true;

        for (var child : prevChildren) child.finish();

        return true;
    }

    public final boolean ended() { return ended; }
    public final boolean finished() { return finished; }
    public final Scope child() { return child; }

    public Scope() {
        this(null);
    }
    public Scope(Scope parent) {
        if (parent != null) {
            if (parent.ended) throw new RuntimeException("Parent is not active");
            if (parent.finished) throw new RuntimeException("Parent is finished");
            if (parent.child != null) throw new RuntimeException("Parent has an active child");

            this.parent = parent;
            this.parent.child = this;
            this.parent.prevChildren.add(this);
        }
        else this.parent = null;
    }
}
