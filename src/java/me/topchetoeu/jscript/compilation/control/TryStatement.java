package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;

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

    public static ParseRes<TryStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "try")) return ParseRes.failed();
        n += 3;

        var tryBody = CompoundStatement.parse(src, i + n);
        if (!tryBody.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a try body");
        n += tryBody.n;
        n += Parsing.skipEmpty(src, i + n);

        String name = null;
        Statement catchBody = null, finallyBody = null;

        if (Parsing.isIdentifier(src, i + n, "catch")) {
            n += 5;
            n += Parsing.skipEmpty(src, i + n);
            if (src.is(i + n, "(")) {
                n++;
                var nameRes = Parsing.parseIdentifier(src, i + n);
                if (!nameRes.isSuccess()) return nameRes.chainError(src.loc(i + n), "xpected a catch variable name");
                name = nameRes.result;
                n += nameRes.n;
                n += Parsing.skipEmpty(src, i + n);

                if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after catch variable name");
                n++;
            }

            var bodyRes = CompoundStatement.parse(src, i + n);
            if (!bodyRes.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a catch body");
            n += bodyRes.n;
            n += Parsing.skipEmpty(src, i + n);

            catchBody = bodyRes.result;
        }

        if (Parsing.isIdentifier(src, i + n, "finally")) {
            n += 7;

            var bodyRes = CompoundStatement.parse(src, i + n);
            if (!bodyRes.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a finally body");
            n += bodyRes.n;
            n += Parsing.skipEmpty(src, i + n);
            finallyBody = bodyRes.result;
        }

        if (finallyBody == null && catchBody == null) ParseRes.error(src.loc(i + n), "Expected catch or finally");

        return ParseRes.res(new TryStatement(loc, tryBody.result, catchBody, finallyBody, name), n);
    }
}
