package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public final class VoidValue extends PrimitiveValue {
	public final String name;
	public final String type;

	@Override public StringValue type() { return StringValue.of(type); }
	@Override public boolean toBoolean() { return false; }
	@Override public NumberValue toNumber(Environment ext) { return NumberValue.NAN; }
	@Override public String toString(Environment ext) { return name; }

	@Override public ObjectValue getPrototype(Environment env) { return null; }

	@Override public Member getOwnMember(Environment env, KeyCache key) {
		if (key.isSymbol()) throw EngineException.ofError(String.format("Cannot read properties of %s (reading '%s')", name, key.toSymbol().toString()));
		else throw EngineException.ofError(String.format("Cannot read properties of %s (reading '%s')", name, key.toString(env)));
	}

	@Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		return Arrays.asList(name);
	}

	public VoidValue(String name, String type) {
		this.name = name;
		this.type = type;
	}
}
