package me.topchetoeu.jscript.runtime.values.objects;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;

public class ScopeValue extends ObjectValue {
    private class VariableField extends FieldMember {
        private int i;

        public VariableField(int i) {
            super(false, true, true);
            this.i = i;
        }

        @Override public Value get(Environment env, Value self) {
            return variables[i].get(env);
        }

        @Override public boolean set(Environment env, Value val, Value self) {
            return variables[i].set(env, val);
        }
    }

    public final ValueVariable[] variables;

    public ScopeValue(ValueVariable[] variables, String[] names) {
        this.variables = variables;
        for (var i = 0; i < names.length && i < variables.length; i++) {
            defineOwnMember(Environment.empty(), i, new VariableField(i));
        }
    }
}
