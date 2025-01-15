package me.topchetoeu.j2s.runtime;

import java.util.function.Function;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Key;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.functions.FunctionValue;

public interface Compiler {
	public Key<Compiler> KEY = new Key<>();

	public FunctionValue compile(Environment env, Filename filename, String source, Function<Location, Location> map);

	public static Compiler get(Environment ext) {
		return ext.get(KEY, (env, filename, src, map) -> {
			throw EngineException.ofError("No compiler attached to engine");
		});
	}

	public static FunctionValue compileFunc(Environment env, Filename filename, String raw) {
		return get(env).compile(env, filename, raw, v -> v);
	}
}
