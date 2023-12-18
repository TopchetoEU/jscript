package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class TypeofStatement extends Statement {
    public final Statement value;

    // Not really pure, since a variable from the global scope could be accessed,
    // which could lead to code execution, that would get omitted
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (value instanceof VariableStatement) {
            var i = scope.getKey(((VariableStatement)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof(loc(), (String)i));
                return;
            }
        }
        value.compile(target, scope, pollute);
        target.add(Instruction.typeof(loc()));
    }

    public TypeofStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
