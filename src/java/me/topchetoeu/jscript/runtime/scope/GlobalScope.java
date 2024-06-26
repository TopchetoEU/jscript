package me.topchetoeu.jscript.runtime.scope;

import java.util.HashSet;
import java.util.Set;

import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;

public class GlobalScope {
    public static final Key<GlobalScope> KEY = new Key<>();

    public final ObjectValue obj;

    public boolean has(Extensions ext, String name) {
        return Values.hasMember(ext, obj, name, false);
    }

    public GlobalScope child() {
        var obj = new ObjectValue();
        Values.setPrototype(null, obj, this.obj);
        return new GlobalScope(obj);
    }

    public Object define(Extensions ext, String name) {
        if (Values.hasMember(ext, obj, name, false)) return name;
        obj.defineProperty(ext, name, null);
        return name;
    }
    public void define(Extensions ext, String name, Variable val) {
        obj.defineProperty(ext, name,
            new NativeFunction("get " + name, args -> val.get(args.ctx)),
            new NativeFunction("set " + name, args -> { val.set(args.ctx, args.get(0)); return null; }),
            true, true
        );
    }
    public void define(Extensions ext, String name, boolean readonly, Object val) {
        obj.defineProperty(ext, name, val, readonly, true, true);
    }
    public void define(Extensions ext, String ...names) {
        for (var n : names) define(ext, n);
    }
    public void define(Extensions ext, boolean readonly, FunctionValue val) {
        define(ext, val.name, readonly, val);
    }

    public Object get(Extensions ext, String name) {
        if (!Values.hasMember(ext, obj, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        else return Values.getMember(ext, obj, name);
    }
    public void set(Extensions ext, String name, Object val) {
        if (!Values.hasMember(ext, obj, name, false)) throw EngineException.ofSyntax("The variable '" + name + "' doesn't exist.");
        if (!Values.setMember(ext, obj, name, val)) throw EngineException.ofSyntax("The global '" + name + "' is readonly.");
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

    public static GlobalScope get(Extensions ext) {
        if (ext.has(KEY)) return ext.get(KEY);
        else return new GlobalScope();
    }
}
