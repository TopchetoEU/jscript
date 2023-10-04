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
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        func.compile(target, scope, true);

        for (var arg : args) arg.compile(target, scope, true);

        target.add(Instruction.callNew(args.length).locate(loc()).setDebug(true));
    }

    public NewStatement(Location loc, Statement func, Statement ...args) {
        super(loc);
        this.func = func;
        this.args = args;
    }
}
