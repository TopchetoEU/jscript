package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;

public class FunctionValueNode extends FunctionNode {
    public final String name;

    @Override public String name() { return name; }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        compile(target, pollute, true, name, null, bp);
    }

    public FunctionValueNode(Location loc, Location end, Parameters params, CompoundNode body, String name) {
        super(loc, end, params, body);
        this.name = name;
    }
}
