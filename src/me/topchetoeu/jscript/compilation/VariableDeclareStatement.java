package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableDeclareStatement extends Statement {
    public static class Pair {
        public final String name;
        public final Statement value;

        public Pair(String name, Statement value) {
            this.name = name;
            this.value = value;
        }
    }

    public final List<Pair> values;

    @Override
    public void declare(ScopeRecord varsScope) {
        for (var key : values) {
            varsScope.define(key.name);
        }
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        for (var entry : values) {
            if (entry.name == null) continue;
            var key = scope.getKey(entry.name);
            if (key instanceof String) target.add(Instruction.makeVar((String)key).locate(loc()));

            if (entry.value instanceof FunctionStatement) {
                ((FunctionStatement)entry.value).compile(target, scope, entry.name, false);
                target.add(Instruction.storeVar(key).locate(loc()));
            }
            else if (entry.value != null) {
                entry.value.compile(target, scope, true);
                target.add(Instruction.storeVar(key).locate(loc()));
            }
        }

        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }

    public VariableDeclareStatement(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }
}
