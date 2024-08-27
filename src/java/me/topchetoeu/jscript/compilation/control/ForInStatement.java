package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class ForInStatement extends Statement {
    public final String varName;
    public final boolean isDeclaration;
    public final Statement object, body;
    public final String label;
    public final Location varLocation;

    @Override public void declare(CompileResult target) {
        body.declare(target);
        if (isDeclaration) target.scope.define(varName);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        var key = target.scope.getKey(varName);

        if (key instanceof String) target.add(Instruction.makeVar((String)key));

        object.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.keys(true));

        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.pushUndefined());
        target.add(Instruction.operation(Operation.EQUALS));
        int mid = target.temp();

        target.add(Instruction.pushValue("value")).setLocation(varLocation);
        target.add(Instruction.loadMember()).setLocation(varLocation);
        target.add(Instruction.storeVar(key)).setLocationAndDebug(object.loc(), BreakpointType.STEP_OVER);

        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIf(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForInStatement(Location loc, Location varLocation, String label, boolean isDecl, String varName, Statement object, Statement body) {
        super(loc);
        this.varLocation = varLocation;
        this.label = label;
        this.isDeclaration = isDecl;
        this.varName = varName;
        this.object = object;
        this.body = body;
    }

    public static ParseRes<ForInStatement> parse(Source src, int i) {
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
        if (!name.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a variable name for for-in loop");
        var nameLoc = src.loc(i + n);
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "in")) return ParseRes.error(src.loc(i + n), "Expected 'in' keyword after variable declaration");
        n += 2;

        var obj = Parsing.parseValue(src, i + n, 0);
        if (!obj.isSuccess()) return obj.chainError(src.loc(i + n), "Expected a value");
        n += obj.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren");
        n++;

        var bodyRes = Parsing.parseStatement(src, i + n);
        if (!bodyRes.isSuccess()) return bodyRes.chainError(src.loc(i + n), "Expected a for-in body");
        n += bodyRes.n;

        return ParseRes.res(new ForInStatement(loc, nameLoc, label.result, isDecl, name.result, obj.result, bodyRes.result), n);
    }
}
