package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class IndexStatement extends AssignableStatement {
    public final Statement object;
    public final Statement index;

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new IndexAssignStatement(loc(), object, index, val, operation);
    }
    public void compile(CompileResult target, boolean dupObj, boolean pollute) {
        object.compile(target, true);
        if (dupObj) target.add(Instruction.dup());

        index.compile(target, true);
        target.add(Instruction.loadMember()).setLocationAndDebug(loc(), BreakpointType.STEP_IN);
        if (!pollute) target.add(Instruction.discard());
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        compile(target, false, pollute);
    }

    public IndexStatement(Location loc, Statement object, Statement index) {
        super(loc);
        this.object = object;
        this.index = index;
    }

    public static ParseRes<IndexStatement> parseIndex(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;
    
        if (precedence > 18) return ParseRes.failed();
    
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACKET_OPEN)) return ParseRes.failed();
    
        var valRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value in index expression.", valRes);
        n += valRes.n;
    
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACKET_CLOSE)) return ParseRes.error(loc, "Expected a closing bracket.");
    
        return ParseRes.res(new IndexStatement(loc, prev, valRes.result), n);
    }
    public static ParseRes<IndexStatement> parseMember(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;
    
        if (precedence > 18) return ParseRes.failed();
    
        if (!Parsing.isOperator(tokens, i + n++, Operator.DOT)) return ParseRes.failed();
    
        var literal = Parsing.parseIdentifier(tokens, i + n++);
        if (!literal.isSuccess()) return ParseRes.error(loc, "Expected an identifier after member access.");
    
        return ParseRes.res(new IndexStatement(loc, prev, new ConstantStatement(loc, literal.result)), n);
    }
}
