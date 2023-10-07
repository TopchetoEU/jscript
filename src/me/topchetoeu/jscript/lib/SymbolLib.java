package me.topchetoeu.jscript.lib;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeInit;

public class SymbolLib {
    private static final Map<String, Symbol> symbols = new HashMap<>();

    @NativeGetter public static Symbol typeName(Context ctx) { return ctx.environment().symbol("Symbol.typeName"); }
    @NativeGetter public static Symbol replace(Context ctx) { return ctx.environment().symbol("Symbol.replace"); }
    @NativeGetter public static Symbol match(Context ctx) { return ctx.environment().symbol("Symbol.match"); }
    @NativeGetter public static Symbol matchAll(Context ctx) { return ctx.environment().symbol("Symbol.matchAll"); }
    @NativeGetter public static Symbol split(Context ctx) { return ctx.environment().symbol("Symbol.split"); }
    @NativeGetter public static Symbol search(Context ctx) { return ctx.environment().symbol("Symbol.search"); }
    @NativeGetter public static Symbol iterator(Context ctx) { return ctx.environment().symbol("Symbol.iterator"); }
    @NativeGetter public static Symbol asyncIterator(Context ctx) { return ctx.environment().symbol("Symbol.asyncIterator"); }

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

    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, env.symbol("Symbol.typeName"), "Symbol");
    }
}
