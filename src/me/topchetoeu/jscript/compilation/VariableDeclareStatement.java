package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableDeclareStatement extends Statement {
    public static record Pair(String name, Statement value) {}

    public final List<Pair> values;

    @Override
    public boolean pollutesStack() { return false; }
    @Override
    public void declare(ScopeRecord varsScope) {
        for (var key : values) {
            varsScope.define(key.name());
        }
    }
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        for (var entry : values) {
            if (entry.name() == null) continue;
            var key = scope.getKey(entry.name());
            if (key instanceof String) target.add(Instruction.makeVar((String)key).locate(loc()));

            if (entry.value() instanceof FunctionStatement) {
                ((FunctionStatement)entry.value()).compile(target, scope, entry.name(), false);
                target.add(Instruction.storeVar(key).locate(loc()));
            }
            else if (entry.value() != null) {
                entry.value().compileWithPollution(target, scope);
                target.add(Instruction.storeVar(key).locate(loc()));
            }
        }
    }

    public VariableDeclareStatement(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }
}
