package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ConstantStatement extends Statement {
    public final Object value;
    public final boolean isNull;

    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (pollute) {
            if (isNull) target.add(Instruction.pushNull());
            else if (value instanceof Double) target.add(Instruction.pushValue((Double)value));
            else if (value instanceof String) target.add(Instruction.pushValue((String)value));
            else if (value instanceof Boolean) target.add(Instruction.pushValue((Boolean)value));
            else target.add(Instruction.pushUndefined());
        }
    }

    private ConstantStatement(Location loc, Object val, boolean isNull) {
        super(loc);
        this.value = val;
        this.isNull = isNull;
    }

    public ConstantStatement(Location loc, boolean val) {
        this(loc, val, false);
    }
    public ConstantStatement(Location loc, String val) {
        this(loc, val, false);
    }
    public ConstantStatement(Location loc, double val) {
        this(loc, val, false);
    }

    public static ConstantStatement ofUndefined(Location loc) {
        return new ConstantStatement(loc, null, false);
    }
    public static ConstantStatement ofNull(Location loc) {
        return new ConstantStatement(loc, null, true);
    }
}
