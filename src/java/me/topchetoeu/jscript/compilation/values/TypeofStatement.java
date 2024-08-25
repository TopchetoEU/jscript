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

    public static ParseRes<TypeofStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "typeof")) return ParseRes.failed();
    
        var valRes = Parsing.parseValue(filename, tokens, i + n, 15);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'typeof' keyword.", valRes);
        n += valRes.n;
    
        return ParseRes.res(new TypeofStatement(loc, valRes.result), n);
    }
}
