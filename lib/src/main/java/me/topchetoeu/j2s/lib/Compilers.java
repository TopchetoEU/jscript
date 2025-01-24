package me.topchetoeu.j2s.lib;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Metadata;
import me.topchetoeu.j2s.common.Reading;
import me.topchetoeu.j2s.common.SyntaxException;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.runtime.Compiler;
import me.topchetoeu.j2s.runtime.debug.DebugHandler;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.CodeFunction;
import me.topchetoeu.j2s.runtime.values.functions.FunctionValue;
import me.topchetoeu.j2s.runtime.values.functions.NativeFunction;
import me.topchetoeu.j2s.runtime.values.primitives.StringValue;

public class Compilers {
	public static Compiler jsCompiler() {
		return (env, filename, raw, mapper) -> {
			try {
				var res = JavaScript.compile(env, filename, raw, true);
				var body = res.body();
	
				DebugHandler.get(env).onSourceLoad(filename, raw);
				for (var el : res.all()) {
					DebugHandler.get(env).onFunctionLoad(el.body(), el.map(mapper));
				}
	
				return new CodeFunction(env, filename.toString(), body, new Value[0][]);
			}
			catch (SyntaxException e) {
				var res = EngineException.ofSyntax(e.msg);
				res.add(env, e.loc.filename() + "", e.loc);
				throw res;
			}
		};
	}

	public static Compiler wrap(Compiler first, Environment compilerEnv, Environment targetEnv, FunctionValue factory) {
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

	public static Compiler transpilerFromSource(Compiler prev, Environment target, Filename compilerName, String compilerSrc) {
		var env = StdLib.apply(null);

		var glob = Value.global(env);
		var compilerFactory = new FunctionValue[1];

		glob.defineOwnField(env, "getResource", new NativeFunction(args -> {
			var name = args.get(0).toString(args.env);
			var src = Reading.resourceToString("lib/" + name);

			if (src == null) return Value.UNDEFINED;
			else return StringValue.of(src);
		}));
		glob.defineOwnField(env, "register", new NativeFunction(args -> {
			var func = (FunctionValue)args.get(0);
			compilerFactory[0] = func;
			return Value.UNDEFINED;
		}));
		glob.defineOwnField(env, "registerSource", new NativeFunction(args -> {
			var filename = Filename.parse(args.get(0).toString(args.env));
			var src = args.get(1).toString(args.env);
			DebugHandler.get(target).onSourceLoad(filename, src);
			return Value.UNDEFINED;
		}));

		var compiled = JavaScript.compile(compilerName, compilerSrc, false);
		new CodeFunction(env, "intializer", compiled.body(), new Value[0][]).apply(env, Value.UNDEFINED);

		return wrap(prev, env, target, compilerFactory[0]);
	}

	public static Compiler babelCompiler(Compiler prev, Environment target) {
		return transpilerFromSource(prev, target,
			new Filename(Metadata.name(), "babel.js"),
			Reading.resourceToString("lib/babel.js")
		);
	}
	public static Compiler typescriptCompiler(Compiler prev, Environment target) {
		return transpilerFromSource(prev, target,
			new Filename(Metadata.name(), "typescript.js"),
			Reading.resourceToString("lib/typescript.js")
		);
	}
	public static Compiler coffeescriptCompiler(Compiler prev, Environment target) {
		return transpilerFromSource(prev, target,
			new Filename(Metadata.name(), "coffee.js"),
			Reading.resourceToString("lib/coffee.js")
		);
	}

	public static interface TranspilerFactory {
		Compiler create(Compiler prev, Environment target);
	}

	public static Compiler chainTranspilers(Environment target, Compiler base, TranspilerFactory ...factories) {
		var res = base;

		for (var el : factories) {
			res = el.create(res, target);
		}

		return res;
	}
}
