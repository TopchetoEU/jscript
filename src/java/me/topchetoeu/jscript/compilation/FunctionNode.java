package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.scope.FunctionScope;
import me.topchetoeu.jscript.compilation.scope.LocalScope;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public abstract class FunctionNode extends Node {
    public final CompoundNode body;
    public final String[] args;
    public final Location end;

    public abstract String name();

    // @Override public void declare(CompileResult target) {
    //     if (varName != null && statement) target.scope.define(varName);
    // }

    // public static void checkBreakAndCont(CompileResult target, int start) {
    //     for (int i = start; i < target.size(); i++) {
    //         if (target.get(i).type == Type.NOP) {
    //             if (target.get(i).is(0, "break") ) {
    //                 throw new SyntaxException(target.map.toLocation(i), "Break was placed outside a loop.");
    //             }
    //             if (target.get(i).is(0, "cont")) {
    //                 throw new SyntaxException(target.map.toLocation(i), "Continue was placed outside a loop.");
    //             }
    //         }
    //     }
    // }

    protected void compileLoadFunc(CompileResult target, int[] captures, String name) {
        target.add(Instruction.loadFunc(target.children.size(), name, captures));
    }

    private CompileResult compileBody(CompileResult target, String name, boolean storeSelf, boolean pollute, BreakpointType bp) {
        for (var i = 0; i < args.length; i++) {
            for (var j = 0; j < i; j++) {
                if (args[i].equals(args[j])) {
                    throw new SyntaxException(loc(), "Duplicate parameter '" + args[i] + "'.");
                }
            }
        }

        var env = target.env.child()
            .remove(LabelContext.BREAK_CTX)
            .remove(LabelContext.CONTINUE_CTX);

        var funcScope = new FunctionScope(target.scope);
        var subtarget = new CompileResult(env, new LocalScope(funcScope));

        for (var arg : args) {
            // TODO: Implement default values
            // TODO: Implement argument location
            funcScope.defineArg(arg, loc());
        }

        body.resolve(subtarget);
        body.compile(subtarget, false);

        subtarget.length = args.length;
        subtarget.scope.end();
        funcScope.end();

        if (pollute) compileLoadFunc(target, funcScope.getCaptureIndices(), name);

        return target.addChild(subtarget);
    }

    public void compile(CompileResult target, boolean pollute, boolean storeSelf, String name, BreakpointType bp) {
        if (this.name() != null) name = this.name();

        compileBody(target, name, storeSelf, pollute, bp);
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

    public FunctionNode(Location loc, Location end, String[] args, CompoundNode body) {
        super(loc);

        this.end = end;
        this.args = args;
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

        var args = JavaScript.parseParamList(src, i + n);
        if (!args.isSuccess()) return args.chainError(src.loc(i + n), "Expected a parameter list");
        n += args.n;

        var body = CompoundNode.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for function.");
        n += body.n;

        if (statement) return ParseRes.res(new FunctionStatementNode(
            loc, src.loc(i + n - 1),
            args.result.toArray(String[]::new), body.result, name.result
        ), n);
        else return ParseRes.res(new FunctionValueNode(
            loc, src.loc(i + n - 1),
            args.result.toArray(String[]::new), body.result, name.result
        ), n);
    }
}
