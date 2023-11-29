package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    @Override
    public boolean pure() { return true; }

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
