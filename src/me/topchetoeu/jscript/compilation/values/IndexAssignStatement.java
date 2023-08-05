package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IndexAssignStatement extends Statement {
    public final Statement object;
    public final Statement index;
    public final Statement value;
    public final Type operation;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        int start = 0;
        if (operation != null) {
            object.compileWithPollution(target, scope);
            index.compileWithPollution(target, scope);

            target.add(Instruction.dup(1, 1).locate(loc()));
            target.add(Instruction.dup(1, 1).locate(loc()));
            target.add(Instruction.loadMember().locate(loc()));
            value.compileWithPollution(target, scope);
            target.add(Instruction.operation(operation).locate(loc()));

            target.add(Instruction.storeMember(true).locate(loc()));
        }
        else {
            object.compileWithPollution(target, scope);
            index.compileWithPollution(target, scope);
            value.compileWithPollution(target, scope);
            target.add(Instruction.storeMember(true).locate(loc()));
        }
        target.get(start).setDebug(true);
    }

    public IndexAssignStatement(Location loc, Statement object, Statement index, Statement value, Type operation) {
        super(loc);
        this.object = object;
        this.index = index;
        this.value = value;
        this.operation = operation;
    }
}
