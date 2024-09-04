package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class ThrowSyntaxStatement extends Statement {
    public final String name;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.throwSyntax(name));
    }

    public ThrowSyntaxStatement(SyntaxException e) {
        super(e.loc);
        this.name = e.msg;
    }
}
