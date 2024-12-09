package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class NullNode extends Node {
	@Override public void compileFunctions(CompileResult target) {
	}

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushNull());
    }

    public NullNode(Location loc) { super(loc); }
}
