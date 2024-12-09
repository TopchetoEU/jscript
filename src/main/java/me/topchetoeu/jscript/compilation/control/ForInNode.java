package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class ForInNode extends Node {
	public final boolean isDecl;
    public final VariableNode binding;
    public final Node object, body;
    public final String label;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
        binding.resolve(target);
    }

	@Override public void compileFunctions(CompileResult target) {
		object.compileFunctions(target);
		body.compileFunctions(target);
	}
    @Override public void compile(CompileResult target, boolean pollute) {
        object.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.keys(false, true));

        int start = target.size();
        target.add(Instruction.dup());
        int mid = target.temp();

        target.add(Instruction.loadMember("value")).setLocation(binding.loc());
        target.add(VariableNode.toSet(target, loc(), binding.name, false, true));

        target.setLocationAndDebug(object.loc(), BreakpointType.STEP_OVER);

        var end = new DeferredIntSupplier();

        LabelContext.pushLoop(target.env, loc(), label, end, start);
        CompoundNode.compileMultiEntry(body, target, false, BreakpointType.STEP_OVER);

        int endI = target.size();

        target.add(Instruction.jmp(start - endI));
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIfNot(endI - mid + 1));

		end.set(endI);
        LabelContext.popLoop(target.env, label);

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForInNode(Location loc, String label, VariableNode binding, boolean isDecl, Node object, Node body) {
        super(loc);
        this.label = label;
        this.binding = binding;
		this.isDecl = isDecl;
        this.object = object;
        this.body = body;
    }

    public static ParseRes<ForInNode> parse(Source src, int i) {
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

        var varKw = JavaScript.parseDeclarationType(src, i + n);
        n += varKw.n;
        n += Parsing.skipEmpty(src, i + n);

		var bindingLoc = src.loc(i + n);

		var name = Parsing.parseIdentifier(src, i + n);
		if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a variable name");
		n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "in")) return ParseRes.error(src.loc(i + n), "Expected 'in' keyword after variable declaration");
        n += 2;

        var obj = JavaScript.parseExpression(src, i + n, 0);
        if (!obj.isSuccess()) return obj.chainError(src.loc(i + n), "Expected a value");
        n += obj.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren");
        n++;

        var bodyRes = JavaScript.parseStatement(src, i + n);
        if (!bodyRes.isSuccess()) return bodyRes.chainError(src.loc(i + n), "Expected a for-in body");
        n += bodyRes.n;

        return ParseRes.res(new ForInNode(loc, label.result, new VariableNode(bindingLoc, name.result), varKw.isSuccess(), obj.result, bodyRes.result), n);
    }
}
