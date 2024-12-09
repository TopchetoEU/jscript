package me.topchetoeu.jscript.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

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
            catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }

            for (var arg : args) {
                try {
                    var file = new File(arg);
                    var raw = Reading.streamToString(new FileInputStream(file));

                    try {
                        var res = engine.pushMsg(
                            false, environment,
                            Filename.fromFile(file), raw, null
                        ).get();

                        System.err.println(res.toReadable(environment));
                    }
                    catch (ExecutionException e) { throw e.getCause(); }
                }
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }
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
                catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }
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

        res.defineOwnMember(env, "makeSymbol", new NativeFunction(args -> new SymbolValue(args.get(0).toString(args.env))));
        res.defineOwnMember(env, "getSymbol", new NativeFunction(args -> SymbolValue.get(args.get(0).toString(args.env))));
        res.defineOwnMember(env, "getSymbolKey", new NativeFunction(args -> ((SymbolValue)args.get(0)).key()));
        res.defineOwnMember(env, "getSymbolDescriptor", new NativeFunction(args -> StringValue.of(((SymbolValue)args.get(0)).value)));

        return res;
    }

    private static ObjectValue numberPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "parseInt", new NativeFunction(args -> {
            var nradix = args.get(1).toNumber(env);
            var radix = nradix.isInt() ? nradix.getInt() : 10;

            if (radix != 10 && args.get(0) instanceof NumberValue num) {
                if (num.isInt()) return num;
                else return NumberValue.of(num.getDouble() - num.getDouble() % 1);
            }
            else return NumberValue.parseInt(args.get(0).toString(), radix, false);
        }));
        res.defineOwnMember(env, "parseFloat", new NativeFunction(args -> {
            if (args.get(0) instanceof NumberValue) {
                return args.get(0);
            }
            else return NumberValue.parseFloat(args.get(0).toString(), false);
        }));
        res.defineOwnMember(env, "isNaN", new NativeFunction(args -> BoolValue.of(args.get(0).isNaN())));
        res.defineOwnMember(env, "NaN", NumberValue.NAN);
        res.defineOwnMember(env, "Infinity", NumberValue.of(Double.POSITIVE_INFINITY));

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

            return StringValue.of(sb.toString());
        }));

        res.defineOwnMember(env, "fromCharCode", new NativeFunction(args -> {
            var parts = ((ArrayValue)args.get(0)).toArray();
            var sb = new StringBuilder();

            for (var i = 0; i < parts.length; i++) {
                sb.append(((StringValue)parts[i]).value);
            }

            return StringValue.of(sb.toString());
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

            return BoolValue.of(obj.defineOwnMember(args.env, key, FieldMember.of(obj, value, configurable, enumerable, writable)));
        }));
        res.defineOwnMember(env, "defineProperty", new NativeFunction(args -> {
            var obj = (ObjectValue)args.get(0);
            var key = args.get(1);
            var enumerable = args.get(2).toBoolean();
            var configurable = args.get(3).toBoolean();
            var getter = args.get(4) instanceof VoidValue ? null : (FunctionValue)args.get(4);
            var setter = args.get(5) instanceof VoidValue ? null : (FunctionValue)args.get(5);

            return BoolValue.of(obj.defineOwnMember(args.env, key, new PropertyMember(obj, getter, setter, configurable, enumerable)));
        }));
        res.defineOwnMember(env, "getPrototype", new NativeFunction(args -> {
            return args.get(0).getPrototype(env);
        }));
        res.defineOwnMember(env, "setPrototype", new NativeFunction(args -> {
            var proto = args.get(1) instanceof VoidValue ? null : (ObjectValue)args.get(1);
            args.get(0).setPrototype(env, proto);
            return args.get(0);
        }));
        res.defineOwnMember(env, "getOwnMembers", new NativeFunction(args -> {
            var val = new ArrayValue();

            for (var key : args.get(0).getOwnMembers(env, args.get(1).toBoolean())) {
                val.set(args.env, val.size(), StringValue.of(key));
            }

            return val;
        }));
        res.defineOwnMember(env, "getOwnSymbolMembers", new NativeFunction(args -> {
            return ArrayValue.of(args.get(0).getOwnSymbolMembers(env, args.get(1).toBoolean()));
        }));

        return res;
    }

    private static ObjectValue functionPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "setCallable", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            func.enableApply = args.get(1).toBoolean();
            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "setConstructable", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            func.enableConstruct = args.get(1).toBoolean();
            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "invokeType", new NativeFunction(args -> {
            if (((ArgumentsValue)args.get(0)).frame.isNew) return StringValue.of("new");
            else return StringValue.of("call");
        }));

        res.defineOwnMember(env, "invoke", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            var self = args.get(1);
            var funcArgs = (ArrayValue)args.get(2);

            return func.apply(env, self, funcArgs.toArray());
        }));
        res.defineOwnMember(env, "construct", new NativeFunction(args -> {
            var func = (FunctionValue)args.get(0);
            var funcArgs = (ArrayValue)args.get(1);

            return func.construct(env, funcArgs.toArray());
        }));

        return res;
    }

    private static ObjectValue jsonPrimordials(Environment env) {
        var res = new ObjectValue();
        res.setPrototype(null, null);

        res.defineOwnMember(env, "stringify", new NativeFunction(args -> {
            return StringValue.of(JSON.stringify(JSONConverter.fromJs(env, args.get(0))));
        }));
        res.defineOwnMember(env, "parse", new NativeFunction(args -> {
            return JSONConverter.toJs(JSON.parse(null, args.get(0).toString(env)));
        }));

        return res;
    }

	private static void setProto(Environment env, Environment target, Key<ObjectValue> key, ObjectValue repo, String name) {
		var val = repo.getMember(env, name);
		if (val instanceof ObjectValue obj) {
			target.add(key, obj);
		}
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

        res.defineOwnMember(env, "setGlobalPrototypes", new NativeFunction(args -> {
            var obj = (ObjectValue)args.get(0);

			setProto(args.env, env, Value.OBJECT_PROTO, obj, "object");
			setProto(args.env, env, Value.FUNCTION_PROTO, obj, "function");
			setProto(args.env, env, Value.ARRAY_PROTO, obj, "array");
			setProto(args.env, env, Value.BOOL_PROTO, obj, "boolean");
			setProto(args.env, env, Value.NUMBER_PROTO, obj, "number");
			setProto(args.env, env, Value.STRING_PROTO, obj, "string");
			setProto(args.env, env, Value.SYMBOL_PROTO, obj, "symbol");
			setProto(args.env, env, Value.ERROR_PROTO, obj, "error");
			setProto(args.env, env, Value.SYNTAX_ERR_PROTO, obj, "syntax");
			setProto(args.env, env, Value.TYPE_ERR_PROTO, obj, "type");
			setProto(args.env, env, Value.RANGE_ERR_PROTO, obj, "range");

            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "setIntrinsic", new NativeFunction(args -> {
            var name = args.get(0).toString(env);
            var val = args.get(1);

            Value.intrinsics(environment).put(name, val);

            return Value.UNDEFINED;
        }));
        res.defineOwnMember(env, "compile", new NativeFunction(args -> {
            return Compiler.compileFunc(env, new Filename("jscript", "func" + i[0]++ + ".js"), args.get(0).toString(env));
        }));

        return res;
    }

    private static void initEnv() {
        environment.add(EventLoop.KEY, engine);
        environment.add(DebugContext.KEY, new DebugContext());
        environment.add(Compiler.KEY, Compiler.DEFAULT);
        // environment.add(CompileResult.DEBUG_LOG);

        var glob = Value.global(environment);

        glob.defineOwnMember(null, "exit", new NativeFunction("exit", args -> {
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }));
        glob.defineOwnMember(null, "print", new NativeFunction("print", args -> {
            for (var el : args.args) {
                if (el instanceof StringValue) System.out.print(((StringValue)el).value);
                else System.out.print(el.toReadable(args.env));
            }
            System.out.println();

            return Value.UNDEFINED;
        }));
        glob.defineOwnMember(null, "measure", new NativeFunction("measure", args -> {
            var start = System.nanoTime();

            ((FunctionValue)args.get(0)).apply(args.env, Value.UNDEFINED);

            System.out.println(String.format("Finished in %sns", System.nanoTime() - start));

            return Value.UNDEFINED;
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
            Value.UNDEFINED, Value.global(environment), primordials(environment)
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
