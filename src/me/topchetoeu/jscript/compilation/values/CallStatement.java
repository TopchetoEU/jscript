package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CallStatement extends Statement {
    public final Statement func;
    public final Statement[] args;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (func instanceof IndexStatement) {
            ((IndexStatement)func).compile(target, scope, true, true);
        }
        else {
            target.add(Instruction.loadValue(null).locate(loc()));
            func.compile(target, scope, true);
        }

        for (var arg : args) arg.compile(target, scope, true);

        target.add(Instruction.call(args.length).locate(loc()));
        target.setDebug();
        if (!pollute) target.add(Instruction.discard().locate(loc()));
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
