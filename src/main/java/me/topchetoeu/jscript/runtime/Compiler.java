package me.topchetoeu.jscript.runtime;

import java.util.function.Function;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.repl.mapping.NativeMapper;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;

public interface Compiler {
	public static final Compiler DEFAULT = (env, filename, raw, mapper) -> {
		try {
			var res = JavaScript.compile(env, filename, raw, true);
			var body = res.body();
			DebugContext.get(env).onSource(filename, raw);
			registerFunc(env, body, res, mapper);
			return new CodeFunction(env, filename.toString(), body, new Value[0][]);
		}
		catch (SyntaxException e) {
			var res = EngineException.ofSyntax(e.msg);
			res.add(env, e.loc.filename() + "", e.loc);
			throw res;
		}
	};

	public Key<Compiler> KEY = new Key<>();

	public FunctionValue compile(Environment env, Filename filename, String source, Function<Location, Location> map);

	public default Compiler wrap(Environment compilerEnv, Environment targetEnv, FunctionValue factory) {
		var curr = new NativeFunction(args -> {
			var filename = Filename.parse(args.get(0).toString(args.env));
			var src = args.get(1).toString(args.env);
			var mapper = (FunctionValue)args.get(2);
			return this.compile(targetEnv, filename, src, NativeMapper.unwrap(args.env, mapper));
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

	public static Compiler get(Environment ext) {
		return ext.get(KEY, (env, filename, src, map) -> {
			throw EngineException.ofError("No compiler attached to engine");
		});
	}

	static void registerFunc(Environment env, FunctionBody body, CompileResult res, Function<Location, Location> mapper) {
		var map = res.map(mapper);

		DebugContext.get(env).onFunctionLoad(body, map);

		for (var i = 0; i < body.children.length; i++) {
			registerFunc(env, body.children[i], res.children.get(i), mapper);
		}
	}

	public static FunctionValue compileFunc(Environment env, Filename filename, String raw) {
		return get(env).compile(env, filename, raw, v -> v);
	}
}
