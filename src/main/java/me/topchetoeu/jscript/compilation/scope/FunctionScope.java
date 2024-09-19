package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;
import java.util.HashSet;

import me.topchetoeu.jscript.common.parsing.Location;

public class FunctionScope extends Scope {
    private final VariableList captures = new VariableList(VariableIndex.IndexType.CAPTURES);

    private final HashMap<String, Variable> specialVarMap = new HashMap<>();
    private final HashMap<String, Variable> functionVarMap = new HashMap<>();
    private final HashMap<String, Variable> capturesMap = new HashMap<>();
    private final HashSet<String> blacklistNames = new HashSet<>();

    private final HashMap<Variable, Variable> childToParent = new HashMap<>();

    private final Scope captureParent;

    public final boolean passtrough;

    @Override public boolean hasNonStrict(String name) {
        if (functionVarMap.containsKey(name)) return true;
        if (blacklistNames.contains(name)) return true;

        return false;
    }

    @Override public Variable define(Variable var, Location loc) {
        checkNotEnded();
        if (strictVarMap.containsKey(var.name)) throw alreadyDefinedErr(loc, var.name);

        if (passtrough) {
            blacklistNames.add(var.name);
            return null;
        }
        else {
            functionVarMap.put(var.name, var);
            return locals.add(var);
        }
    }
    public Variable defineSpecial(Variable var, Location loc) {
        checkNotEnded();
        if (strictVarMap.containsKey(var.name)) throw alreadyDefinedErr(loc, var.name);

        specialVarMap.put(var.name, var);
        return locals.add(var);
    }

    @Override public Variable get(String name, boolean capture) {
        var superRes = super.get(name, capture);
        if (superRes != null) return superRes;

        if (specialVarMap.containsKey(name)) return addCaptured(specialVarMap.get(name), capture);
        if (functionVarMap.containsKey(name)) return addCaptured(functionVarMap.get(name), capture);
        if (capturesMap.containsKey(name)) return addCaptured(capturesMap.get(name), capture);

        if (captureParent == null) return null;

        var parentVar = captureParent.get(name, true);
        if (parentVar == null) return null;

        var childVar = captures.add(parentVar.clone());
        capturesMap.put(childVar.name, childVar);
        childToParent.put(childVar, parentVar);

        return childVar;
    }

    @Override public boolean has(String name, boolean capture) {
        if (functionVarMap.containsKey(name)) return true;
        if (specialVarMap.containsKey(name)) return true;

        if (capture) {
            if (capturesMap.containsKey(name)) return true;
            if (captureParent != null) return captureParent.has(name, true);
        }

        return false;
    }

    @Override protected void onFinish() {
        captures.freeze();
        super.onFinish();
    }

    @Override public int capturesCount() {
        return captures.size();
    }

    public int[] getCaptureIndices() {
        var res = new int[captures.size()];
        var i = 0;

        for (var el : captures.all()) {
            assert childToParent.containsKey(el);
            res[i] = childToParent.get(el).index().toCaptureIndex();
            i++;
        }

        return res;
    }

    public FunctionScope(Scope parent) {
        super();
        if (parent.finished()) throw new RuntimeException("Parent is finished");
        this.captureParent = parent;
        this.passtrough = false;
        this.singleEntry = false;
    }
    public FunctionScope(boolean passtrough) {
        super();
        this.captureParent = null;
        this.passtrough = passtrough;
        this.singleEntry = false;
    }
}
