package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
import me.topchetoeu.jscript.compilation.Node;

public class TryNode extends Node {
    public final CompoundNode tryBody;
    public final CompoundNode catchBody;
    public final CompoundNode finallyBody;
    public final String captureName;
    public final String label;

    @Override public void resolve(CompileResult target) {
        tryBody.resolve(target);
        catchBody.resolve(target);
        finallyBody.resolve(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType bpt) {
        int replace = target.temp();
        var endSuppl = new DeferredIntSupplier();

        int start = replace + 1, catchStart = -1, finallyStart = -1;

        LabelContext.getBreak(target.env).push(loc(), label, endSuppl);

        tryBody.compile(target, false);
        target.add(Instruction.tryEnd());

        if (catchBody != null) {
            catchStart = target.size() - start;

            if (captureName != null) {
                var subtarget = target.subtarget();
                subtarget.scope.defineStrict(captureName, false, catchBody.loc());
                catchBody.compile(subtarget, false);
                subtarget.scope.end();
            }
            else catchBody.compile(target, false);

            target.add(Instruction.tryEnd());
        }

        if (finallyBody != null) {
            finallyStart = target.size() - start;
            finallyBody.compile(target, false);
        }

        LabelContext.getBreak(target.env).pop(label);

        endSuppl.set(target.size());

        target.set(replace, Instruction.tryStart(catchStart, finallyStart, target.size() - start));
        target.setLocationAndDebug(replace, loc(), BreakpointType.STEP_OVER);

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public TryNode(Location loc, String label, CompoundNode tryBody, CompoundNode catchBody, CompoundNode finallyBody, String captureName) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.captureName = captureName;
        this.label = label;
    }

    public static ParseRes<TryNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = JavaScript.parseLabel(src, i + n);
        n += labelRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "try")) return ParseRes.failed();
        n += 3;

        var tryBody = CompoundNode.parse(src, i + n);
        if (!tryBody.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a try body");
        n += tryBody.n;
        n += Parsing.skipEmpty(src, i + n);

        String capture = null;
        CompoundNode catchBody = null, finallyBody = null;

        if (Parsing.isIdentifier(src, i + n, "catch")) {
            n += 5;
            n += Parsing.skipEmpty(src, i + n);
            if (src.is(i + n, "(")) {
                n++;
                var nameRes = Parsing.parseIdentifier(src, i + n);
                if (!nameRes.isSuccess()) return nameRes.chainError(src.loc(i + n), "xpected a catch variable name");
                capture = nameRes.result;
                n += nameRes.n;
                n += Parsing.skipEmpty(src, i + n);

                if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after catch variable name");
                n++;
            }

            var bodyRes = CompoundNode.parse(src, i + n);
            if (!bodyRes.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a catch body");
            n += bodyRes.n;
            n += Parsing.skipEmpty(src, i + n);

            catchBody = bodyRes.result;
        }

        if (Parsing.isIdentifier(src, i + n, "finally")) {
            n += 7;

            var bodyRes = CompoundNode.parse(src, i + n);
            if (!bodyRes.isSuccess()) return tryBody.chainError(src.loc(i + n), "Expected a finally body");
            n += bodyRes.n;
            n += Parsing.skipEmpty(src, i + n);
            finallyBody = bodyRes.result;
        }

        if (finallyBody == null && catchBody == null) ParseRes.error(src.loc(i + n), "Expected catch or finally");

        return ParseRes.res(new TryNode(loc, labelRes.result, tryBody.result, catchBody, finallyBody, capture), n);
    }
}
