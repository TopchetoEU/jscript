package me.topchetoeu.jscript.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import me.topchetoeu.jscript.common.Metadata;
import me.topchetoeu.jscript.common.Reading;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.parsing.Filename;
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
import me.topchetoeu.jscript.runtime.values.primitives.UserValue;
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

			System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

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

	@SuppressWarnings("unchecked")
	private static ObjectValue mapPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		var prototype = new ObjectValue[1];
		NativeFunction mapConstr = new NativeFunction(args -> {
			return UserValue.of(new LinkedHashMap<>(), prototype[0]);
		});
		mapConstr.prototype.defineOwnMember(env, "get", new NativeFunction(getArgs -> {
			var map = getArgs.self(LinkedHashMap.class);
			var key = getArgs.get(0);
			var val = map.get(key);
			return val == null ? Value.UNDEFINED : (Value)val;
		}));
		mapConstr.prototype.defineOwnMember(env, "set", new NativeFunction(getArgs -> {
			var map = getArgs.self(LinkedHashMap.class);
			var key = getArgs.get(0);
			var val = getArgs.get(1);
			map.put(key, val);

			return Value.UNDEFINED;
		}));
		mapConstr.prototype.defineOwnMember(env, "has", new NativeFunction(getArgs -> {
			var map = getArgs.self(LinkedHashMap.class);
			var key = getArgs.get(0);
			return BoolValue.of(map.containsKey(key));
		}));
		mapConstr.prototype.defineOwnMember(env, "delete", new NativeFunction(getArgs -> {
			var map = getArgs.self(LinkedHashMap.class);
			var key = getArgs.get(0);
			map.remove(key);
			return Value.UNDEFINED;
		}));
		mapConstr.prototype.defineOwnMember(env, "keys", new NativeFunction(getArgs -> {
			var map = getArgs.self(LinkedHashMap.class);
			return ArrayValue.of(map.keySet());
		}));
		prototype[0] = (ObjectValue)mapConstr.prototype;

		return mapConstr;
	}

	private static ObjectValue regexPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		var prototype = new ObjectValue[1];
		NativeFunction mapConstr = new NativeFunction(args -> {
			var pattern = Pattern.compile(args.get(0).toString(args.env));
			return UserValue.of(pattern, prototype[0]);
		});
		mapConstr.prototype.defineOwnMember(env, "exec", new NativeFunction(args -> {
			var pattern = args.self(Pattern.class);
			var target = args.get(0).toString(args.env);
			var offset = args.get(1).toNumber(args.env).getInt();
			var index = args.get(2).toBoolean();

			var matcher = pattern.matcher(target).region(offset, target.length());

			var obj = new ArrayValue();
			for (var i = 0; i < matcher.groupCount(); i++) {
				obj.set(args.env, i, StringValue.of(matcher.group(i)));
			}

			obj.defineOwnMember(args.env, "index", NumberValue.of(matcher.start()));
			obj.defineOwnMember(args.env, "input", StringValue.of(target));
			if (index) {
				var indices = new ArrayValue();
				indices.setPrototype(args.env, null);
				for (var i = 0; i < matcher.groupCount(); i++) {
					obj.set(args.env, i, ArrayValue.of(Arrays.asList(
						NumberValue.of(matcher.start(i)),
						NumberValue.of(matcher.end(i))
					)));
				}

			}


			return Value.UNDEFINED;
			// return val == null ? Value.UNDEFINED : (Value)val;
		}));
		prototype[0] = (ObjectValue)mapConstr.prototype;

		return mapConstr;
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
		res.defineOwnMember(env, "isArray", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof ArrayValue);
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
		res.defineOwnMember(env, "invokeTypeInfer", new NativeFunction(args -> {
			var frame = Frame.get(args.env, args.get(0).toNumber(args.env).getInt());
			if (frame.isNew) return StringValue.of("new");
			else return StringValue.of("call");
		}));
		res.defineOwnMember(env, "target", new NativeFunction(args -> {
			var frame = Frame.get(args.env, args.get(0).toNumber(args.env).getInt());
			if (frame.target == null) return Value.UNDEFINED;
			else return frame.target;
		}));

		res.defineOwnMember(env, "invoke", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			var self = args.get(1);
			var funcArgs = (ArrayValue)args.get(2);

			return func.apply(env, self, funcArgs.toArray());
		}));
		res.defineOwnMember(env, "construct", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			var target = args.get(1);
			var funcArgs = (ArrayValue)args.get(2);

			if (target == Value.UNDEFINED) return func.constructNoSelf(env, funcArgs.toArray());
			else return func.construct(env, target, funcArgs.toArray());
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
		res.defineOwnMember(env, "map", mapPrimordials(env));
		res.defineOwnMember(env, "regex", regexPrimordials(env));

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
			var val = obj.getMember(args.env, "regex");
			if (val instanceof FunctionValue func) {
				env.add(Value.REGEX_CONSTR, func);
			}
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

	private static Environment createESEnv() throws InterruptedException, ExecutionException {
		var env = initEnv();
		var stubEnv = initEnv();
		Value.global(stubEnv).defineOwnMember(stubEnv, "target", Value.global(env));
		Value.global(stubEnv).defineOwnMember(stubEnv, "primordials", primordials(env));

		EventLoop.get(stubEnv).pushMsg(
			false, stubEnv,
			Filename.parse("jscript://init.js"), Reading.resourceToString("lib/index.js"),
			Value.UNDEFINED
		).get();

		return env;
	}

	private static Environment initEnv() {
		var env = new Environment();
		env.add(EventLoop.KEY, engine);
		env.add(DebugContext.KEY, new DebugContext());
		env.add(Compiler.KEY, Compiler.DEFAULT);
		// environment.add(CompileResult.DEBUG_LOG);

		var glob = Value.global(env);

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

		return env;
	}
	private static void initEngine() {
		engineTask = engine.start();
	}
	private static void initGlobals() throws InterruptedException, ExecutionException {
		environment = createESEnv();
		var tsEnv = createESEnv();
		var res = new FunctionValue[1];
		var setter = new NativeFunction(args -> {
			res[0] = (FunctionValue)args.get(0);
			return Value.UNDEFINED;
		});

		var ts = Reading.resourceToString("lib/ts.js");
		if (ts != null) EventLoop.get(tsEnv).pushMsg(
			false, tsEnv,
			Filename.parse("jscript://ts.js"), ts,
			Value.UNDEFINED, setter
		).get();
	}

	public static void main(String args[]) throws InterruptedException {
		SimpleRepl.args = args;
		var reader = new Thread(SimpleRepl::reader);

		environment = initEnv();
		initEngine();

		reader.setDaemon(true);
		reader.setName("STD Reader");
		reader.start();

		engine.thread().join();
		engineTask.interrupt();
	}
}
