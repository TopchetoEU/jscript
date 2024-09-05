package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;
import java.util.HashSet;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class FunctionScope extends Scope {
    private final VariableList captures = new VariableList().setIndexMap(v -> ~v);
    private final VariableList specials = new VariableList();
    private final VariableList locals = new VariableList(specials);
    private final HashMap<Variable, Variable> childToParent = new HashMap<>();
    private final HashSet<String> blacklistNames = new HashSet<>();

    private final Scope captureParent;

    public final boolean passtrough;

    private void removeCapture(String name) {
        var res = captures.remove(name);
        if (res != null) {
            childToParent.remove(res);
            res.setIndexSupplier(() -> { throw new SyntaxException(null, res.name + " has been shadowed"); });
        }
    }

    @Override public Variable define(Variable var, Location loc) {
        checkNotEnded();
        if (variables.has(var.name)) throw alreadyDefinedErr(loc, var.name);

        if (passtrough) {
            blacklistNames.add(var.name);
            return null;
        }

        removeCapture(var.name);
        return locals.add(var);
    }
    @Override public Variable defineStrict(Variable var, Location loc) {
        checkNotEnded();
        if (locals.has(var.name)) throw alreadyDefinedErr(loc, var.name);
        if (blacklistNames.contains(var.name)) throw alreadyDefinedErr(loc, var.name);

        var res = super.defineStrict(var, loc);
        removeCapture(var.name);
        return res;
    }
    public Variable defineSpecial(Variable var, Location loc) {
        return specials.add(var);
    }

    @Override public boolean flattenVariable(Variable variable, boolean capturable) {
        // if (!ended()) throw new IllegalStateException("Tried to flatten a variable before the scope has ended");
        this.locals.overlay(variable);
        return true;
    }

    @Override public Variable get(String name, boolean capture) {
        var superRes = super.get(name, capture);
        if (superRes != null) return superRes;

        if (specials.has(name)) return addCaptured(specials.get(name), capture);
        if (locals.has(name)) return addCaptured(locals.get(name), capture);
        if (captures.has(name)) return addCaptured(captures.get(name), capture);

        if (captureParent == null) return null;

        var parentVar = captureParent.get(name, true);
        if (parentVar == null) return null;

        var childVar = captures.add(parentVar.clone());

        childToParent.put(childVar, parentVar);

        return childVar;
    }

    @Override public boolean has(String name, boolean capture) {
        if (specials.has(name)) return true;
        if (locals.has(name)) return true;

        if (capture) {
            if (captures.has(name)) return true;
            if (captureParent != null) return captureParent.has(name, true);
        }

        return false;
    }

    @Override public boolean finish() {
        if (!super.finish()) return false;

        captures.freeze();
        locals.freeze();
        specials.freeze();

        return true;
    }

    @Override public int allocCount() {
        return 0;
    }
    @Override public int capturesCount() {
        return captures.size();
    }
    @Override public int localsCount() {
        return locals.size() + specials.size() + super.allocCount();
    }

    public int offset() {
        return specials.size() + locals.size();
    }

    public int[] getCaptureIndices() {
        var res = new int[captures.size()];
        var i = 0;

        for (var el : captures.all()) {
            assert childToParent.containsKey(el);
            res[i] = childToParent.get(el).index();
            i++;
        }

        return res;
    }

    public FunctionScope(Scope parent) {
        super();
        if (parent.finished()) throw new RuntimeException("Parent is finished");
        this.captureParent = parent;
        this.passtrough = false;
    }
    public FunctionScope(boolean passtrough) {
        super();
        this.captureParent = null;
        this.passtrough = passtrough;
    }
}
