package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class VariableStatement extends AssignableStatement {
    public final String name;

    @Override public boolean pure() { return false; }

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new VariableAssignStatement(loc(), name, val, operation);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.getKey(name);
        target.add(Instruction.loadVar(i));
        if (!pollute) target.add(Instruction.discard());
    }

    public VariableStatement(Location loc, String name) {
        super(loc);
        this.name = name;
    }

    public static ParseRes<VariableStatement> parseVariable(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var literal = Parsing.parseIdentifier(tokens, i);
    
        if (!literal.isSuccess()) return ParseRes.failed();
    
        if (!Parsing.checkVarName(literal.result)) {
            if (literal.result.equals("await")) return ParseRes.error(loc, "'await' expressions are not supported.");
            if (literal.result.equals("const")) return ParseRes.error(loc, "'const' declarations are not supported.");
            if (literal.result.equals("let")) return ParseRes.error(loc, "'let' declarations are not supported.");
            return ParseRes.error(loc, String.format("Unexpected identifier '%s'.", literal.result));
        }
    
        return ParseRes.res(new VariableStatement(loc, literal.result), 1);
    }
}
