package me.topchetoeu.jscript.engine.scope;

import java.util.HashSet;
import java.util.Set;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class GlobalScope implements ScopeRecord {
    public final ObjectValue obj;

    public boolean has(Context ctx, String name) {
        return Values.hasMember(null, obj, name, false);
    }
    public Object getKey(String name) {
        return name;
    }

    public GlobalScope globalChild() {
        var obj = new ObjectValue();
        Values.setPrototype(null, obj, this.obj);
        return new GlobalScope(obj);
    }
    public LocalScopeRecord child() {
        return new LocalScopeRecord();
    }

    public Object define(String name) {
        if (Values.hasMember(Context.NULL, obj, name, false)) return name;
        obj.defineProperty(Context.NULL, name, null);
        return name;
    }
    public void define(String name, Variable val) {
        obj.defineProperty(Context.NULL, name,
            new NativeFunction("get " + name, args -> val.get(args.ctx)),
            new NativeFunction("set " + name, args -> { val.set(args.ctx, args.get(0)); return null; }),
            true, true
        );
    }
    public void define(Context ctx, String name, boolean readonly, Object val) {
        obj.defineProperty(ctx, name, val, readonly, true, true);
    }
    public void define(String ...names) {
        for (var n : names) define(n);
    }
    public void define(boolean readonly, FunctionValue val) {
        define(null, val.name, readonly, val);
    }

    public Object get(Context ctx, String name) {
        if (!Values.hasMember(ctx, obj, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        else return Values.getMember(ctx, obj, name);
    }
    public void set(Context ctx, String name, Object val) {
        if (!Values.hasMember(ctx, obj, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        if (!Values.setMember(ctx, obj, name, val)) throw EngineException.ofSyntax("The global '" + name + "' is readonly.");
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
    public GlobalScope(ObjectValue val) {
        this.obj = val;
    }
}
