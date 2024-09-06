package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.AssignableNode;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class AssignNode extends Node {
    public final AssignableNode assignable;
    public final Node value;

    @Override public void compile(CompileResult target, boolean pollute) {
        assignable.compileBeforeAssign(target, false);
        value.compile(target, true);
        assignable.compileAfterAssign(target, false, pollute);
    }

    public AssignNode(Location loc, AssignableNode assignable, Node value) {
        super(loc);
        this.assignable = assignable;
        this.value = value;
    }
}
