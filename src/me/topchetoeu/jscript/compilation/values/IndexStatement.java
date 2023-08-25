package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignStatement;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IndexStatement extends AssignableStatement {
    public final Statement object;
    public final Statement index;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public AssignStatement toAssign(Statement val, Operation operation) {
        return new IndexAssignStatement(loc(), object, index, val, operation);
    }
    public void compile(List<Instruction> target, ScopeRecord scope, boolean dupObj) {
        int start = 0;
        object.compileWithPollution(target, scope);
        if (dupObj) target.add(Instruction.dup().locate(loc()));
        if (index instanceof ConstantStatement) {
            target.add(Instruction.loadMember(((ConstantStatement)index).value).locate(loc()));
            return;
        }

        index.compileWithPollution(target, scope);
        target.add(Instruction.loadMember().locate(loc()));
        target.get(start).setDebug(true);
    }
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        compile(target, scope, false);
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
