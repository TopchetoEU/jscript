package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class VariableIndexNode extends Node {
    public final int index;

    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.loadVar(index));
    }

    public VariableIndexNode(Location loc, int i) {
        super(loc);
        this.index = i;
    }
}
