package me.topchetoeu.jscript.core.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class VariableDeclareStatement extends Statement {
    public static class Pair {
        public final String name;
        public final Statement value;
        public final Location location;

        public Pair(String name, Statement value, Location location) {
            this.name = name;
            this.value = value;
            this.location = location;
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

            if (key instanceof String) target.add(Instruction.makeVar(entry.location, (String)key));

            if (entry.value != null) {
                FunctionStatement.compileWithName(entry.value, target, scope, true, entry.name, BreakpointType.STEP_OVER);
                target.add(Instruction.storeVar(entry.location, key));
            }
        }

        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public VariableDeclareStatement(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }
}
