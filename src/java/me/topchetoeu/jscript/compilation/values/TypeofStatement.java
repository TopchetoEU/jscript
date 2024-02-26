package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class TypeofStatement extends Statement {
    public final Statement value;

    // Not really pure, since a variable from the global scope could be accessed,
    // which could lead to code execution, that would get omitted
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (value instanceof VariableStatement) {
            var i = target.scope.getKey(((VariableStatement)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof((String)i));
                return;
            }
        }
        value.compile(target, pollute);
        target.add(Instruction.typeof());
    }

    public TypeofStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
