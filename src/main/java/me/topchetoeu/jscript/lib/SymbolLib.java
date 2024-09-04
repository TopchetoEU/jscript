package me.topchetoeu.jscript.lib;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Symbol")
public class SymbolLib {
    private static final Map<String, Symbol> symbols = new HashMap<>();

    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __typeName = Symbol.get("Symbol.typeName");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __replace = Symbol.get("Symbol.replace");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __match = Symbol.get("Symbol.match");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __matchAll = Symbol.get("Symbol.matchAll");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __split = Symbol.get("Symbol.split");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __search = Symbol.get("Symbol.search");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __iterator = Symbol.get("Symbol.iterator");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __asyncIterator = Symbol.get("Symbol.asyncIterator");
    @ExposeField(target = ExposeTarget.STATIC)
    public static final Symbol __cause = Symbol.get("Symbol.cause");

    public final Symbol value;

    private static Symbol passThis(Arguments args, String funcName) {
        var val = args.self;
        if (Values.isWrapper(val, SymbolLib.class)) return Values.wrapper(val, SymbolLib.class).value;
        else if (val instanceof Symbol) return (Symbol)val;
        else throw EngineException.ofType(String.format("'%s' may only be called upon object and primitve symbols.", funcName));
    }

    public SymbolLib(Symbol val) {
        this.value = val;
    }

    @Expose public static String __toString(Arguments args) {
        return passThis(args, "toString").value;
    }
    @Expose public static Symbol __valueOf(Arguments args) {
        return passThis(args, "valueOf");
    }

    @ExposeConstructor
    public static Object __constructor(Arguments args) {
        if (args.self instanceof ObjectValue) throw EngineException.ofType("Symbol constructor may not be called with new.");
        if (args.get(0) == null) return new Symbol("");
        else return new Symbol(args.getString(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static Symbol __for(Arguments args) {
        var key = args.getString(0);
        if (symbols.containsKey(key)) return symbols.get(key);
        else {
            var sym = new Symbol(key);
            symbols.put(key, sym);
            return sym;
        }
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __keyFor(Arguments args) {
        return passThis(new Arguments(args.ctx, args.get(0)), "keyFor").value;
    }
}
