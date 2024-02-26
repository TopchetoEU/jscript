package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;

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
    public void declare(CompileResult target) {
        for (var key : values) {
            target.scope.define(key.name);
        }
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        for (var entry : values) {
            if (entry.name == null) continue;
            var key = target.scope.getKey(entry.name);

            if (key instanceof String) target.add(Instruction.makeVar((String)key));

            if (entry.value != null) {
                FunctionStatement.compileWithName(entry.value, target, true, entry.name, BreakpointType.STEP_OVER);
                target.add(Instruction.storeVar(key));
            }
        }

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public VariableDeclareStatement(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }
}
