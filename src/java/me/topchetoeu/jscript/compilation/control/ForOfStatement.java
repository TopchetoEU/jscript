package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;

public class ForOfStatement extends Statement {
    public final String varName;
    public final boolean isDeclaration;
    public final Statement iterable, body;
    public final String label;
    public final Location varLocation;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
        if (isDeclaration) target.scope.define(varName);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var key = target.scope.getKey(varName);

        if (key instanceof String) target.add(Instruction.makeVar((String)key));

        iterable.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.dup());
        target.add(Instruction.loadVar("Symbol"));
        target.add(Instruction.pushValue("iterator"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());

        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.dup());
        target.add(Instruction.pushValue("next"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());
        target.add(Instruction.dup());
        target.add(Instruction.pushValue("done"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        int mid = target.temp();

        target.add(Instruction.pushValue("value"));
        target.add(Instruction.loadMember()).setLocation(varLocation);
        target.add(Instruction.storeVar(key)).setLocationAndDebug(iterable.loc(), BreakpointType.STEP_OVER);

        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.add(Instruction.discard());
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIf(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForOfStatement(Location loc, Location varLocation, String label, boolean isDecl, String varName, Statement object, Statement body) {
        super(loc);
        this.varLocation = varLocation;
        this.label = label;
        this.isDeclaration = isDecl;
        this.varName = varName;
        this.iterable = object;
        this.body = body;
    }

    public static ParseRes<ForOfStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var label = WhileStatement.parseLabel(src, i + n);
        n += label.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "for")) return ParseRes.failed();
        n += 3;
        n += Parsing.skipEmpty(src, i + n);

        var isDecl = false;

        if (Parsing.isIdentifier(src, i + n, "var")) {
            isDecl = true;
            n += 3;
        }

        var name = Parsing.parseIdentifier(src, i + n);
        if (!name.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a variable name for for-of loop");
        var nameLoc = src.loc(i + n);
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "fo")) return ParseRes.error(src.loc(i + n), "Expected 'of' keyword after variable declaration");
        n += 2;

        var obj = JavaScript.parseExpression(src, i + n, 0);
        if (!obj.isSuccess()) return obj.chainError(src.loc(i + n), "Expected a value");
        n += obj.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren");
        n++;

        var bodyRes = JavaScript.parseStatement(src, i + n);
        if (!bodyRes.isSuccess()) return bodyRes.chainError(src.loc(i + n), "Expected a for-of body");
        n += bodyRes.n;

        return ParseRes.res(new ForOfStatement(loc, nameLoc, label.result, isDecl, name.result, obj.result, bodyRes.result), n);
    }
}
