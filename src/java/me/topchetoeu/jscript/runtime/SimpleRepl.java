package me.topchetoeu.jscript.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import me.topchetoeu.jscript.common.Compiler;
import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class SimpleRepl {
    static Thread engineTask;
    static Engine engine = new Engine();
    static Environment environment = Environment.empty();

    static int j = 0;
    static String[] args;

    private static void reader() {
        try {
            try {
                try { initGlobals(); } catch (ExecutionException e) { throw e.getCause(); }
            }
            catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(e, null)); }

            for (var arg : args) {
                try {
                    var file = Path.of(arg);
                    var raw = Files.readString(file);

                    try {
                        var res = engine.pushMsg(
                            false, environment,
                            Filename.fromFile(file.toFile()), raw, null
                        ).get();

                        System.err.println(res.toReadable(environment));
                    }
                    catch (ExecutionException e) { throw e.getCause(); }
                }
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(e, null)); }
            }

            for (var i = 0; ; i++) {
                try {
                    var raw = Reading.readline();

                    if (raw == null) break;

                    try {
                        var res = engine.pushMsg(
                            false, environment,
                            new Filename("jscript", "repl/" + i + ".js"), raw,
                            Value.UNDEFINED
                        ).get();
                        System.err.println(res.toReadable(environment));
                    }
                    catch (ExecutionException e) { throw e.getCause(); }
                }
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(e, null)); }
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
            engine.thread().interrupt();
        }
        catch (CancellationException | InterruptedException e) { return; }
        catch (Throwable ex) {
            System.out.println("Internal error ocurred:");
            ex.printStackTrace();
        }
    }

    private static ObjectValue symbolPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "makeSymbol", new NativeFunction(args -> new SymbolValue(args.get(0).toString(args.env).value)));
        res.defineOwnMember(env, "getSymbol", new NativeFunction(args -> SymbolValue.get(args.get(0).toString(args.env).value)));
        res.defineOwnMember(env, "getSymbolKey", new NativeFunction(args -> ((SymbolValue)args.get(0)).key()));
        res.defineOwnMember(env, "getSymbolDescriptor", new NativeFunction(args -> new StringValue(((SymbolValue)args.get(0)).value)));

        return res;
    }

    private static ObjectValue numberPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "parseInt", new NativeFunction(args -> {
            var radix = args.get(1).toInt(env);

            if (radix != 10 && args.get(0) instanceof NumberValue) {
                return new NumberValue(args.get(0).toNumber(env).value - args.get(0).toNumber(env).value % 1);
            }
            else {
                return NumberValue.parseInt(args.get(0).toString(), radix, false);
            }
        }));
        res.defineOwnMember(env, "parseFloat", new NativeFunction(args -> {
            if (args.get(0) instanceof NumberValue) {
                return args.get(0);
            }
            else return NumberValue.parseFloat(args.get(0).toString(), false);
        }));
        res.defineOwnMember(env, "isNaN", new NativeFunction(args -> BoolValue.of(args.get(0).isNaN())));
        res.defineOwnMember(env, "NaN", new NumberValue(Double.NaN));
        res.defineOwnMember(env, "Infinity", new NumberValue(Double.POSITIVE_INFINITY));

        return res;
    }

    private static ObjectValue stringPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "stringBuild", new NativeFunction(args -> {
            var parts = ((ArrayValue)args.get(0)).toArray();
            var sb = new StringBuilder();

            for (var i = 0; i < parts.length; i++) {
                sb.append(((StringValue)parts[i]).value);
            }

            return new StringValue(sb.toString());
        }));

        res.defineOwnMember(env, "fromCharCode", new NativeFunction(args -> {
            var parts = ((ArrayValue)args.get(0)).toArray();
            var sb = new StringBuilder();

            for (var i = 0; i < parts.length; i++) {
                sb.append(((StringValue)parts[i]).value);
            }

            return new StringValue(sb.toString());
        }));

        return res;
    }

    private static ObjectValue objectPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "defineField", new NativeFunction(args -> {
            var obj = (ObjectValue)args.get(0);
            var key = args.get(1);
            var writable = args.get(2).toBoolean();
            var enumerable = args.get(3).toBoolean();
            var configurable = args.get(4).toBoolean();
            var value = args.get(5);

            obj.defineOwnMember(args.env, key, FieldMember.of(value, enumerable, configurable, writable));

            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "defineProperty", new NativeFunction(args -> {
            var obj = (ObjectValue)args.get(0);
            var key = args.get(1);
            var enumerable = args.get(2).toBoolean();
            var configurable = args.get(3).toBoolean();
            var getter = args.get(4) instanceof VoidValue ? null : (FunctionValue)args.get(4);
            var setter = args.get(5) instanceof VoidValue ? null : (FunctionValue)args.get(5);

            obj.defineOwnMember(args.env, key, new PropertyMember(getter, setter, configurable, enumerable));

            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "getPrototype", new NativeFunction(args -> {
            return args.get(0).getPrototype(env);
        }));
        res.defineOwnMember(env, "setPrototype", new NativeFunction(args -> {
            var proto = args.get(1) instanceof VoidValue ? null : (ObjectValue)args.get(1);
            args.get(0).setPrototype(env, proto);
            return args.get(0);
        }));
        return res;
    }

    private static ObjectValue functionPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "setCallable", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            func.enableCall = args.get(1).toBoolean();
            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "setConstructable", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            func.enableNew = args.get(1).toBoolean();
            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "invokeType", new NativeFunction(args -> {
            if (((ArgumentsValue)args.get(0)).frame.isNew) return new StringValue("new");
            else return new StringValue("call");
        }));
        res.defineOwnMember(env, "invoke", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            var self = args.get(1);
            var funcArgs = (ArrayValue)args.get(2);

            return func.call(env, self, funcArgs.toArray());
        }));

        return res;
    }

    private static ObjectValue jsonPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "stringify", new NativeFunction(args -> {
            return new StringValue(JSON.stringify(JSONConverter.fromJs(env, args.get(0))));
        }));
        res.defineOwnMember(env, "parse", new NativeFunction(args -> {
            return JSONConverter.toJs(JSON.parse(null, args.get(0).toString(env).value));
        }));
        res.defineOwnMember(env, "setConstructable", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            func.enableNew = args.get(1).toBoolean();
            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "invokeType", new NativeFunction(args -> {
            if (((ArgumentsValue)args.get(0)).frame.isNew) return new StringValue("new");
            else return new StringValue("call");
        }));
        res.defineOwnMember(env, "invoke", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            var self = args.get(1);
            var funcArgs = (ArrayValue)args.get(2);

            return func.call(env, self, funcArgs.toArray());
        }));

        return res;
    }

    private static ObjectValue primordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "symbol", symbolPrimordials(env));
        res.defineOwnMember(env, "number", numberPrimordials(env));
        res.defineOwnMember(env, "string", stringPrimordials(env));
        res.defineOwnMember(env, "object", objectPrimordials(env));
        res.defineOwnMember(env, "function", functionPrimordials(env));
        res.defineOwnMember(env, "json", jsonPrimordials(env));

        int[] i = new int[1];

        res.defineOwnMember(env, "setGlobalPrototype", new NativeFunction(args -> {
            var type = args.get(0).toString(env).value;
            var obj = (ObjectValue)args.get(1);

            switch (type) {
                case "string":
                    args.env.add(Value.STRING_PROTO, obj);
                    break;
                case "number":
                    args.env.add(Value.NUMBER_PROTO, obj);
                    break;
                case "boolean":
                    args.env.add(Value.BOOL_PROTO, obj);
                    break;
                case "symbol":
                    args.env.add(Value.SYMBOL_PROTO, obj);
                    break;
                case "object":
                    args.env.add(Value.OBJECT_PROTO, obj);
                    break;
            }

            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "compile", new NativeFunction(args -> {
            return Compiler.compileFunc(env, new Filename("jscript", "func" + i[0]++ + ".js"), args.get(0).toString(env).value);
        }));
        return res;
    }

    private static void initEnv() {
        // glob.define(null, false, new NativeFunction("go", args -> {
        //     try {
        //         var f = Path.of("do.js");
        //         var func = Compiler.compile(args.env, new Filename("do", "do/" + j++ + ".js"), new String(Files.readAllBytes(f)));
        //         return func.call(args.env);
        //     }
        //     catch (IOException e) {
        //         throw new EngineException("Couldn't open do.js");
        //     }
        // }));

        // var fs = new RootFilesystem(PermissionsProvider.get(environment));
        // fs.protocols.put("temp", new MemoryFilesystem(Mode.READ_WRITE));
        // fs.protocols.put("file", new PhysicalFilesystem("."));
        // fs.protocols.put("std", new STDFilesystem(System.in, System.out, System.err));

        // environment.add(PermissionsProvider.KEY, PermissionsManager.ALL_PERMS);
        // environment.add(Filesystem.KEY, fs);
        // environment.add(ModuleRepo.KEY, ModuleRepo.ofFilesystem(fs));
        // environment.add(Compiler.KEY, new JSCompiler(environment));
        environment.add(EventLoop.KEY, engine);
        environment.add(GlobalScope.KEY, new GlobalScope());
        environment.add(DebugContext.KEY, new DebugContext());
        // environment.add(EventLoop.KEY, engine);
        environment.add(Compiler.KEY, Compiler.DEFAULT);

        var glob = GlobalScope.get(environment);

        glob.define(null, false, new NativeFunction("exit", args -> {
            Thread.currentThread().interrupt();
            throw new InterruptException();
        }));
        glob.define(null, false, new NativeFunction("log", args -> {
            for (var el : args.args) {
                if (el instanceof StringValue) System.out.print(((StringValue)el).value);
                else System.out.print(el.toReadable(args.env));
            }

            return null;
        }));
    }
    private static void initEngine() {
        // var ctx = new DebugContext();
        // environment.add(DebugContext.KEY, ctx);

        // debugServer.targets.put("target", (ws, req) -> new SimpleDebugger(ws).attach(ctx));
        engineTask = engine.start();
        // debugTask = debugServer.start(new InetSocketAddress("127.0.0.1", 9229), true);
    }
    private static void initGlobals() throws InterruptedException, ExecutionException {
        EventLoop.get(environment).pushMsg(
            false, environment,
            Filename.parse("jscript://init.js"), Reading.resourceToString("lib/index.js"),
            Value.UNDEFINED, GlobalScope.get(environment).object, primordials(environment)
        ).get();
    }

    public static void main(String args[]) throws InterruptedException {
        System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

        SimpleRepl.args = args;
        var reader = new Thread(SimpleRepl::reader);

        initEnv();
        initEngine();

        reader.setDaemon(true);
        reader.setName("STD Reader");
        reader.start();

        engine.thread().join();
        // debugTask.interrupt();
        engineTask.interrupt();
    }
}