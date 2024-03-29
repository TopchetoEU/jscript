package me.topchetoeu.jscript.runtime.scope;

import java.util.HashSet;
import java.util.Set;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;

public class GlobalScope {
    public final ObjectValue obj;

    public boolean has(Context ctx, String name) {
        return Values.hasMember(null, obj, name, false);
    }

    public GlobalScope globalChild() {
        var obj = new ObjectValue();
        Values.setPrototype(null, obj, this.obj);
        return new GlobalScope(obj);
    }

    public Object define(Context ctx, String name) {
        if (Values.hasMember(ctx, obj, name, false)) return name;
        obj.defineProperty(ctx, name, null);
        return name;
    }
    public void define(Context ctx, String name, Variable val) {
        obj.defineProperty(ctx, name,
            new NativeFunction("get " + name, args -> val.get(args.ctx)),
            new NativeFunction("set " + name, args -> { val.set(args.ctx, args.get(0)); return null; }),
            true, true
        );
    }
    public void define(Context ctx, String name, boolean readonly, Object val) {
        obj.defineProperty(ctx, name, val, readonly, true, true);
    }
    public void define(Context ctx, String ...names) {
        for (var n : names) define(ctx, n);
    }
    public void define(Context ctx, boolean readonly, FunctionValue val) {
        define(ctx, val.name, readonly, val);
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
