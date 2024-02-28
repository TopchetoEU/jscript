package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    // Not really pure, since a function is called, but can be ignored.
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadRegex(pattern, flags));
        if (!pollute) target.add(Instruction.discard());
    }

    public RegexStatement(Location loc, String pattern, String flags) {
        super(loc);
        this.pattern = pattern;
        this.flags = flags;
    }
}
