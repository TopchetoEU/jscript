package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.VariableStatement;

public class TypeofStatement extends Statement {
    public final Statement value;

    // Not really pure, since a variable from the global scope could be accessed,
    // which could lead to code execution, that would get omitted
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (value instanceof VariableStatement) {
            var i = target.scope.getKey(((VariableStatement)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof((String)i));
                return;
            }
        }
        value.compile(target, pollute);
        target.add(Instruction.typeof());
    }

    public TypeofStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<TypeofStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "typeof")) return ParseRes.failed();
        n += 6;

        var valRes = ES5.parseExpression(src, i + n, 15);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'typeof' keyword.");
        n += valRes.n;
    
        return ParseRes.res(new TypeofStatement(loc, valRes.result), n);
    }
}
