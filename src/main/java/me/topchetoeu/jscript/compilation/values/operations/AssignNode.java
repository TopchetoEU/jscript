package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.patterns.AssignTarget;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class AssignNode extends Node implements AssignTarget {
    public final AssignTarget assignable;
    public final Node value;

    @Override public void compile(CompileResult target, boolean pollute) {
        if (assignable instanceof AssignNode other) throw new SyntaxException(other.loc(), "Assign deconstructor not allowed here");

        assignable.beforeAssign(target);
        value.compile(target, true);
        assignable.afterAssign(target, pollute);
    }

    @Override public void afterAssign(CompileResult target, boolean pollute) {
        if (assignable instanceof AssignNode other) throw new SyntaxException(other.loc(), "Double assign deconstructor not allowed");

        if (pollute) target.add(Instruction.dup(2, 0));
        else target.add(Instruction.dup());
        target.add(Instruction.pushUndefined());
        target.add(Instruction.operation(Operation.EQUALS));
        var start = target.temp();
        target.add(Instruction.discard());

        value.compile(target, true);

        target.set(start, Instruction.jmpIfNot(target.size() - start));

        assignable.assign(target, false);
        if (!pollute) target.add(Instruction.discard());
    }

    public AssignNode(Location loc, AssignTarget assignable, Node value) {
        super(loc);
        this.assignable = assignable;
        this.value = value;
    }
}
