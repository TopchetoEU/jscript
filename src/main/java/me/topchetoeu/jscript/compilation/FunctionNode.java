package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public abstract class FunctionNode extends Node {
    public final CompoundNode body;
    public final List<VariableNode> params;
    public final Location end;

    public abstract String name();
	public final String name(String fallback) {
        return this.name() != null ? this.name() : fallback;
	}

    protected final int[] captures(int id, CompileResult target) {
        return target.children.get(id).scope.getCaptureIndices();
    }

    protected final Environment rootEnv(Environment env) {
        return env.get(JavaScript.COMPILE_ROOT);
    }

    public final CompileResult compileBody(Environment env, FunctionScope scope, boolean lastReturn, String _name, String selfName) {
        var target = new CompileResult(env, scope, params.size());
		var i = 0;

		for (var param : params) {
			var index = scope.define(param.name);

			target.add(Instruction.loadArg(i++));
			target.add(index.index().toInit());
		}

		// if (selfName != null && !scope.has(selfName, false)) {
		//     var i = scope.defineSpecial(new Variable(selfName, true), end);

		//     target.add(Instruction.loadCalled());
		//     target.add(_i -> i.index().toInit());
		// }

		body.resolve(target);
		body.compile(target, lastReturn, BreakpointType.NONE);

		return target;
    }
    public final CompileResult compileBody(CompileResult parent, String name, String selfName) {
        return compileBody(rootEnv(parent.env).child(), new FunctionScope(parent.scope), false, name, selfName);
    }

    public abstract void compile(CompileResult target, boolean pollute, String name, BreakpointType bp);
    public void compile(CompileResult target, boolean pollute, String name) {
        compile(target, pollute, name, BreakpointType.NONE);
    }
    @Override public void compile(CompileResult target, boolean pollute, BreakpointType bp) {
        compile(target, pollute, (String)null, bp);
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, (String)null, BreakpointType.NONE);
    }

    public FunctionNode(Location loc, Location end, List<VariableNode> params, CompoundNode body) {
        super(loc);

        this.end = end;
        this.params = params;
        this.body = body;
    }

    public static void compileWithName(Node stm, CompileResult target, boolean pollute, String name) {
        if (stm instanceof FunctionNode) ((FunctionNode)stm).compile(target, pollute, name);
        else stm.compile(target, pollute);
    }
    public static void compileWithName(Node stm, CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (stm instanceof FunctionNode) ((FunctionNode)stm).compile(target, pollute, name, bp);
        else stm.compile(target, pollute, bp);
    }

    public static ParseRes<FunctionNode> parseFunction(Source src, int i, boolean statement) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "function")) return ParseRes.failed();
        n += 8;

        var name = Parsing.parseIdentifier(src, i + n);
        if (!name.isSuccess() && statement) return ParseRes.error(src.loc(i + n), "A statement function requires a name");
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        var params = JavaScript.parseParameters(src, i + n);
        if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected a parameter list");
        n += params.n;

        var body = CompoundNode.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for function");
        n += body.n;

        if (statement) return ParseRes.res(new FunctionStatementNode(
            loc, src.loc(i + n - 1),
            params.result, body.result, name.result
        ), n);
        else return ParseRes.res(new FunctionValueNode(
            loc, src.loc(i + n - 1),
            params.result, body.result, name.result
        ), n);
    }
}
