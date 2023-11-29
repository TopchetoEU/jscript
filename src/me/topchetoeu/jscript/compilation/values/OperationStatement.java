package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.control.ThrowStatement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class OperationStatement extends Statement {
    public final Statement[] args;
    public final Operation operation;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        for (var arg : args) {
            arg.compile(target, scope, true);
        }

        if (pollute) target.add(Instruction.operation(loc(), operation));
        else target.add(Instruction.discard(loc()));
    }

    @Override
    public boolean pure() {
        for (var arg : args) {
            if (!arg.pure()) return false;
        }
        return true;
    }

    @Override
    public Statement optimize() {
        var args = new Statement[this.args.length];
        var allConst = true;

        for (var i = 0; i < this.args.length; i++) {
            args[i] = this.args[i].optimize();
            if (!(args[i] instanceof ConstantStatement)) allConst = false;
        }

        if (allConst) {
            var vals = new Object[this.args.length];

            for (var i = 0; i < args.length; i++) {
                vals[i] = ((ConstantStatement)args[i]).value;
            }

            try { return new ConstantStatement(loc(), Values.operation(null, operation, vals)); }
            catch (EngineException e) { return new ThrowStatement(loc(), new ConstantStatement(loc(), e.value)); }
        }

        return new OperationStatement(loc(), operation, args);

    }

    public OperationStatement(Location loc, Operation operation, Statement ...args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }
}
