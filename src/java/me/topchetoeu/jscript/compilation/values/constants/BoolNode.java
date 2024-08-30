package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class BoolNode extends Node {
    public final boolean value;

    @Override public boolean pure() { return true; }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushValue(value));
    }

    public BoolNode(Location loc, boolean value) {
        super(loc);
        this.value = value;
    }
}
