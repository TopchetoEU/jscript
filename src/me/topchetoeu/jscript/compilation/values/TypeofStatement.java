package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.control.ArrayStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class TypeofStatement extends Statement {
    public final Statement value;

    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (value instanceof VariableStatement) {
            var i = scope.getKey(((VariableStatement)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof((String)i).locate(loc()));
                return;
            }
        }
        value.compile(target, scope, pollute);
        target.add(Instruction.typeof().locate(loc()));
    }

    @Override
    public Statement optimize() {
        var val = value.optimize();

        if (val instanceof ConstantStatement) {
            return new ConstantStatement(loc(), Values.type(((ConstantStatement)val).value));
        }
        else if (
            val instanceof ObjectStatement ||
            val instanceof ArrayStatement ||
            val instanceof GlobalThisStatement
        ) return new ConstantStatement(loc(), "object");
        else if(val instanceof FunctionStatement) return new ConstantStatement(loc(), "function");

        return new TypeofStatement(loc(), val);
    }

    public TypeofStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
