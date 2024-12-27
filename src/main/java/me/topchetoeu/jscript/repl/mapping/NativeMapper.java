package me.topchetoeu.jscript.repl.mapping;

import java.util.function.Function;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ArrayLikeValue;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class NativeMapper extends FunctionValue {
	public final Function<Location, Location> mapper;

	@Override protected Value onApply(Environment env, Value thisArg, Value... args) {
		var rawLoc = (ArrayLikeValue)args[0];
		var loc = Location.of(
			Filename.parse(rawLoc.get(0).toString(env)),
			rawLoc.get(1).toNumber(env).getInt(),
			rawLoc.get(2).toNumber(env).getInt()
		);

		var res = mapper.apply(loc);
		if (res == null) return Value.UNDEFINED;

		return new ArrayValue(
			StringValue.of(res.filename().toString()),
			NumberValue.of(res.line()),
			NumberValue.of(res.start())
		);
	}

	@Override protected Value onConstruct(Environment ext, Value target, Value... args) {
		throw EngineException.ofType("Function cannot be constructed");
	}

	public NativeMapper(Function<Location, Location> mapper) {
		super("mapper", 1);
		this.mapper = mapper;
	}

	public static Function<Location, Location> unwrap(Environment env, FunctionValue func) {
		if (func instanceof NativeMapper nat) return nat.mapper;

		return loc -> {
			var rawLoc = new ArrayValue(
				StringValue.of(loc.filename().toString()),
				NumberValue.of(loc.line()),
				NumberValue.of(loc.start())
			);

			var rawRes = func.apply(env, Value.UNDEFINED, rawLoc);
			if (rawRes instanceof ArrayLikeValue arr) return Location.of(
				Filename.parse(arr.get(0).toString(env)),
				arr.get(1).toNumber(env).getInt(),
				arr.get(2).toNumber(env).getInt()
			);
			else if (rawRes == Value.UNDEFINED || rawRes == Value.NULL) return null;
			else throw EngineException.ofType("Location must be an array, null or undefined");
		};
	}
}
