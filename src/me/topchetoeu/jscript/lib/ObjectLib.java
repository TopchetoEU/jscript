package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

@Native("Object") public class ObjectLib {
    @Native public static ObjectValue assign(Context ctx, ObjectValue dst, Object... src) {
        for (var obj : src) {
            for (var key : Values.getMembers(ctx, obj, true, true)) {
                Values.setMember(ctx, dst, key, Values.getMember(ctx, obj, key));
            }
        }
        return dst;
    }
    @Native public static ObjectValue create(Context ctx, ObjectValue proto, ObjectValue props) {
        var obj = new ObjectValue();
        obj.setPrototype(ctx, proto);
        return defineProperties(ctx, obj, props);
    }

    @Native public static ObjectValue defineProperty(Context ctx, ObjectValue obj, Object key, ObjectValue attrib) {
        var hasVal = attrib.hasMember(ctx, "value", false);
        var hasGet = attrib.hasMember(ctx, "get", false);
        var hasSet = attrib.hasMember(ctx, "set", false);

        if (hasVal) {
            if (hasGet || hasSet) throw EngineException.ofType("Cannot specify a value and accessors for a property.");
            if (!obj.defineProperty(
                ctx, key,
                attrib.getMember(ctx, "value"),
                Values.toBoolean(attrib.getMember(ctx, "writable")),
                Values.toBoolean(attrib.getMember(ctx, "configurable")),
                Values.toBoolean(attrib.getMember(ctx, "enumerable"))
            )) throw EngineException.ofType("Can't define property '" + key + "'.");
        }
        else {
            var get = attrib.getMember(ctx, "get");
            var set = attrib.getMember(ctx, "set");
            if (get != null && !(get instanceof FunctionValue)) throw EngineException.ofType("Get accessor must be a function.");
            if (set != null && !(set instanceof FunctionValue)) throw EngineException.ofType("Set accessor must be a function.");

            if (!obj.defineProperty(
                ctx, key,
                (FunctionValue)get, (FunctionValue)set,
                Values.toBoolean(attrib.getMember(ctx, "configurable")),
                Values.toBoolean(attrib.getMember(ctx, "enumerable"))
            )) throw EngineException.ofType("Can't define property '" + key + "'.");
        }

        return obj;
    }
    @Native public static ObjectValue defineProperties(Context ctx, ObjectValue obj, ObjectValue attrib) {
        for (var key : Values.getMembers(null, obj, false, false)) {
            obj.defineProperty(ctx, key, attrib.getMember(ctx, key));
        }

        return obj;
    }

    @Native public static ArrayValue keys(Context ctx, Object obj, Object all) {
        var res = new ArrayValue();
        var _all = Values.toBoolean(all);

        for (var key : Values.getMembers(ctx, obj, true, false)) {
            if (_all || !(key instanceof Symbol)) res.set(ctx, res.size(), key);
        }

        return res;
    }
    @Native public static ArrayValue entries(Context ctx, Object obj, Object all) {
        var res = new ArrayValue();
        var _all = Values.toBoolean(all);

        for (var key : Values.getMembers(ctx, obj, true, false)) {
            if (_all || !(key instanceof Symbol)) res.set(ctx, res.size(), new ArrayValue(ctx, key, Values.getMember(ctx, obj, key)));
        }

        return res;
    }
    @Native public static ArrayValue values(Context ctx, Object obj, Object all) {
        var res = new ArrayValue();
        var _all = Values.toBoolean(all);

        for (var key : Values.getMembers(ctx, obj, true, false)) {
            if (_all || key instanceof String) res.set(ctx, res.size(), Values.getMember(ctx, obj, key));
        }

        return res;
    }

    @Native public static ObjectValue getOwnPropertyDescriptor(Context ctx, Object obj, Object key) {
        return Values.getMemberDescriptor(ctx, obj, key);
    }
    @Native public static ObjectValue getOwnPropertyDescriptors(Context ctx, Object obj) {
        var res = new ObjectValue();
        for (var key : Values.getMembers(ctx, obj, true, true)) {
            res.defineProperty(ctx, key, getOwnPropertyDescriptor(ctx, obj, key));
        }
        return res;
    }

    @Native public static ArrayValue getOwnPropertyNames(Context ctx, Object obj, Object all) {
        var res = new ArrayValue();
        var _all = Values.toBoolean(all);

        for (var key : Values.getMembers(ctx, obj, true, true)) {
            if (_all || !(key instanceof Symbol)) res.set(ctx, res.size(), key);
        }

        return res;
    }
    @Native public static ArrayValue getOwnPropertySymbols(Context ctx, Object obj) {
        var res = new ArrayValue();

        for (var key : Values.getMembers(ctx, obj, true, true)) {
            if (key instanceof Symbol) res.set(ctx, res.size(), key);
        }

        return res;
    }
    @Native public static boolean hasOwn(Context ctx, Object obj, Object key) {
        return Values.hasMember(ctx, obj, key, true);
    }

    @Native public static ObjectValue getPrototypeOf(Context ctx, Object obj) {
        return Values.getPrototype(ctx, obj);
    }
    @Native public static Object setPrototypeOf(Context ctx, Object obj, Object proto) {
        Values.setPrototype(ctx, obj, proto);
        return obj;
    }

    @Native public static ObjectValue fromEntries(Context ctx, Object iterable) {
        var res = new ObjectValue();

        for (var el : Values.fromJSIterator(ctx, iterable)) {
            if (el instanceof ArrayValue) {
                res.defineProperty(ctx, ((ArrayValue)el).get(0), ((ArrayValue)el).get(1));
            }
        }

        return res;
    }

    @Native public static Object preventExtensions(Context ctx, Object obj) {
        if (obj instanceof ObjectValue) ((ObjectValue)obj).preventExtensions();
        return obj;
    }
    @Native public static Object seal(Context ctx, Object obj) {
        if (obj instanceof ObjectValue) ((ObjectValue)obj).seal();
        return obj;
    }
    @Native public static Object freeze(Context ctx, Object obj) {
        if (obj instanceof ObjectValue) ((ObjectValue)obj).freeze();
        return obj;
    }

    @Native public static boolean isExtensible(Context ctx, Object obj) {
        return obj instanceof ObjectValue && ((ObjectValue)obj).extensible();
    }
    @Native public static boolean isSealed(Context ctx, Object obj) {
        if (obj instanceof ObjectValue && ((ObjectValue)obj).extensible()) {
            var _obj = (ObjectValue)obj;
            for (var key : _obj.keys(true)) {
                if (_obj.memberConfigurable(key)) return false;
            }
        }

        return true;
    }
    @Native public static boolean isFrozen(Context ctx, Object obj) {
        if (obj instanceof ObjectValue && ((ObjectValue)obj).extensible()) {
            var _obj = (ObjectValue)obj;
            for (var key : _obj.keys(true)) {
                if (_obj.memberConfigurable(key)) return false;
                if (_obj.memberWritable(key)) return false;
            }
        }

        return true;
    }

    @Native(thisArg = true) public static Object valueOf(Context ctx, Object thisArg) {
        return thisArg;
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        var name = Values.getMember(ctx, thisArg, ctx.environment().symbol("Symbol.typeName"));
        if (name == null) name = "Unknown";
        else name = Values.toString(ctx, name);

        return "[object " + name + "]";
    }
    @Native(thisArg = true) public static boolean hasOwnProperty(Context ctx, Object thisArg, Object key) {
        return ObjectLib.hasOwn(ctx, thisArg, Values.convert(ctx, key, String.class));
    }

    @NativeConstructor(thisArg = true) public static Object constructor(Context ctx, Object thisArg, Object arg) {
        if (arg == null || arg == Values.NULL) return new ObjectValue();
        else if (arg instanceof Boolean) return BooleanLib.constructor(ctx, thisArg, arg);
        else if (arg instanceof Number) return NumberLib.constructor(ctx, thisArg, arg);
        else if (arg instanceof String) return StringLib.constructor(ctx, thisArg, arg);
        // else if (arg instanceof Symbol) return SymbolPolyfill.constructor(ctx, thisArg, arg);
        else return arg;
    }
}
