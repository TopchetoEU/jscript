package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

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

    public static ParseRes<ConstantStatement> parseNumber(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        if (Parsing.inBounds(tokens, i)) {
            if (tokens.get(i).isNumber()) {
                return ParseRes.res(new ConstantStatement(loc, tokens.get(i).number()), 1);
            }
            else return ParseRes.failed();
        }
        else return ParseRes.failed();
    }
    public static ParseRes<ConstantStatement> parseString(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        if (Parsing.inBounds(tokens, i)) {
            if (tokens.get(i).isString()) {
                return ParseRes.res(new ConstantStatement(loc, tokens.get(i).string()), 1);
            }
            else return ParseRes.failed();
        }
        else return ParseRes.failed();
    }
}
