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
import me.topchetoeu.jscript.compilation.patterns.Binding;

public class ForOfNode extends Node {
    public final Binding binding;
    public final Node iterable, body;
    public final String label;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
        binding.resolve(target);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        binding.declareLateInit(target);

        iterable.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.dup());
        target.add(Instruction.loadIntrinsics("it_key"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0, true)).setLocation(iterable.loc());

        int start = target.size();
        target.add(Instruction.dup(2, 0));
        target.add(Instruction.loadMember("next")).setLocation(iterable.loc());
        target.add(Instruction.call(0, true)).setLocation(iterable.loc());
        target.add(Instruction.dup());
        target.add(Instruction.loadMember("done")).setLocation(iterable.loc());
        int mid = target.temp();

        target.add(Instruction.loadMember("value")).setLocation(binding.loc);
        binding.assign(target, false);

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

    public ForOfNode(Location loc, String label, Binding binding, Node object, Node body) {
        super(loc);
        this.label = label;
        this.binding = binding;
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

        var binding = Binding.parse(src, i + n);
        if (!binding.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a binding in for-of loop");
        n += binding.n;
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

        return ParseRes.res(new ForOfNode(loc, label.result, binding.result, obj.result, bodyRes.result), n);
    }
}
