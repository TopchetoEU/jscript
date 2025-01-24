package me.topchetoeu.j2s.lib;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Metadata;
import me.topchetoeu.j2s.common.Reading;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.CodeFunction;

public class StdLib {
	private static final CompileResult RUNNER = JavaScript.compile(
		new Filename(Metadata.name(), "init.js"),
		Reading.resourceToString("lib/stdlib.js"), false
	);

	public static Environment apply(Environment env, CompileResult body) {
		env = new Environment();

		var stubEnv = new Environment();
		Value.global(stubEnv).defineOwnField(stubEnv, "target", Value.global(env));
		Value.global(stubEnv).defineOwnField(stubEnv, "primordials", Primordials.create(env));

		var func = new CodeFunction(stubEnv, "intializer", body.body(), new Value[0][]);
		try {
			func.apply(stubEnv, Value.UNDEFINED);
		}
		catch (EngineException e) {
			System.out.println(Value.errorToReadable(env, e, "in environment initializer"));
		}
	
		return env;
	}

	public static Environment apply(Environment env) {
		if (env == null) env = new Environment();
		return apply(env, RUNNER);
	}
}
