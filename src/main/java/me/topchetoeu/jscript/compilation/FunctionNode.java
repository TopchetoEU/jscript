package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public abstract class FunctionNode extends Node {
    public final CompoundNode body;
    public final Parameters params;
    public final Location end;

    public abstract String name();

    protected final int[] captures(int id, CompileResult target) {
        return ((FunctionScope)target.children.get(id).scope).getCaptureIndices();
    }

    public final CompileResult compileBody(Environment env, FunctionScope scope, boolean lastReturn, boolean hasArgs, String _name, String selfName) {
        var name = this.name() != null ? this.name() : _name;

        env = env.child()
            .remove(LabelContext.BREAK_CTX)
            .remove(LabelContext.CONTINUE_CTX);

        return new CompileResult(env, scope, params.params.size(), target -> {
            if (hasArgs || params.params.size() > 0) target.add(Instruction.loadArgs());

            if (hasArgs) {
                var argsVar = scope.defineStrict(new Variable("arguments", true), loc());
                target.add(_i -> Instruction.storeVar(argsVar.index(), params.params.size() > 0));
            }

            if (params.params.size() > 0) {
                if (params.params.size() > 1) target.add(Instruction.dup(params.params.size() - 1));
                var i = 0;

                for (var param : params.params) {
                    if (scope.has(param.name, false)) throw new SyntaxException(param.loc, "Duplicate parameter name not allowed");
                    if (!JavaScript.checkVarName(param.name)) {
                        throw new SyntaxException(param.loc, String.format("Unexpected identifier '%s'", param.name));
                    }
                    var varI = scope.define(new Variable(param.name, false), param.loc);

                    target.add(Instruction.loadMember(i++));

                    if (param.node != null) {
                        var end = new DeferredIntSupplier();

                        target.add(Instruction.dup());
                        target.add(Instruction.pushUndefined());
                        target.add(Instruction.operation(Operation.EQUALS));
                        target.add(Instruction.jmpIfNot(end));
                        target.add(Instruction.discard());
                        param.node.compile(target, true);

                        end.set(target.size());
                    }

                    target.add(_i -> Instruction.storeVar(varI.index()));
                }
            }

            if (params.restName != null) {
                if (scope.has(params.restName, false)) throw new SyntaxException(params.restLocation, "Duplicate parameter name not allowed");
                var restVar = scope.defineParam(new Variable(params.restName, false), params.restLocation);
                target.add(Instruction.loadRestArgs(params.params.size()));
                target.add(_i -> Instruction.storeVar(restVar.index()));
            }

            if (selfName != null && !scope.has(name, false)) {
                var i = scope.defineParam(new Variable(selfName, true), end);

                target.add(Instruction.loadCallee());
                target.add(_i -> Instruction.storeVar(i.index(), false));
            }

            body.resolve(target);
            body.compile(target, lastReturn, false, BreakpointType.NONE);

            scope.end();

            for (var child : target.children) child.buildTask.run();

            scope.finish();
        });
    }
    public final CompileResult compileBody(CompileResult parent, boolean hasArgs, String name, String selfName) {
        return compileBody(parent.env, new FunctionScope(parent.scope), false, hasArgs, name, selfName);
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

    public FunctionNode(Location loc, Location end, Parameters params, CompoundNode body) {
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
