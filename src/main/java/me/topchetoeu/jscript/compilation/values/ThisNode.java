package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;


public class ThisNode extends Node {
    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.loadThis());
    }

    public ThisNode(Location loc) {
        super(loc);
    }
}
