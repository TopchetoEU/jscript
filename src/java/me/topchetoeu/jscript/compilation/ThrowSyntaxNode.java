package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class ThrowSyntaxNode extends Node {
    public final String name;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.throwSyntax(name));
    }

    public ThrowSyntaxNode(SyntaxException e) {
        super(e.loc);
        this.name = e.msg;
    }
}
