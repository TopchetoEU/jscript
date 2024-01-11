package me.topchetoeu.jscript.core.compilation;

import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.core.exceptions.SyntaxException;

public class ThrowSyntaxStatement extends Statement {
    public final String name;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.throwSyntax(loc(), name));
    }

    public ThrowSyntaxStatement(SyntaxException e) {
        super(e.loc);
        this.name = e.msg;
    }
}
