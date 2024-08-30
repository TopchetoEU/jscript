package me.topchetoeu.jscript.runtime.scope;

import java.util.HashSet;
import java.util.Set;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class GlobalScope {
    public static final Key<GlobalScope> KEY = new Key<>();

    public final ObjectValue object;

    public boolean has(Environment ext, String name) {
        return object.hasMember(ext, new StringValue(name), false);
    }

    public GlobalScope child() {
        var res = new GlobalScope();
        res.object.setPrototype(null, this.object);
        return res;
    }

    public void define(Environment ext, String name, Variable variable) {
        object.defineOwnMember(ext, name, variable.toField(true, true));
    }
    public void define(Environment ext, boolean readonly, String name, Value val) {
        object.defineOwnMember(ext, name, FieldMember.of(val, !readonly));
    }
    public void define(Environment ext, boolean readonly, String ...names) {
        for (var name : names) define(ext, name, new ValueVariable(readonly, VoidValue.UNDEFINED));
    }
    public void define(Environment ext, boolean readonly, FunctionValue val) {
        define(ext, readonly, val.name, val);
    }

    public Value get(Environment env, String name) {
        if (!object.hasMember(env, name, false)) throw EngineException.ofSyntax(name + " is not defined");
        else return object.getMember(env, name);
    }
    public void set(Environment ext, String name, Value val) {
        if (!object.hasMember(ext, name, false)) throw EngineException.ofSyntax(name + " is not defined");
        if (!object.setMember(ext, name, val)) throw EngineException.ofSyntax("Assignment to constant variable");
    }

    public Set<String> keys() {
        var res = new HashSet<String>();

        for (var key : keys()) {
            if (key instanceof String) res.add((String)key);
        }

        return res;
    }

    public GlobalScope() {
        this.object = new ObjectValue();
        this.object.setPrototype(null, null);
    }
    public GlobalScope(ObjectValue val) {
        this.object = val;
    }

    public static GlobalScope get(Environment ext) {
        if (ext.has(KEY)) return ext.get(KEY);
        else return new GlobalScope();
    }
}
