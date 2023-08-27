package me.topchetoeu.jscript.polyfills;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.modules.ModuleManager;

public class PolyfillEngine extends Engine {
    public static String streamToString(InputStream in) {
        try {
            StringBuilder out = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            for(var line = br.readLine(); line != null; line = br.readLine()) {
                out.append(line).append('\n');
            }

            br.close();
            return out.toString();
        }
        catch (IOException e) {
            return null;
        }
    }
    public static String resourceToString(String name) {
        var str = PolyfillEngine.class.getResourceAsStream("/me/topchetoeu/jscript/" + name);
        if (str == null) return null;
        return streamToString(str);
    }

    public final ModuleManager modules;

    @Override
    public Object makeRegex(String pattern, String flags) {
        return new RegExp(pattern, flags);
    }

    @Override
    public ModuleManager modules() {
        return modules;
    }
    public PolyfillEngine(File root) {
        super();

        this.modules = new ModuleManager(root);

        // exposeNamespace("Math", Math.class);
        // exposeNamespace("JSON", JSON.class);
        // exposeClass("Promise", Promise.class);
        // exposeClass("RegExp", RegExp.class);
        // exposeClass("Date", Date.class);
        // exposeClass("Map", Map.class);
        // exposeClass("Set", Set.class);

        // global().define("Object", "Function", "String", "Number", "Boolean", "Symbol");
        // global().define("Array", "require");
        // global().define("Error", "SyntaxError", "TypeError", "RangeError");
        // global().define("setTimeout", "setInterval", "clearTimeout", "clearInterval");
        // global().define("debugger");

        // global().define(true, new NativeFunction("measure", (ctx, thisArg, values) -> {
        //     var start = System.nanoTime();
        //     try {
        //         return Values.call(ctx, values[0], ctx);
        //     }
        //     finally {
        //         System.out.println(String.format("Function took %s ms", (System.nanoTime() - start) / 1000000.));
        //     }
        // }));
        // global().define(true, new NativeFunction("isNaN", (ctx, thisArg, args) -> {
        //     if (args.length == 0) return true;
        //     else return Double.isNaN(Values.toNumber(ctx, args[0]));
        // }));
        // global().define(true, new NativeFunction("log", (el, t, args) -> {
        //     for (var obj : args) Values.printValue(el, obj);
        //     System.out.println();
        //     return null;
        // }));

        // var scope = global().globalChild();
        // scope.define("gt", true, global().obj);
        // scope.define("lgt", true, scope.obj);
        // scope.define("setProps", "setConstr");
        // scope.define("internals", true, new Internals());
        // scope.define(true, new NativeFunction("run", (ctx, thisArg, args) -> {
        //     var filename = (String)args[0];
        //     boolean pollute = args.length > 1 && args[1].equals(true);
        //     FunctionValue func;
        //     var src = resourceToString("js/" + filename);
        //     if (src == null) throw new RuntimeException("The source '" + filename + "' doesn't exist.");

        //     if (pollute) func = Parsing.compile(global(), filename, src);
        //     else func = Parsing.compile(scope.globalChild(), filename, src);

        //     func.call(ctx);
        //     return null;
        // }));

        // pushMsg(false, scope.globalChild(), java.util.Map.of(), "core.js", resourceToString("js/core.js"), null);
    }
}
