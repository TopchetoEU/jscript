package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CallStatement extends Statement {
    public final Statement func;
    public final Statement[] args;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (func instanceof IndexStatement) {
            ((IndexStatement)func).compile(target, scope, true);
        }
        else {
            target.add(Instruction.loadValue(null).locate(loc()));
            func.compileWithPollution(target, scope);
        }

        for (var arg : args) {
            arg.compileWithPollution(target, scope);
        }

        target.add(Instruction.call(args.length).locate(loc()).setDebug(true));
    }

    public CallStatement(Location loc, Statement func, Statement ...args) {
        super(loc);
        this.func = func;
        this.args = args;
    }
    public CallStatement(Location loc, Statement obj, Object key, Statement ...args) {
        super(loc);
        this.func = new IndexStatement(loc, obj, new ConstantStatement(loc, key));
        this.args = args;
    }
}
