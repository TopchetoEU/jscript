package me.topchetoeu.j2s.repl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Metadata;
import me.topchetoeu.j2s.common.Reading;
import me.topchetoeu.j2s.common.SyntaxException;
import me.topchetoeu.j2s.common.environment.Environment;
import me.topchetoeu.j2s.common.environment.Key;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.json.JSON;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;
import me.topchetoeu.j2s.repl.debug.DebugServer;
import me.topchetoeu.j2s.repl.debug.Debugger;
import me.topchetoeu.j2s.repl.debug.SimpleDebugger;
import me.topchetoeu.j2s.repl.mapping.NativeMapper;
import me.topchetoeu.j2s.runtime.ArgumentsValue;
import me.topchetoeu.j2s.runtime.Compiler;
import me.topchetoeu.j2s.runtime.Engine;
import me.topchetoeu.j2s.runtime.EventLoop;
import me.topchetoeu.j2s.runtime.Frame;
import me.topchetoeu.j2s.runtime.debug.DebugContext;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.CodeFunction;
import me.topchetoeu.j2s.runtime.values.functions.FunctionValue;
import me.topchetoeu.j2s.runtime.values.functions.NativeFunction;
import me.topchetoeu.j2s.runtime.values.objects.ArrayLikeValue;
import me.topchetoeu.j2s.runtime.values.objects.ArrayValue;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;
import me.topchetoeu.j2s.runtime.values.objects.buffers.Int32ArrayValue;
import me.topchetoeu.j2s.runtime.values.objects.buffers.Int8ArrayValue;
import me.topchetoeu.j2s.runtime.values.objects.buffers.TypedArrayValue;
import me.topchetoeu.j2s.runtime.values.objects.buffers.Uint8ArrayValue;
import me.topchetoeu.j2s.runtime.values.primitives.BoolValue;
import me.topchetoeu.j2s.runtime.values.primitives.StringValue;
import me.topchetoeu.j2s.runtime.values.primitives.SymbolValue;
import me.topchetoeu.j2s.runtime.values.primitives.UserValue;
import me.topchetoeu.j2s.runtime.values.primitives.VoidValue;
import me.topchetoeu.j2s.runtime.values.primitives.numbers.NumberValue;

public class SimpleRepl {
	public static final Compiler DEFAULT_COMPILER = (env, filename, raw, mapper) -> {
		try {
			var res = JavaScript.compile(env, filename, raw, true);
			var body = res.body();

			DebugContext.get(env).onSource(filename, raw);
			for (var el : res.bodies()) {
				DebugContext.get(env).onFunctionLoad(el, res.map(mapper));
			}

			return new CodeFunction(env, filename.toString(), body, new Value[0][]);
		}
		catch (SyntaxException e) {
			var res = EngineException.ofSyntax(e.msg);
			res.add(env, e.loc.filename() + "", e.loc);
			throw res;
		}
	};

	static Thread engineTask, debugTask;
	static Engine engine = new Engine();
	static Environment environment = Environment.empty(), tsEnvironment;
	static DebugServer server;
	static Debugger debugger;
	static Key<OutputStream> STDOUT = new Key<>();
	static Key<OutputStream> STDERR = new Key<>();
	static Key<InputStream> STDIN = new Key<>();

	static int j = 0;
	static String[] args;

	private static void reader() {
		try {
			try {
				environment = createESEnv();
				tsEnvironment = createESEnv();
			}
			catch (ExecutionException e) { throw e.getCause(); }
			
			server = new DebugServer();
			debugTask = server.start(new InetSocketAddress("127.0.0.1", 9229), true);
			server.targets.put("default", (socket, req) -> new SimpleDebugger(socket)
				.attach(DebugContext.get(environment))
				.attach(DebugContext.get(tsEnvironment))
			);

			try {
				try { initGlobals(); } catch (ExecutionException e) { throw e.getCause(); }
			}
			catch (EngineException | SyntaxException e) {
				System.err.println("Failed to load stdlib. Falling back to barebones environment...");
				System.err.println(Value.errorToReadable(environment, e, null));
			}


			System.out.println(String.format("Running %s v%s by %s", Metadata.name(), Metadata.version(), Metadata.author()));

			for (var arg : args) {
				var file = new File(arg);
				var raw = Reading.streamToString(new FileInputStream(file));

				try {
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
				var raw = Reading.readline();

				if (raw == null) break;

				try {
					try {
						var res = engine.pushMsg(
							false, environment,
							new Filename(Metadata.name(), "repl/" + i + ".js"), raw,
							Value.UNDEFINED
						).get();
						System.err.println(res.toReadable(environment));
					}
					catch (ExecutionException e) { throw e.getCause(); }
				}
				catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }

			}
		}
		catch (EngineException | SyntaxException e) { System.err.println(Value.errorToReadable(environment, e, null)); }
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
			var isWeak = args.get(0).toBoolean();
			return UserValue.of(isWeak ? new WeakHashMap<>() : new LinkedHashMap<>(), prototype[0]);
		});
		mapConstr.prototype.defineOwnField(env, "get", new NativeFunction(getArgs -> {
			var map = getArgs.self(Map.class);
			var key = getArgs.get(0);
			var val = map.get(key);
			return val == null ? Value.UNDEFINED : (Value)val;
		}));
		mapConstr.prototype.defineOwnField(env, "set", new NativeFunction(getArgs -> {
			var map = getArgs.self(Map.class);
			var key = getArgs.get(0);
			var val = getArgs.get(1);
			map.put(key, val);

			return Value.UNDEFINED;
		}));
		mapConstr.prototype.defineOwnField(env, "has", new NativeFunction(getArgs -> {
			var map = getArgs.self(Map.class);
			var key = getArgs.get(0);
			return BoolValue.of(map.containsKey(key));
		}));
		mapConstr.prototype.defineOwnField(env, "delete", new NativeFunction(getArgs -> {
			var map = getArgs.self(Map.class);
			var key = getArgs.get(0);
			map.remove(key);
			return Value.UNDEFINED;
		}));
		mapConstr.prototype.defineOwnField(env, "keys", new NativeFunction(getArgs -> {
			var map = getArgs.self(Map.class);
			return ArrayValue.of(map.keySet());
		}));
		mapConstr.prototype.defineOwnField(env, "clear", new NativeFunction(getArgs -> {
			getArgs.self(Map.class).clear();
			return Value.UNDEFINED;
		}));
		mapConstr.prototype.defineOwnField(env, "size", new NativeFunction(getArgs -> {
			return NumberValue.of(getArgs.self(Map.class).size());
		}));
		prototype[0] = (ObjectValue)mapConstr.prototype;

		return mapConstr;
	}

	public static String processRegex(String src) {
		var n = 0;

		var source = new StringBuilder();

		StringBuilder bracesSource = null;
		StringBuilder bracketsSource = null;

		while (true) {
			if (n >= src.length()) break;
			var c = src.charAt(n++);

			if (c == '\\' && n + 1 < src.length() && src.charAt(n) == 'b') {
				c = '\b';
				n++;
			}

			if (bracesSource != null) {
				var failed = true;

				if (Character.isDigit(c)) {
					bracesSource.append(c);
					failed = false;
				}
				else if (c == ',' && bracesSource.indexOf(",") < 0) {
					bracesSource.append(c);
					failed = false;
				}
				else if (c == '}' && bracesSource.length() > 0) {
					bracesSource.append(c);
					source.append(bracesSource);
					bracesSource = null;
					continue;
				}

				if (failed) {
					source.append("\\");
					source.append(bracesSource);
					bracesSource = null;
					n--;
				}
			}
			else if (bracketsSource != null) {
				if (c == '[') bracketsSource.append("\\[");
				else if (c == ']') {
					var res = bracketsSource.append(']').toString();
					bracketsSource = null;
					if (res.equals("[^]")) res = "[\\s\\S]";
						else if (res.equals("[]")) res = "[^\\s\\S]";
					source.append(res);
				}
				else if (c == '\\') {
					if (n >= src.length()) break;
					bracketsSource.append(c).append(src.charAt(n++));
				}
				else bracketsSource.append(c);
			}
			else if (c == '\\') {
				if (n >= src.length()) throw new PatternSyntaxException("Unexpected end", src, n);
				c = src.charAt(n++);
				source.append('\\').append(c);
			}
			else if (c == '[') {
				bracketsSource = new StringBuilder("[");
			}
			else if (c == '{' && bracketsSource == null) {
				bracesSource = new StringBuilder("{");
			}
			else source.append(c);
		}

		if (bracesSource != null) {
			source.append("\\");
			source.append(bracesSource);
		}
		if (bracketsSource != null) throw new PatternSyntaxException("Unmatched '['", src, n - bracketsSource.length());

		return source.toString();
	}

	private static ObjectValue regexPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		var prototype = new ObjectValue[1];
		NativeFunction mapConstr = new NativeFunction(args -> {
			var flags = 0;
			if (args.get(1).toBoolean()) flags |= Pattern.MULTILINE;
			if (args.get(2).toBoolean()) flags |= Pattern.CASE_INSENSITIVE;
			if (args.get(3).toBoolean()) flags |= Pattern.DOTALL;
			if (args.get(4).toBoolean()) flags |= Pattern.UNICODE_CASE | Pattern.CANON_EQ;
			if (args.get(5).toBoolean()) flags |= Pattern.UNICODE_CHARACTER_CLASS;
			try {
				var pattern = Pattern.compile(processRegex(args.get(0).toString(args.env)), flags);
				return UserValue.of(pattern, prototype[0]);
			}
			catch (PatternSyntaxException e) {
				throw EngineException.ofSyntax("(regex):" + e.getIndex() + ": " + e.getDescription());
			}
		});
		mapConstr.prototype.defineOwnField(env, "exec", new NativeFunction(args -> {
			var pattern = args.self(Pattern.class);
			var target = args.get(0).toString(args.env);
			var offset = args.get(1).toNumber(args.env).getInt();
			var index = args.get(2).toBoolean();

			if (offset > target.length()) return Value.NULL;

			var matcher = pattern.matcher(target).region(offset, target.length());
			if (!matcher.find()) return Value.NULL;

			var matchesArr = new ArrayValue(matcher.groupCount() + 1);
			for (var i = 0; i < matcher.groupCount() + 1; i++) {
				var group = matcher.group(i);
				if (group == null) continue;
				matchesArr.set(args.env, i, StringValue.of(group));
			}

			matchesArr.defineOwnField(args.env, "index", NumberValue.of(matcher.start()));
			matchesArr.defineOwnField(args.env, "input", StringValue.of(target));
			if (index) {
				var indices = new ArrayValue();
				indices.setPrototype(args.env, null);
				for (var i = 0; i < matcher.groupCount(); i++) {
					matchesArr.set(args.env, i, ArrayValue.of(Arrays.asList(
						NumberValue.of(matcher.start(i)),
						NumberValue.of(matcher.end(i))
					)));
				}
			}

			var obj = new ObjectValue();
			obj.defineOwnField(args.env, "matches", matchesArr);
			obj.defineOwnField(args.env, "end", NumberValue.of(matcher.end()));

			return obj;
			// return val == null ? Value.UNDEFINED : (Value)val;
		}));
		mapConstr.prototype.defineOwnField(env, "groupCount", new NativeFunction(args -> {
			var pattern = args.self(Pattern.class);
			return NumberValue.of(pattern.matcher("").groupCount());
		}));
		prototype[0] = (ObjectValue)mapConstr.prototype;

		return mapConstr;
	}

	private static ObjectValue symbolPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "makeSymbol", new NativeFunction(args -> new SymbolValue(args.get(0).toString(args.env))));
		res.defineOwnField(env, "getSymbol", new NativeFunction(args -> SymbolValue.get(args.get(0).toString(args.env))));
		res.defineOwnField(env, "getSymbolKey", new NativeFunction(args -> ((SymbolValue)args.get(0)).key()));
		res.defineOwnField(env, "getSymbolDescriptor", new NativeFunction(args -> StringValue.of(((SymbolValue)args.get(0)).value)));

		return res;
	}

	private static ObjectValue numberPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "parseInt", new NativeFunction(args -> {
			var nradix = args.get(1).toNumber(env);
			var radix = nradix.isInt() ? nradix.getInt() : 10;

			if (radix != 10 && args.get(0) instanceof NumberValue num) {
				if (num.isInt()) return num;
				else return NumberValue.of(num.getDouble() - num.getDouble() % 1);
			}
			else {
				if (radix < 2 || radix > 36) return NumberValue.NAN;
		
				var str = args.get(0).toString().trim();
				var numRes = Parsing.parseInt(new Source(str), 0, "0123456789abcdefghijklmnopqrstuvwxyz".substring(0, radix), true);
				if (numRes.isSuccess()) {
					if (numRes.n == str.length()) return NumberValue.of(numRes.result);
				}
				return NumberValue.NAN;
			}
		}));
		res.defineOwnField(env, "parseFloat", new NativeFunction(args -> {
			if (args.get(0) instanceof NumberValue) {
				return args.get(0);
			}
			else {
				var str = args.get(0).toString().trim();
				var numRes = Parsing.parseFloat(new Source(str), 0, true);
				if (numRes.isSuccess()) {
					if (numRes.n == str.length()) return NumberValue.of(numRes.result);
				}
				return NumberValue.NAN;
			}
		}));
		res.defineOwnField(env, "isNaN", new NativeFunction(args -> BoolValue.of(args.get(0).isNaN())));

		res.defineOwnField(env, "pow", new NativeFunction(args -> {
			return NumberValue.of(Math.pow(args.get(0).toNumber(args.env).getDouble(), args.get(1).toNumber(args.env).getDouble()));
		}));
		res.defineOwnField(env, "log", new NativeFunction(args -> {
			return NumberValue.of(Math.log(args.get(0).toNumber(args.env).getDouble()));
		}));

		res.defineOwnField(env, "NaN", NumberValue.NAN);
		res.defineOwnField(env, "Infinity", NumberValue.of(Double.POSITIVE_INFINITY));
		res.defineOwnField(env, "PI", NumberValue.of(Math.PI));
		res.defineOwnField(env, "E", NumberValue.of(Math.E));

		return res;
	}

	private static ObjectValue stringPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "stringBuild", new NativeFunction(args -> {
			var parts = ((ArrayValue)args.get(0)).toArray();
			var sb = new StringBuilder();

			for (var i = 0; i < parts.length; i++) {
				sb.append(((StringValue)parts[i]).value);
			}

			return StringValue.of(sb.toString());
		}));

		res.defineOwnField(env, "fromCharCode", new NativeFunction(args -> {
			return StringValue.of(new String(new char[] { (char)args.get(0).toNumber(args.env).getInt() }));
		}));

		res.defineOwnField(env, "toCharCode", new NativeFunction(args -> {
			return NumberValue.of(args.get(0).toString(args.env).charAt(0));
		}));
		res.defineOwnField(env, "toCodePoint", new NativeFunction(args -> {
			return NumberValue.of(args.get(0).toString(args.env).codePointAt(args.get(1).toNumber(args.env).getInt()));
		}));

		res.defineOwnField(env, "substring", new NativeFunction(args -> {
			var str = args.get(0).toString(args.env);
			var start = args.get(1).toNumber(args.env).getInt();
			var end = args.get(2).toNumber(args.env).getInt();

			if (end <= start) return StringValue.of("");

			start = Math.max(Math.min(start, str.length()), 0);
			end = Math.max(Math.min(end, str.length()), 0);

			return StringValue.of(str.substring(start, end));
		}));

		res.defineOwnField(env, "indexOf", new NativeFunction(args -> {
			var str = args.get(0).toString(args.env);
			var search = args.get(1).toString(args.env);
			var start = args.get(2).toNumber(args.env).getInt();
			if (start > str.length()) return NumberValue.of(-1);
			var reverse = args.get(3).toBoolean();

			if (reverse) return NumberValue.of(str.lastIndexOf(search, start));
			else return NumberValue.of(str.indexOf(search, start));
		}));

		res.defineOwnField(env, "lower", new NativeFunction(args -> {
			return StringValue.of(args.get(0).toString(args.env).toLowerCase());
		}));
		res.defineOwnField(env, "upper", new NativeFunction(args -> {
			return StringValue.of(args.get(0).toString(args.env).toUpperCase());
		}));

		return res;
	}

	private static ObjectValue objectPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "defineField", new NativeFunction(args -> {
			var obj = (ObjectValue)args.get(0);
			var key = args.get(1);
			var desc = (ObjectValue)args.get(2);

			var valField = desc.getOwnMember(env, "v");
			var writeField = desc.getOwnMember(env, "w");
			var configField = desc.getOwnMember(env, "c");
			var enumField = desc.getOwnMember(env, "e");

			var enumerable = enumField == null ? null : enumField.get(env, desc).toBoolean();
			var configurable = configField == null ? null : configField.get(env, desc).toBoolean();
			var writable = writeField == null ? null : writeField.get(env, desc).toBoolean();
			var value = valField == null ? null : valField.get(env, desc);

			return BoolValue.of(obj.defineOwnField(args.env, key, value, configurable, enumerable, writable));
		}));
		res.defineOwnField(env, "defineProperty", new NativeFunction(args -> {
			var obj = (ObjectValue)args.get(0);
			var key = args.get(1);
			var desc = args.get(2);

			var configField = desc.getOwnMember(env, "c");
			var enumField = desc.getOwnMember(env, "e");
			var getField = desc.getOwnMember(env, "g");
			var setField = desc.getOwnMember(env, "s");

			var enumerable = enumField == null ? null : enumField.get(env, desc).toBoolean();
			var configurable = configField == null ? null : configField.get(env, desc).toBoolean();
			Optional<FunctionValue> getter = null, setter = null;

			if (getField != null) {
				var getVal = getField.get(env, desc);
				if (getVal == Value.UNDEFINED) getter = Optional.empty();
				else getter = Optional.of((FunctionValue)getVal);
			}
			if (setField != null) {
				var setVal = setField.get(env, desc);
				if (setVal == Value.UNDEFINED) setter = Optional.empty();
				else setter = Optional.of((FunctionValue)setVal);
			}

			return BoolValue.of(obj.defineOwnProperty(args.env, key, getter, setter, configurable, enumerable));
		}));
		res.defineOwnField(env, "getPrototype", new NativeFunction(args -> {
			var proto = args.get(0).getPrototype(env);
			if (proto == null) return Value.NULL;
			else return proto;
		}));
		res.defineOwnField(env, "setPrototype", new NativeFunction(args -> {
			var proto = args.get(1) instanceof VoidValue ? null : (ObjectValue)args.get(1);
			args.get(0).setPrototype(env, proto);
			return args.get(0);
		}));
		res.defineOwnField(env, "getOwnMembers", new NativeFunction(args -> {
			var val = new ArrayValue();

			for (var key : args.get(0).getOwnMembers(env, args.get(1).toBoolean())) {
				val.set(args.env, val.size(), StringValue.of(key));
			}

			return val;
		}));
		res.defineOwnField(env, "getOwnSymbolMembers", new NativeFunction(args -> {
			return ArrayValue.of(args.get(0).getOwnSymbolMembers(env, args.get(1).toBoolean()));
		}));
		res.defineOwnField(env, "getOwnMember", new NativeFunction(args -> {
			var obj = args.get(0);
			var key = args.get(1);

			var member = obj.getOwnMember(args.env, key);
			if (member == null) return Value.UNDEFINED;
			else return member.descriptor(args.env, obj);
		}));
		res.defineOwnField(env, "preventExt", new NativeFunction(args -> {
			args.get(0).preventExtensions();
			return VoidValue.UNDEFINED;
		}));
		res.defineOwnField(env, "seal", new NativeFunction(args -> {
			args.get(0).seal();
			return VoidValue.UNDEFINED;
		}));
		res.defineOwnField(env, "freeze", new NativeFunction(args -> {
			args.get(0).freeze();
			return VoidValue.UNDEFINED;
		}));
		res.defineOwnField(env, "memcpy", new NativeFunction(args -> {
			var src = (ArrayValue)args.get(0);
			var dst = (ArrayValue)args.get(1);
			var srcI = args.get(2).toNumber(args.env).getInt();
			var dstI = args.get(3).toNumber(args.env).getInt();
			var n = args.get(4).toNumber(args.env).getInt();

			src.copyTo(dst, srcI, dstI, n);

			return VoidValue.UNDEFINED;
		}));
		res.defineOwnField(env, "sort", new NativeFunction(args -> {
			var arr = (ArrayValue)args.get(0);
			var func = (FunctionValue)args.get(1);

			arr.sort((a, b) -> {
				return func.apply(args.env, Value.UNDEFINED, a, b).toNumber(args.env).getInt();
			});

			return arr;
		}));

		res.defineOwnField(env, "isArray", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof ArrayLikeValue);
		}));

		return res;
	}

	private static ObjectValue bufferPrimordials(Environment env) {
		var buffProto = new ObjectValue();
		buffProto.defineOwnProperty(env, "length", Optional.of(new NativeFunction(args -> {
			return NumberValue.of(args.self(byte[].class).length);
		})), Optional.empty(), false, true);

		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "buff", new NativeFunction(args -> {
			var size = args.get(0).toNumber(env).getInt();
			return TypedArrayValue.buffer(new byte[size], buffProto);
		}));

		res.defineOwnField(env, "uint8", new NativeFunction(args -> {
			var buff = args.get(byte[].class, 0);
			var start = args.get(1).toNumber(env).getInt();
			var end = args.get(2).toNumber(env).getInt();
			return new Uint8ArrayValue(buff, start, end);
		}));
		res.defineOwnField(env, "int8", new NativeFunction(args -> {
			var buff = args.get(byte[].class, 0);
			var start = args.get(1).toNumber(env).getInt();
			var end = args.get(2).toNumber(env).getInt();
			return new Int8ArrayValue(buff, start, end);
		}));
		res.defineOwnField(env, "int32", new NativeFunction(args -> {
			var buff = args.get(byte[].class, 0);
			var start = args.get(1).toNumber(env).getInt();
			var end = args.get(2).toNumber(env).getInt();
			return new Int32ArrayValue(buff, start, end);
		}));

		res.defineOwnField(env, "isUint8", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof Uint8ArrayValue);
		}));
		res.defineOwnField(env, "isInt8", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof Int8ArrayValue);
		}));
		res.defineOwnField(env, "isInt32", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof Int32ArrayValue);
		}));

		res.defineOwnField(env, "is", new NativeFunction(args -> {
			return BoolValue.of(args.get(0) instanceof TypedArrayValue);
		}));
		res.defineOwnField(env, "isBuff", new NativeFunction(args -> {
			return BoolValue.of(args.get(byte[].class, 0) != null);
		}));

		res.defineOwnField(env, "backer", new NativeFunction(args -> {
			return TypedArrayValue.buffer(((TypedArrayValue)args.get(0)).buffer, buffProto);
		}));
		res.defineOwnField(env, "start", new NativeFunction(args -> {
			return NumberValue.of(((TypedArrayValue)args.get(0)).start);
		}));
		res.defineOwnField(env, "end", new NativeFunction(args -> {
			return NumberValue.of(((TypedArrayValue)args.get(0)).end);
		}));

		return res;
	}

	private static ObjectValue functionPrimordials(Environment env) {
		var res = new ObjectValue();
		res.setPrototype(null, null);

		res.defineOwnField(env, "setCallable", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			func.enableApply = args.get(1).toBoolean();
			return Value.UNDEFINED;
		}));
		res.defineOwnField(env, "setConstructable", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			func.enableConstruct = args.get(1).toBoolean();
			return Value.UNDEFINED;
		}));
		res.defineOwnField(env, "invokeType", new NativeFunction(args -> {
			if (((ArgumentsValue)args.get(0)).frame.isNew) return StringValue.of("new");
			else return StringValue.of("call");
		}));
		res.defineOwnField(env, "invokeTypeInfer", new NativeFunction(args -> {
			var frame = Frame.get(args.env, args.get(0).toNumber(args.env).getInt());
			if (frame.isNew) return StringValue.of("new");
			else return StringValue.of("call");
		}));
		res.defineOwnField(env, "target", new NativeFunction(args -> {
			var frame = Frame.get(args.env, args.get(0).toNumber(args.env).getInt());
			if (frame.target == null) return Value.UNDEFINED;
			else return frame.target;
		}));

		res.defineOwnField(env, "invoke", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			var self = args.get(1);
			var funcArgs = (ArrayValue)args.get(2);

			return func.apply(env, self, funcArgs.toArray());
		}));
		res.defineOwnField(env, "construct", new NativeFunction(args -> {
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

		res.defineOwnField(env, "stringify", new NativeFunction(args -> {
			return StringValue.of(JSON.stringify(JSONConverter.fromJs(env, args.get(0))));
		}));
		res.defineOwnField(env, "parse", new NativeFunction(args -> {
			try {
				return JSONConverter.toJs(JSON.parse(null, args.get(0).toString(env)));
			}
			catch (SyntaxException e) {
				throw EngineException.ofSyntax(e.msg).add(env, e.loc.filename() + "", e.loc);
			}
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

		res.defineOwnField(env, "symbol", symbolPrimordials(env));
		res.defineOwnField(env, "number", numberPrimordials(env));
		res.defineOwnField(env, "string", stringPrimordials(env));
		res.defineOwnField(env, "object", objectPrimordials(env));
		res.defineOwnField(env, "buffer", bufferPrimordials(env));
		res.defineOwnField(env, "function", functionPrimordials(env));
		res.defineOwnField(env, "json", jsonPrimordials(env));
		res.defineOwnField(env, "map", mapPrimordials(env));
		res.defineOwnField(env, "regex", regexPrimordials(env));

		int[] i = new int[1];

		res.defineOwnField(env, "setGlobalPrototypes", new NativeFunction(args -> {
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
			setProto(args.env, env, Value.UINT8_ARR_PROTO, obj, "uint8");
			setProto(args.env, env, Value.INT32_ARR_PROTO, obj, "int32");
			var val = obj.getMember(args.env, "regex");
			if (val instanceof FunctionValue func) {
				env.add(Value.REGEX_CONSTR, func);
			}
			return Value.UNDEFINED;
		}));
		res.defineOwnField(env, "setIntrinsic", new NativeFunction(args -> {
			var name = args.get(0).toString(env);
			var val = args.get(1);

			Value.intrinsics(environment).put(name, val);

			return Value.UNDEFINED;
		}));
		res.defineOwnField(env, "compile", new NativeFunction(args -> {
			return Compiler.compileFunc(env, new Filename(Metadata.name(), "func" + i[0]++ + ".js"), args.get(0).toString(env));
		}));
		res.defineOwnField(env, "now", new NativeFunction(args -> {
			return NumberValue.of(System.currentTimeMillis());
		}));
		res.defineOwnField(env, "next", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			EventLoop.get(env).pushMsg(() -> {
				func.apply(env, Value.UNDEFINED);
			}, true);
			return Value.UNDEFINED;
		}));

		return res;
	}

	private static Environment createESEnv() throws InterruptedException, ExecutionException {
		var env = initEnv();
		var stubEnv = initEnv();
		Value.global(stubEnv).defineOwnField(stubEnv, "target", Value.global(env));
		Value.global(stubEnv).defineOwnField(stubEnv, "primordials", primordials(env));

		EventLoop.get(stubEnv).pushMsg(
			false, stubEnv,
			new Filename(Metadata.name(), "init.js"), Reading.resourceToString("lib/stdlib.js"),
			Value.UNDEFINED
		).get();

		return env;
	}

	private static Compiler wrap(Compiler first, Environment compilerEnv, Environment targetEnv, FunctionValue factory) {
		var curr = new NativeFunction(args -> {
			var filename = Filename.parse(args.get(0).toString(args.env));
			var src = args.get(1).toString(args.env);
			var mapper = (FunctionValue)args.get(2);
			return first.compile(targetEnv, filename, src, NativeMapper.unwrap(args.env, mapper));
		});

		var next = (FunctionValue)factory.apply(compilerEnv, Value.UNDEFINED, curr);

		return (env, filename, source, map) -> {
			return (FunctionValue)next.apply(
				compilerEnv, Value.UNDEFINED,
				StringValue.of(filename.toString()),
				StringValue.of(source),
				new NativeMapper(map)
			);
		};
	}


	private static Environment initEnv() {
		var env = new Environment();
		env.add(EventLoop.KEY, engine);
		env.add(DebugContext.KEY, new DebugContext());
		env.add(Compiler.KEY, DEFAULT_COMPILER);
		// env.add(CompileResult.DEBUG_LOG);

		var glob = Value.global(env);

		glob.defineOwnField(null, "exit", new NativeFunction("exit", args -> {
			Thread.currentThread().interrupt();
			throw new CancellationException();
		}));
		glob.defineOwnField(null, "print", new NativeFunction("print", args -> {
			for (var el : args.args) {
				if (el instanceof StringValue) System.out.print(((StringValue)el).value + " \t");
				else System.out.print(el.toReadable(args.env) + " \t");
			}
			System.out.println();

			return Value.UNDEFINED;
		}));
		glob.defineOwnField(null, "measure", new NativeFunction("measure", args -> {
			var start = System.nanoTime();

			((FunctionValue)args.get(0)).apply(args.env, Value.UNDEFINED);

			System.out.println(String.format("Finished in %sms", (System.nanoTime() - start) / 1000000.));

			return Value.UNDEFINED;
		}));

		return env;
	}
	private static void initEngine() {
		engineTask = engine.start();
	}
	private static void initGlobals() throws InterruptedException, ExecutionException {
		var res = new FunctionValue[1];
		var setter = new NativeFunction(args -> {
			res[0] = (FunctionValue)args.get(0);
			return Value.UNDEFINED;
		});

		var tsGlob = Value.global(tsEnvironment);
		var tsCompilerFactory = new FunctionValue[1];

		tsGlob.defineOwnField(tsEnvironment, "getResource", new NativeFunction(args -> {
			var name = args.get(0).toString(args.env);
			var src = Reading.resourceToString("lib/" + name);

			if (src == null) return Value.UNDEFINED;
			else return StringValue.of(src);
		}));
		tsGlob.defineOwnField(tsEnvironment, "register", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			tsCompilerFactory[0] = func;
			return Value.UNDEFINED;
		}));
		tsGlob.defineOwnField(tsEnvironment, "registerSource", new NativeFunction(args -> {
			var filename = Filename.parse(args.get(0).toString(args.env));
			var src = args.get(1).toString(args.env);
			DebugContext.get(environment).onSource(filename, src);
			return Value.UNDEFINED;
		}));

		var ts = Reading.resourceToString("lib/transpiler.js");
		if (ts != null) EventLoop.get(tsEnvironment).pushMsg(
			false, tsEnvironment,
			new Filename(Metadata.name(), "transpiler.js"), ts,
			Value.UNDEFINED, setter
		).get();

		var tsCompiler = wrap(Compiler.get(environment), tsEnvironment, environment, tsCompilerFactory[0]);
		environment.add(Compiler.KEY, tsCompiler);
	}


	public static void main(String args[]) throws InterruptedException {
		SimpleRepl.args = args;
		var reader = new Thread(SimpleRepl::reader);

		environment = initEnv();

		initEngine();

		reader.setDaemon(true);
		reader.setName("STD Reader");
		reader.start();

		reader.join();
		engineTask.interrupt();
		debugTask.interrupt();
	}
}
