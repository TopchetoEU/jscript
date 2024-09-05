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
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class ForOfNode extends Node {
    public final String varName;
    public final DeclarationType declType;
    public final Node iterable, body;
    public final String label;
    public final Location varLocation;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
        if (declType != null && !declType.strict) target.scope.define(new Variable(varName, false), varLocation);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (declType != null && declType.strict) target.scope.defineStrict(new Variable(varName, declType.readonly), varLocation);

        iterable.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.dup());
        target.add(Instruction.loadIntrinsics("it_key"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());

        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.dup());
        target.add(Instruction.loadMember("next")).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());
        target.add(Instruction.dup());
        target.add(Instruction.loadMember("done")).setLocation(iterable.loc());
        int mid = target.temp();

        target.add(Instruction.loadMember("value")).setLocation(varLocation);
        target.add(VariableNode.toSet(target, varLocation, varName, false, declType != null && declType.strict));

        var end = new DeferredIntSupplier();

        LabelContext.pushLoop(target.env, loc(), label, end, start);
        CompoundNode.compileMultiEntry(body, target, false, BreakpointType.STEP_OVER);
        LabelContext.popLoop(target.env, label);

        int endI = target.size();
        end.set(endI);

        target.add(Instruction.jmp(start - endI));
        target.add(Instruction.discard());
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIf(endI - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForOfNode(Location loc, Location varLocation, String label, DeclarationType declType, String varName, Node object, Node body) {
        super(loc);
        this.varLocation = varLocation;
        this.label = label;
        this.declType = declType;
        this.varName = varName;
        this.iterable = object;
        this.body = body;
    }

    public static ParseRes<ForOfNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var label = JavaScript.parseLabel(src, i + n);
        n += label.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "for")) return ParseRes.failed();
        n += 3;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected an opening paren");
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var declType = JavaScript.parseDeclarationType(src, i + n);
        n += declType.n;

        var name = Parsing.parseIdentifier(src, i + n);
        if (!name.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a variable name for for-of loop");
        var nameLoc = src.loc(i + n);
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "of")) return ParseRes.error(src.loc(i + n), "Expected 'of' keyword after variable declaration");
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

        return ParseRes.res(new ForOfNode(loc, nameLoc, label.result, declType.result, name.result, obj.result, bodyRes.result), n);
    }
}
