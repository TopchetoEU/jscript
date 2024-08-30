package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.constants.StringStatement;

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

    public static ParseRes<IndexStatement> parseIndex(Source src, int i, Statement prev, int precedence) {
        if (precedence > 18) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "[")) return ParseRes.failed();
        n++;

        var valRes = ES5.parseExpression(src, i + n, 0);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value in index expression");
        n += valRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "]")) return ParseRes.error(src.loc(i + n), "Expected a closing bracket");
        n++;

        return ParseRes.res(new IndexStatement(loc, prev, valRes.result), n);
    }
    public static ParseRes<IndexStatement> parseMember(Source src, int i, Statement prev, int precedence) {
        if (precedence > 18) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, ".")) return ParseRes.failed();
        n++;

        var literal = Parsing.parseIdentifier(src, i + n);
        if (!literal.isSuccess()) return literal.chainError(src.loc(i + n), "Expected an identifier after member access.");
        n += literal.n;

        return ParseRes.res(new IndexStatement(loc, prev, new StringStatement(loc, literal.result)), n);
    }
}
