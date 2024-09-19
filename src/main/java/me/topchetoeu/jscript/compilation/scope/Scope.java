package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;
import java.util.LinkedList;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;

public class Scope {
    protected final HashMap<String, Variable> strictVarMap = new HashMap<>();

    protected final VariableList locals = new VariableList(VariableIndex.IndexType.LOCALS, this::variableOffset);
    protected final VariableList capturables = new VariableList(VariableIndex.IndexType.CAPTURABLES, this::capturablesOffset);

    private boolean ended = false;
    private boolean finished = false;
    private Scope child;
    private LinkedList<Scope> children = new LinkedList<>();

    public final Scope parent;

    /**
     * Wether or not the scope is going to be entered multiple times.
     * If set to true, captured variables will be kept as allocations, otherwise will be converted to locals
     */
    public boolean singleEntry = true;


    protected void transferCaptured(Variable var) {
        if (!singleEntry) {
            this.capturables.add(var);
        }
        else if (parent != null) {
            parent.transferCaptured(var);
        }
        else throw new IllegalStateException("Couldn't transfer captured variable");
    }

    protected final Variable addCaptured(Variable var, boolean captured) {
        if (captured) transferCaptured(var);
        return var;
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
     * Defines a nameless variable for holding intermediate temporary values
     * 
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public Variable defineTemp() {
        checkNotEnded();
        return this.locals.add(new Variable("<temp>", false));
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
        if (strictVarMap.containsKey(var.name)) throw alreadyDefinedErr(loc, var.name);
        if (parent != null) return parent.define(var, loc);

        return null;
    }

    /**
     * Checks if this scope's function parent has a non-strict variable of the given name
     */
    public boolean hasNonStrict(String name) { return false; }

    /**
     * Defines an ES2015-style variable
     * @param readonly True if const, false if let
     * @return The index supplier of the variable
     * @throws SyntaxException If any variable with the same name exists in the current scope
     * @throws RuntimeException If the scope is finalized or has an active child
     */
    public Variable defineStrict(Variable var, Location loc) {
        checkNotEnded();
        if (strictVarMap.containsKey(var.name)) throw alreadyDefinedErr(loc, var.name);
        if (hasNonStrict(var.name)) throw alreadyDefinedErr(loc, var.name);

        strictVarMap.put(var.name, var);
        return locals.add(var);
    }
    /**
     * Gets the index supplier of the given variable name, or null if it is a global
     * 
     * @param capture If true, the variable is being captured by a function
     */
    public Variable get(String name, boolean capture) {
        var res = strictVarMap.get(name);

        if (res != null) return addCaptured(res, capture);
        if (parent != null) return parent.get(name, capture);

        return null;
    }
    /**
     * Checks if the given variable name is accessible
     * 
     * @param capture If true, will check beyond this function's scope
     */
    public boolean has(String name, boolean capture) {
        if (strictVarMap.containsKey(name)) return true;
        if (parent != null) return parent.has(name, capture);

        return false;
    }
    /**
     * Gets the index offset from this scope to its children
     */
    public final int variableOffset() {
        var res = 0;

        for (var curr = parent; curr != null; curr = curr.parent) {
            res += parent.locals.size();
        }

        return res;
    }
    public final int capturablesOffset() {
        var res = 0;

        for (var curr = this; curr != null; curr = curr.parent) {
            if (curr != this) res += parent.capturables.size();
            if (curr.parent == null) res += curr.localsCount();
        }

        return res;
    }

    public final Variable define(DeclarationType type, String name, Location loc) {
        if (type.strict) return defineStrict(new Variable(name, type.readonly), loc);
        else return define(new Variable(name, type.readonly), loc);
    }

    public int localsCount() {
        var res = 0;
        for (var child : children) {
            var childN = child.localsCount();
            if (res < childN) res = childN;
        }

        return res + locals.size();
    }
    public int capturesCount() { return 0; }
    public int allocCount() {
        var res = capturables.size();
        return res;
    }
    public int capturablesCount() {
        var res = capturables.size();
        for (var child : children) res += child.capturablesCount();
        return res;
    }

    public Iterable<Variable> capturables() {
        return capturables.all();
    }
    public Iterable<Variable> locals() {
        return locals.all();
    }

    /**
     * Ends this scope. This will make it possible for another child to take its place
     */
    public boolean end() {
        if (ended) return false;

        this.ended = true;

        if (this.parent != null) {
            assert this.parent.child == this;
            this.parent.child = null;
        }

        return true;
    }

    protected void onFinish() {
        this.locals.freeze();
        this.capturables.freeze();
    }

    /**
     * Finalizes this scope. The scope will become immutable after this call
     * @return
     */
    public final boolean finish() {
        if (finished) return false;

        if (parent != null && parent.finished) throw new IllegalStateException("Tried to finish a child after the parent was finished");
        this.onFinish();

        for (var child : children) child.finish();

        this.finished = true;

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
            this.parent.children.add(this);
        }
        else this.parent = null;
    }
}
