package me.topchetoeu.jscript.engine.scope;

import java.util.HashSet;
import java.util.Set;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;

public class GlobalScope implements ScopeRecord {
    public final ObjectValue obj;

    public boolean has(CallContext ctx, String name) throws InterruptedException {
        return obj.hasMember(ctx, name, false);
    }
    public Object getKey(String name) {
        return name;
    }

    public GlobalScope globalChild() {
        return new GlobalScope(this);
    }
    public LocalScopeRecord child() {
        return new LocalScopeRecord(this);
    }

    public Object define(String name) {
        try {
            if (obj.hasMember(null, name, true)) return name;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return name;
        }
        obj.defineProperty(name, null);
        return name;
    }
    public void define(String name, Variable val) {
        obj.defineProperty(name,
            new NativeFunction("get " + name, (ctx, th, a) -> val.get(ctx)),
            new NativeFunction("set " + name, (ctx, th, args) -> { val.set(ctx, args.length > 0 ? args[0] : null); return null; }),
            true, true
        );
    }
    public void define(String name, boolean readonly, Object val) {
        obj.defineProperty(name, val, readonly, true, true);
    }
    public void define(String... names) {
        for (var n : names) define(n);
    }
    public void define(boolean readonly, FunctionValue val) {
        define(val.name, readonly, val);
    }

    public Object get(CallContext ctx, String name) throws InterruptedException {
        if (!obj.hasMember(ctx, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        else return obj.getMember(ctx, name);
    }
    public void set(CallContext ctx, String name, Object val) throws InterruptedException {
        if (!obj.hasMember(ctx, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        if (!obj.setMember(ctx, name, val, false)) throw EngineException.ofSyntax("The global '" + name + "' is readonly.");
    }

    public Set<String> keys() {
        var res = new HashSet<String>();

        for (var key : keys()) {
            if (key instanceof String) res.add((String)key);
        }

        return res;
    }

    public GlobalScope() {
        this.obj = new ObjectValue();
    }
    public GlobalScope(GlobalScope parent) {
        this.obj = new ObjectValue();
        this.obj.setPrototype(null, parent.obj);
    }
}
