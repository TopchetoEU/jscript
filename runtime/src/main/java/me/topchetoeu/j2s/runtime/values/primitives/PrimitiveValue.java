package me.topchetoeu.j2s.runtime.values.primitives;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.runtime.values.KeyCache;
import me.topchetoeu.j2s.runtime.values.Member;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.functions.FunctionValue;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;

public interface PrimitiveValue extends Value {
	public default boolean defineOwnField(
		Environment env, KeyCache key, Value val,
		Boolean writable, Boolean enumerable, Boolean configurable
	) { return false; }
	public default boolean defineOwnProperty(
		Environment env, KeyCache key, Optional<FunctionValue> get, Optional<FunctionValue> set,
		Boolean enumerable, Boolean configurable
	) { return false; }
	public default boolean deleteOwnMember(Environment env, KeyCache key) { return true; }

	public default boolean isPrimitive() { return true; }
	public default Value toPrimitive(Environment env) { return this; }

	public default boolean setPrototype(Environment env, ObjectValue val) { return false; }

	public default Member getOwnMember(Environment env, KeyCache key) { return null; }
	public default Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) { return new HashSet<>(); }
	public default Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) { return new HashSet<>(); }

	public default State getState() { return State.FROZEN; }

	public default void preventExtensions() {}
	public default void seal() {}
	public default void freeze() {}
}
