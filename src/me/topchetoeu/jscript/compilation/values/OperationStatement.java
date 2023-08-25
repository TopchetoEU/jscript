package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.control.ThrowStatement;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class OperationStatement extends Statement {
    public final Statement[] args;
    public final Operation operation;

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        for (var arg : args) {
            arg.compileWithPollution(target, scope);
        }
        target.add(Instruction.operation(operation).locate(loc()));
    }

    @Override
    public boolean pollutesStack() { return true; }
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

            try {
                var ctx = new CallContext(null);
    
                switch (operation) {
                    case ADD: return new ConstantStatement(loc(), Values.add(ctx, vals[0], vals[1]));
                    case SUBTRACT: return new ConstantStatement(loc(), Values.subtract(ctx, vals[0], vals[1]));
                    case DIVIDE: return new ConstantStatement(loc(), Values.divide(ctx, vals[0], vals[1]));
                    case MULTIPLY: return new ConstantStatement(loc(), Values.multiply(ctx, vals[0], vals[1]));
                    case MODULO: return new ConstantStatement(loc(), Values.modulo(ctx, vals[0], vals[1]));

                    case AND: return new ConstantStatement(loc(), Values.and(ctx, vals[0], vals[1]));
                    case OR: return new ConstantStatement(loc(), Values.or(ctx, vals[0], vals[1]));
                    case XOR: return new ConstantStatement(loc(), Values.xor(ctx, vals[0], vals[1]));

                    case EQUALS: return new ConstantStatement(loc(), Values.strictEquals(vals[0], vals[1]));
                    case NOT_EQUALS: return new ConstantStatement(loc(), !Values.strictEquals(vals[0], vals[1]));
                    case LOOSE_EQUALS: return new ConstantStatement(loc(), Values.looseEqual(ctx, vals[0], vals[1]));
                    case LOOSE_NOT_EQUALS: return new ConstantStatement(loc(), !Values.looseEqual(ctx, vals[0], vals[1]));

                    case GREATER: return new ConstantStatement(loc(), Values.compare(ctx, vals[0], vals[1]) < 0);
                    case GREATER_EQUALS: return new ConstantStatement(loc(), Values.compare(ctx, vals[0], vals[1]) <= 0);
                    case LESS: return new ConstantStatement(loc(), Values.compare(ctx, vals[0], vals[1]) > 0);
                    case LESS_EQUALS: return new ConstantStatement(loc(), Values.compare(ctx, vals[0], vals[1]) >= 0);

                    case INVERSE: return new ConstantStatement(loc(), Values.bitwiseNot(ctx, vals[0]));
                    case NOT: return new ConstantStatement(loc(), Values.not(vals[0]));
                    case POS: return new ConstantStatement(loc(), Values.toNumber(ctx, vals[0]));
                    case NEG: return new ConstantStatement(loc(), Values.negative(ctx, vals[0]));

                    case SHIFT_LEFT: return new ConstantStatement(loc(), Values.shiftLeft(ctx, vals[0], vals[1]));
                    case SHIFT_RIGHT: return new ConstantStatement(loc(), Values.shiftRight(ctx, vals[0], vals[1]));
                    case USHIFT_RIGHT: return new ConstantStatement(loc(), Values.unsignedShiftRight(ctx, vals[0], vals[1]));

                    default: break;
                }
            }
            catch (EngineException e) {
                return new ThrowStatement(loc(), new ConstantStatement(loc(), e.value));
            }
            catch (InterruptedException e) { return null; }
        }

        return new OperationStatement(loc(), operation, args);

    }

    public OperationStatement(Location loc, Operation operation, Statement... args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }
}
