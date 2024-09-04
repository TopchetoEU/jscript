package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Map;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public abstract class PrimitiveValue extends Value {
    @Override public final boolean defineOwnMember(Environment env, KeyCache key, Member member) { return false; }
    @Override public final boolean deleteOwnMember(Environment env, KeyCache key) { return false; }
    @Override public final boolean isPrimitive() { return true; }
    @Override public final Value toPrimitive(Environment env) { return this; }

    @Override public final boolean setPrototype(Environment env, ObjectValue val) { return false; }

    @Override public Member getOwnMember(Environment env, KeyCache key) { return null; }
    @Override public Map<String, Member> getOwnMembers(Environment env) { return Map.of(); }
    @Override public Map<SymbolValue, Member> getOwnSymbolMembers(Environment env) { return Map.of(); }
}
