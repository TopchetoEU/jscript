package me.topchetoeu.jscript.lib;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

@Native("Symbol") public class SymbolLib {
    private static final Map<String, Symbol> symbols = new HashMap<>();

    @Native public static final Symbol typeName = Symbol.get("Symbol.typeName");
    @Native public static final Symbol replace = Symbol.get("Symbol.replace");
    @Native public static final Symbol match = Symbol.get("Symbol.match");
    @Native public static final Symbol matchAll = Symbol.get("Symbol.matchAll");
    @Native public static final Symbol split = Symbol.get("Symbol.split");
    @Native public static final Symbol search = Symbol.get("Symbol.search");
    @Native public static final Symbol iterator = Symbol.get("Symbol.iterator");
    @Native public static final Symbol asyncIterator = Symbol.get("Symbol.asyncIterator");
    @Native public static final Symbol cause = Symbol.get("Symbol.cause");

    public final Symbol value;

    private static Symbol passThis(Context ctx, String funcName, Object val) {
        if (val instanceof SymbolLib) return ((SymbolLib)val).value;
        else if (val instanceof Symbol) return (Symbol)val;
        else throw EngineException.ofType(String.format("'%s' may only be called upon object and primitve symbols.", funcName));
    }

    @NativeConstructor(thisArg = true) public static Object constructor(Context ctx, Object thisArg, Object val) {
        if (thisArg instanceof ObjectValue) throw EngineException.ofType("Symbol constructor may not be called with new.");
        if (val == null) return new Symbol("");
        else return new Symbol(Values.toString(ctx, val));
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        return passThis(ctx, "toString", thisArg).value;
    }
    @Native(thisArg = true) public static Symbol valueOf(Context ctx, Object thisArg) {
        return passThis(ctx, "valueOf", thisArg);
    }

    @Native("for") public static Symbol _for(String key) {
        if (symbols.containsKey(key)) return symbols.get(key);
        else {
            var sym = new Symbol(key);
            symbols.put(key, sym);
            return sym;
        }
    }
    @Native public static String keyFor(Symbol sym) {
        return sym.value;
    }

    public SymbolLib(Symbol val) {
        this.value = val;
    }
}
