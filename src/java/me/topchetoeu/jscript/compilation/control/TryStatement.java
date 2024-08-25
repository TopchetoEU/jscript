package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class TryStatement extends Statement {
    public final Statement tryBody;
    public final Statement catchBody;
    public final Statement finallyBody;
    public final String name;

    @Override public void declare(CompileResult target) {
        tryBody.declare(target);
        if (catchBody != null) catchBody.declare(target);
        if (finallyBody != null) finallyBody.declare(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType bpt) {
        int replace = target.temp();

        int start = replace + 1, catchStart = -1, finallyStart = -1;

        tryBody.compile(target, false);
        target.add(Instruction.tryEnd());

        if (catchBody != null) {
            catchStart = target.size() - start;
            target.scope.define(name, true);
            catchBody.compile(target, false);
            target.scope.undefine();
            target.add(Instruction.tryEnd());
        }

        if (finallyBody != null) {
            finallyStart = target.size() - start;
            finallyBody.compile(target, false);
            target.add(Instruction.tryEnd());
        }

        target.set(replace, Instruction.tryStart(catchStart, finallyStart, target.size() - start));
        target.setLocationAndDebug(replace, loc(), BreakpointType.STEP_OVER);

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public TryStatement(Location loc, Statement tryBody, Statement catchBody, Statement finallyBody, String name) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.name = name;
    }

    public static ParseRes<TryStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (!Parsing.isIdentifier(tokens, i + n++, "try")) return ParseRes.failed();

        var res = Parsing.parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected an if body.", res);
        n += res.n;

        String name = null;
        Statement catchBody = null, finallyBody = null;
        

        if (Parsing.isIdentifier(tokens, i + n, "catch")) {
            n++;
            if (Parsing.isOperator(tokens, i + n, Operator.PAREN_OPEN)) {
                n++;
                var nameRes = Parsing.parseIdentifier(tokens, i + n++);
                if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a catch variable name.");
                name = nameRes.result;
                if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after catch variable name.");
            }

            var catchRes = Parsing.parseStatement(filename, tokens, i + n);
            if (!catchRes.isSuccess()) return ParseRes.error(loc, "Expected a catch body.", catchRes);
            n += catchRes.n;
            catchBody = catchRes.result;
        }

        if (Parsing.isIdentifier(tokens, i + n, "finally")) {
            n++;
            var finallyRes = Parsing.parseStatement(filename, tokens, i + n);
            if (!finallyRes.isSuccess()) return ParseRes.error(loc, "Expected a finally body.", finallyRes);
            n += finallyRes.n;
            finallyBody = finallyRes.result;
        }

        return ParseRes.res(new TryStatement(loc, res.result, catchBody, finallyBody, name), n);
    }
}
