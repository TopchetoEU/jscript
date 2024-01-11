package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    // Not really pure, since a function is called, but can be ignored.
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.loadRegex(loc(), pattern, flags));
        if (!pollute) target.add(Instruction.discard(loc()));
    }

    public RegexStatement(Location loc, String pattern, String flags) {
        super(loc);
        this.pattern = pattern;
        this.flags = flags;
    }
}
