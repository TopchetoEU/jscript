package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.destructing.AssignTarget;
import me.topchetoeu.jscript.compilation.destructing.Destructor;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class AssignNode extends Node implements Destructor {
    public final AssignTarget assignable;
    public final Node value;

    @Override public void destructDeclResolve(CompileResult target) {
        if (!(assignable instanceof VariableNode var)) {
            throw new SyntaxException(loc(), "Assign target in declaration destructor must be a variable");
        }

        target.scope.define(new Variable(var.name, false), var.loc());
    }

    @Override public void destructArg(CompileResult target) {
        if (!(assignable instanceof VariableNode var)) {
            throw new SyntaxException(loc(), "Assign target in declaration destructor must be a variable");
        }

        var v = target.scope.define(new Variable(var.name, false), var.loc());
        afterAssign(target, null, false);
        target.add(_i -> v.index().toSet(false));
    }
    @Override public void afterAssign(CompileResult target, DeclarationType decl, boolean pollute) {
        if (decl != null && decl.strict) {
            if (!(assignable instanceof VariableNode var)) {
                throw new SyntaxException(loc(), "Assign target in declaration destructor must be a variable");
            }
            target.scope.define(new Variable(var.name, decl.strict), var.loc());
        }

        assignable.beforeAssign(target, decl);

        target.add(Instruction.dup());
        target.add(Instruction.pushUndefined());
        target.add(Instruction.operation(Operation.EQUALS));
        var start = target.temp();
        target.add(Instruction.discard());

        value.compile(target, true);

        target.set(start, Instruction.jmp(target.size() - start));

        assignable.afterAssign(target, decl, pollute);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        assignable.beforeAssign(target, null);
        value.compile(target, true);
        assignable.afterAssign(target, null, pollute);
    }

    public AssignNode(Location loc, AssignTarget assignable, Node value) {
        super(loc);
        this.assignable = assignable;
        this.value = value;
    }
}
