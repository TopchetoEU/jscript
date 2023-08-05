package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Symbol;

public class TypeofStatement extends Statement {
    public final Statement value;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (value instanceof VariableStatement) {
            var i = scope.getKey(((VariableStatement)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof((String)i));
                return;
            }
        }
        value.compileWithPollution(target, scope);
        target.add(Instruction.typeof().locate(loc()));
    }

    @Override
    public Statement optimize() {
        var val = value.optimize();

        if (val instanceof ConstantStatement) {
            var cnst = (ConstantStatement)val;
            if (cnst.value == null) return new ConstantStatement(loc(), "undefined");
            if (cnst.value instanceof Number) return new ConstantStatement(loc(), "number");
            if (cnst.value instanceof Boolean) return new ConstantStatement(loc(), "boolean");
            if (cnst.value instanceof String) return new ConstantStatement(loc(), "string");
            if (cnst.value instanceof Symbol) return new ConstantStatement(loc(), "symbol");
            if (cnst.value instanceof FunctionValue) return new ConstantStatement(loc(), "function");
            return new ConstantStatement(loc(), "object");
        }

        return new TypeofStatement(loc(), val);
    }

    public TypeofStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
