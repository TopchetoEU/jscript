package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;

public interface Variable {
    Value get(Environment env);
    default boolean readonly() { return true; }
    default boolean set(Environment env, Value val) { return false; }

    default FieldMember toField(boolean configurable, boolean enumerable) {
        var self = this;

        return new FieldMember(!readonly(), configurable, enumerable) {
            @Override public Value get(Environment env, Value _self) {
                return self.get(env);
            }
            @Override public boolean set(Environment env, Value val, Value _self) {
                return self.set(env, val);
            }
        };
    }
}
