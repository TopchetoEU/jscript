package me.topchetoeu.jscript.compilation.scope;

import java.util.HashMap;

import me.topchetoeu.jscript.common.parsing.Location;

public class FunctionScope extends Scope {
    private final VariableList captures = new VariableList();
    private final VariableList locals = new VariableList(captures);
    private HashMap<VariableDescriptor, VariableDescriptor> childToParent = new HashMap<>();

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
        if (locals.has(name)) return locals.get(name);
        if (captures.has(name)) return captures.get(name);

        var parentVar = parent.get(name, true);
        var childVar = captures.add(parentVar);

        childToParent.put(childVar, parentVar);

        return childVar;
    }

    public int localsCount() {
        return locals.size();
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

    public FunctionScope() { super(); }
    public FunctionScope(Scope parent) { super(parent); }
}
