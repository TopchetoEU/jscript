package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Object")
public class ObjectLib {
    @Expose(target = ExposeTarget.STATIC)
    public static Object __assign(Arguments args) {
        for (var obj : args.slice(1).args) {
            for (var key : Values.getMembers(args.env, obj, true, true)) {
                Values.setMember(args.env, args.get(0), key, Values.getMember(args.env, obj, key));
            }
        }
        return args.get(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __create(Arguments args) {
        var obj = new ObjectValue();
        Values.setPrototype(args.env, obj, args.get(0));

        if (args.n() >= 1) {
            var newArgs = new Object[args.n()];
            System.arraycopy(args.args, 1, args, 1, args.n() - 1);
            newArgs[0] = obj;

            __defineProperties(new Arguments(args.env, null, newArgs));
        }

        return obj;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __defineProperty(Arguments args) {
        var obj = args.convert(0, ObjectValue.class);
        var key = args.get(1);
        var attrib = args.convert(2, ObjectValue.class);

        var hasVal = Values.hasMember(args.env, attrib, "value", false);
        var hasGet = Values.hasMember(args.env, attrib, "get", false);
        var hasSet = Values.hasMember(args.env, attrib, "set", false);

        if (hasVal) {
            if (hasGet || hasSet) throw EngineException.ofType("Cannot specify a value and accessors for a property.");
            if (!obj.defineProperty(
                args.env, key,
                Values.getMember(args.env, attrib, "value"),
                Values.toBoolean(Values.getMember(args.env, attrib, "writable")),
                Values.toBoolean(Values.getMember(args.env, attrib, "configurable")),
                Values.toBoolean(Values.getMember(args.env, attrib, "enumerable"))
            )) throw EngineException.ofType("Can't define property '" + key + "'.");
        }
        else {
            var get = Values.getMember(args.env, attrib, "get");
            var set = Values.getMember(args.env, attrib, "set");
            if (get != null && !(get instanceof FunctionValue)) throw EngineException.ofType("Get accessor must be a function.");
            if (set != null && !(set instanceof FunctionValue)) throw EngineException.ofType("Set accessor must be a function.");

            if (!obj.defineProperty(
                args.env, key,
                (FunctionValue)get, (FunctionValue)set,
                Values.toBoolean(Values.getMember(args.env, attrib, "configurable")),
                Values.toBoolean(Values.getMember(args.env, attrib, "enumerable"))
            )) throw EngineException.ofType("Can't define property '" + key + "'.");
        }

        return obj;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __defineProperties(Arguments args) {
        var obj = args.convert(0, ObjectValue.class);
        var attrib = args.get(1);

        for (var key : Values.getMembers(null, attrib, false, false)) {
            __defineProperty(new Arguments(args.env, null, obj, key, Values.getMember(args.env, attrib, key)));
        }

        return obj;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __keys(Arguments args) {
        var obj = args.get(0);
        var all = args.getBoolean(1);
        var res = new ArrayValue();

        for (var key : Values.getMembers(args.env, obj, true, false)) {
            if (all || !(key instanceof Symbol)) res.set(args.env, res.size(), key);
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __entries(Arguments args) {
        var res = new ArrayValue();
        var obj = args.get(0);
        var all = args.getBoolean(1);

        for (var key : Values.getMembers(args.env, obj, true, false)) {
            if (all || !(key instanceof Symbol)) res.set(args.env, res.size(), new ArrayValue(args.env, key, Values.getMember(args.env, obj, key)));
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __values(Arguments args) {
        var res = new ArrayValue();
        var obj = args.get(0);
        var all = args.getBoolean(1);

        for (var key : Values.getMembers(args.env, obj, true, false)) {
            if (all || !(key instanceof Symbol)) res.set(args.env, res.size(), Values.getMember(args.env, obj, key));
        }

        return res;
    }
 
    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __getOwnPropertyDescriptor(Arguments args) {
        return Values.getMemberDescriptor(args.env, args.get(0), args.get(1));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __getOwnPropertyDescriptors(Arguments args) {
        var res = new ObjectValue();
        var obj = args.get(0);
        for (var key : Values.getMembers(args.env, obj, true, true)) {
            res.defineProperty(args.env, key, Values.getMemberDescriptor(args.env, obj, key));
        }
        return res;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __getOwnPropertyNames(Arguments args) {
        var res = new ArrayValue();
        var obj = args.get(0);
        var all = args.getBoolean(1);

        for (var key : Values.getMembers(args.env, obj, true, true)) {
            if (all || !(key instanceof Symbol)) res.set(args.env, res.size(), key);
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __getOwnPropertySymbols(Arguments args) {
        var obj = args.get(0);
        var res = new ArrayValue();

        for (var key : Values.getMembers(args.env, obj, true, true)) {
            if (key instanceof Symbol) res.set(args.env, res.size(), key);
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __hasOwn(Arguments args) {
        return Values.hasMember(args.env, args.get(0), args.get(1), true);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __getPrototypeOf(Arguments args) {
        return Values.getPrototype(args.env, args.get(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static Object __setPrototypeOf(Arguments args) {
        Values.setPrototype(args.env, args.get(0), args.get(1));
        return args.get(0);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __fromEntries(Arguments args) {
        var res = new ObjectValue();

        for (var el : Values.fromJSIterator(args.env, args.get(0))) {
            if (el instanceof ArrayValue) {
                res.defineProperty(args.env, ((ArrayValue)el).get(0), ((ArrayValue)el).get(1));
            }
        }

        return res;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static Object __preventExtensions(Arguments args) {
        if (args.get(0) instanceof ObjectValue) args.convert(0, ObjectValue.class).preventExtensions();
        return args.get(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static Object __seal(Arguments args) {
        if (args.get(0) instanceof ObjectValue) args.convert(0, ObjectValue.class).seal();
        return args.get(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static Object __freeze(Arguments args) {
        if (args.get(0) instanceof ObjectValue) args.convert(0, ObjectValue.class).freeze();
        return args.get(0);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isExtensible(Arguments args) {
        var obj = args.get(0);
        if (!(obj instanceof ObjectValue)) return false;
        return ((ObjectValue)obj).extensible();
    }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isSealed(Arguments args) {
        var obj = args.get(0);

        if (!(obj instanceof ObjectValue)) return true;
        var _obj = (ObjectValue)obj;

        if (_obj.extensible()) return false;

        for (var key : _obj.keys(true)) {
            if (_obj.memberConfigurable(key)) return false;
        }

        return true;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isFrozen(Arguments args) {
        var obj = args.get(0);

        if (!(obj instanceof ObjectValue)) return true;
        var _obj = (ObjectValue)obj;

        if (_obj.extensible()) return false;

        for (var key : _obj.keys(true)) {
            if (_obj.memberConfigurable(key)) return false;
            if (_obj.memberWritable(key)) return false;
        }

        return true;
    }

    @Expose
    public static Object __valueOf(Arguments args) {
        return args.self;
    }
    @Expose
    public static String __toString(Arguments args) {
        var name = Values.getMember(args.env, args.self, Symbol.get("Symbol.typeName"));
        if (name == null) name = "Unknown";
        else name = Values.toString(args.env, name);

        return "[object " + name + "]";
    }
    @Expose
    public static boolean __hasOwnProperty(Arguments args) {
        return Values.hasMember(args.env, args.self, args.get(0), true);
    }

    @ExposeConstructor
    public static Object __constructor(Arguments args) {
        var arg = args.get(0);
        if (arg == null || arg == Values.NULL) return new ObjectValue();
        else if (arg instanceof Boolean) return new BooleanLib((boolean)arg);
        else if (arg instanceof Number) return new NumberLib(((Number)arg).doubleValue());
        else if (arg instanceof String) return new StringLib((String)arg);
        else if (arg instanceof Symbol) return new SymbolLib((Symbol)arg);
        else return arg;
    }
}
