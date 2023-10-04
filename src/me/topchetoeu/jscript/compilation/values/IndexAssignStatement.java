package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IndexAssignStatement extends Statement {
    public final Statement object;
    public final Statement index;
    public final Statement value;
    public final Operation operation;

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        if (operation != null) {
            object.compile(target, scope, true);
            index.compile(target, scope, true);
            target.add(Instruction.dup(2, 0).locate(loc()));

            target.add(Instruction.loadMember().locate(loc()));
            value.compile(target, scope, true);
            target.add(Instruction.operation(operation).locate(loc()));

            target.add(Instruction.storeMember(pollute).locate(loc()).setDebug(true));
        }
        else {
            object.compile(target, scope, true);
            index.compile(target, scope, true);
            value.compile(target, scope, true);

            target.add(Instruction.storeMember(pollute).locate(loc()).setDebug(true));
        }
    }

    public IndexAssignStatement(Location loc, Statement object, Statement index, Statement value, Operation operation) {
        super(loc);
        this.object = object;
        this.index = index;
        this.value = value;
        this.operation = operation;
    }
}
