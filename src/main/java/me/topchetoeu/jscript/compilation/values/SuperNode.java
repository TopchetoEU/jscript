package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;


public class SuperNode extends Node {
    @Override public void compile(CompileResult target, boolean pollute) {
        throw new SyntaxException(loc(), "Unexpected 'super' reference here");
    }

    public SuperNode(Location loc) {
        super(loc);
    }
}
