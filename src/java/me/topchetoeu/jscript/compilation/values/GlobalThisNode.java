package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class GlobalThisNode extends Node {
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.loadGlob());
    }

    public GlobalThisNode(Location loc) {
        super(loc);
    }
}
