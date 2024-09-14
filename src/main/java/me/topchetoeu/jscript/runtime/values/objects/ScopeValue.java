package me.topchetoeu.jscript.runtime.values.objects;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;

public final class ScopeValue extends ObjectValue {
    private static class VariableField extends FieldMember {
        private int i;

        public VariableField(int i, ScopeValue self) {
            super(self, false, true, true);
            this.i = i;
        }

        @Override public Value get(Environment env, Value self) {
            return ((ScopeValue)self).variables[i][0];
        }

        @Override public boolean set(Environment env, Value val, Value self) {
            ((ScopeValue)self).variables[i][0] = val;
            return true;
        }
    }

    public final Value[][] variables;

    public ScopeValue(Value[][] variables, String[] names) {
        this.variables = variables;
        for (var i = 0; i < names.length && i < variables.length; i++) {
            defineOwnMember(Environment.empty(), i, new VariableField(i, this));
        }
    }
}
