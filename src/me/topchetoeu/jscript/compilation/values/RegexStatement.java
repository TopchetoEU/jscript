package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        target.add(Instruction.loadRegex(pattern, flags).locate(loc()));
    }

    public RegexStatement(Location loc, String pattern, String flags) {
        super(loc);
        this.pattern = pattern;
        this.flags = flags;
    }
}
