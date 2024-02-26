package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class LazyOrStatement extends Statement {
    public final Statement first, second;

    @Override public boolean pure() { return first.pure() && second.pure(); }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        first.compile(target, true);
        if (pollute) target.add(Instruction.dup());
        int start = target.temp();
        if (pollute) target.add(Instruction.discard());
        second.compile(target, pollute);
        target.set(start, Instruction.jmpIf(target.size() - start));
    }

    public LazyOrStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
