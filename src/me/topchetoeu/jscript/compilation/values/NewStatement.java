package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class NewStatement extends Statement {
    public final Statement func;
    public final Statement[] args;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        func.compileWithPollution(target, scope);
        for (var arg : args) {
            arg.compileWithPollution(target, scope);
        }

        target.add(Instruction.callNew(args.length).locate(loc()).setDebug(true));
    }

    public NewStatement(Location loc, Statement func, Statement ...args) {
        super(loc);
        this.func = func;
        this.args = args;
    }
}
