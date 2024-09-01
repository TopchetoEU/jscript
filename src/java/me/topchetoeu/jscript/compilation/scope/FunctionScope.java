package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;

import me.topchetoeu.jscript.common.parsing.Location;

public class FunctionScope extends Scope {
    private final VariableList captures = new VariableList().setIndexMap(v -> ~v);
    private final VariableList specials = new VariableList();
    private final VariableList locals = new VariableList(specials);
    private HashMap<VariableDescriptor, VariableDescriptor> childToParent = new HashMap<>();
    public final String selfName;
    public final VariableDescriptor selfVar;

    private void removeCapture(String name) {
        var res = captures.remove(name);
        if (res != null) childToParent.remove(res);
    }

    @Override public VariableDescriptor define(String name, boolean readonly, Location loc) {
        var old = locals.get(name);
        if (old != null) return old;

        removeCapture(name);
        return locals.add(name, readonly);
    }
    @Override public VariableDescriptor defineStrict(String name, boolean readonly, Location loc) {
        if (locals.has(name)) throw alreadyDefinedErr(loc, name);
        else if (parent == null) throw new RuntimeException("Strict variables may be defined only in local scopes");
        else return parent.defineStrict(name, readonly, loc);
    }

    @Override public VariableDescriptor get(String name, boolean capture) {
        if (specials.has(name)) return specials.get(name);
        if (locals.has(name)) return locals.get(name);
        if (captures.has(name)) return captures.get(name);
        if (selfName != null && selfName.equals(name)) return selfVar;

        var parentVar = parent.get(name, true);
        if (parentVar == null) return null;

        var childVar = captures.add(parentVar);

        childToParent.put(childVar, parentVar);

        return childVar;
    }

    @Override public boolean has(String name) {
        if (specials.has(name)) return true;
        if (locals.has(name)) return true;
        if (captures.has(name)) return true;
        if (parent != null) return parent.has(name);

        return false;
    }

    @Override public boolean end() {
        if (!super.end()) return false;

        captures.freeze();
        locals.freeze();
        return true;
    }

    @Override public int localsCount() {
        return locals.size() + specials.size();
    }
    @Override public int capturesCount() {
        return captures.size();
    }
    @Override public int allocCount() {
        return 0;
    }

    public int offset() {
        return captures.size() + locals.size();
    }

    public int[] getCaptureIndices() {
        var res = new int[captures.size()];
        var i = 0;

        for (var el : captures) {
            assert childToParent.containsKey(el);
            res[i] = childToParent.get(el).index();
        }

        return res;
    }

    public FunctionScope(String selfName, String[] args) {
        super();
        this.selfName = selfName;

        if (selfName != null) this.selfVar = VariableDescriptor.of(selfName, true, -1);
        else this.selfVar = null;

        for (var arg : args) specials.add(arg, false);
    }
    public FunctionScope(String selfName, String[] args, Scope parent) {
        super(parent);
        this.selfName = selfName;

        if (selfName != null) this.selfVar = VariableDescriptor.of(selfName, true, -1);
        else this.selfVar = null;

        for (var arg : args) specials.add(arg, false);
    }
}
