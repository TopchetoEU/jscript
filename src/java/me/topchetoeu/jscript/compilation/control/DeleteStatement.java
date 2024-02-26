package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class DeleteStatement extends Statement {
    public final Statement key;
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        key.compile(target, true);

        target.add(Instruction.delete());
        if (pollute) target.add(Instruction.pushValue(true));
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
