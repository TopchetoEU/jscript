package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CallStatement extends Statement {
    public final Statement func;
    public final Statement[] args;
    public final boolean isNew;

    @Override
    public void compileWithDebug(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType type) {
        if (isNew) func.compile(target, scope, true);
        else if (func instanceof IndexStatement) {
            ((IndexStatement)func).compile(target, scope, true, true);
        }
        else {
            target.add(Instruction.loadValue(loc(), null));
            func.compile(target, scope, true);
        }

        for (var arg : args) arg.compile(target, scope, true);

        target.queueDebug(type);
        if (isNew) target.add(Instruction.callNew(loc(), args.length));
        else target.add(Instruction.call(loc(), args.length));

        if (!pollute) target.add(Instruction.discard(loc()));
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compileWithDebug(target, scope, pollute, BreakpointType.STEP_IN);
    }

    public CallStatement(Location loc, boolean isNew, Statement func, Statement ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = func;
        this.args = args;
    }
    public CallStatement(Location loc, boolean isNew, Statement obj, Object key, Statement ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = new IndexStatement(loc, obj, new ConstantStatement(loc, key));
        this.args = args;
    }
}
