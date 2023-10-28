package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IndexStatement extends AssignableStatement {
    public final Statement object;
    public final Statement index;

    @Override
    public boolean pure() { return true; }

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new IndexAssignStatement(loc(), object, index, val, operation);
    }
    public void compile(CompileTarget target, ScopeRecord scope, boolean dupObj, boolean pollute) {
        object.compile(target, scope, true);
        if (dupObj) target.add(Instruction.dup().locate(loc()));
        if (index instanceof ConstantStatement) {
            target.add(Instruction.loadMember(((ConstantStatement)index).value).locate(loc()));
            target.setDebug();
            return;
        }

        index.compile(target, scope, true);
        target.add(Instruction.loadMember().locate(loc()));
        target.setDebug();
        if (!pollute) target.add(Instruction.discard().locate(loc()));
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compile(target, scope, false, pollute);
    }

    public IndexStatement(Location loc, Statement object, Statement index) {
        super(loc);
        this.object = object;
        this.index = index;
    }
    public IndexStatement(Location loc, Statement object, Object index) {
        super(loc);
        this.object = object;
        this.index = new ConstantStatement(loc, index);
    }
}
