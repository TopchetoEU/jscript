package me.topchetoeu.jscript.polyfills;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.Values;

public class TypescriptEngine extends PolyfillEngine {
    private FunctionValue ts;

    @Override
    public FunctionValue compile(GlobalScope scope, String filename, String raw) throws InterruptedException {
        if (ts != null) {
            var res = Values.array(ts.call(context(), null, filename, raw));
            var src = Values.toString(context(), res.get(0));
            var func = Values.function(res.get(1));

            var compiled = super.compile(scope, filename, src);

            return new NativeFunction(null, (ctx, thisArg, args) -> {
                return func.call(context(), null, compiled, thisArg, new ArrayValue(args));
            });
        }
        return super.compile(scope, filename, raw);
    }

    public TypescriptEngine(File root) {
        super(root);
        var scope = global().globalChild();

        var decls = new ArrayList<Object>();
        decls.add(resourceToString("dts/core.d.ts"));
        decls.add(resourceToString("dts/iterators.d.ts"));
        decls.add(resourceToString("dts/map.d.ts"));
        decls.add(resourceToString("dts/promise.d.ts"));
        decls.add(resourceToString("dts/regex.d.ts"));
        decls.add(resourceToString("dts/require.d.ts"));
        decls.add(resourceToString("dts/set.d.ts"));
        decls.add(resourceToString("dts/values/array.d.ts"));
        decls.add(resourceToString("dts/values/boolean.d.ts"));
        decls.add(resourceToString("dts/values/number.d.ts"));
        decls.add(resourceToString("dts/values/errors.d.ts"));
        decls.add(resourceToString("dts/values/function.d.ts"));
        decls.add(resourceToString("dts/values/object.d.ts"));
        decls.add(resourceToString("dts/values/string.d.ts"));
        decls.add(resourceToString("dts/values/symbol.d.ts"));

        scope.define("libs", true, ArrayValue.of(decls));
        scope.define(true, new NativeFunction("init", (el, t, args) -> {
            ts = Values.function(args[0]);
            return null;
        }));

        pushMsg(false, scope, Map.of(), "bootstrap.js", resourceToString("js/bootstrap.js"), null);
    }
}
