package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class FunctionStatement extends Statement {
    public final CompoundStatement body;
    public final String varName;
    public final String[] args;
    public final boolean statement;
    public final Location end;

    @Override public boolean pure() { return varName == null && statement; }

    @Override
    public void declare(CompileResult target) {
        if (varName != null && statement) target.scope.define(varName);
    }

    public static void checkBreakAndCont(CompileResult target, int start) {
        for (int i = start; i < target.size(); i++) {
            if (target.get(i).type == Type.NOP) {
                if (target.get(i).is(0, "break") ) {
                    throw new SyntaxException(target.map.toLocation(i), "Break was placed outside a loop.");
                }
                if (target.get(i).is(0, "cont")) {
                    throw new SyntaxException(target.map.toLocation(i), "Continue was placed outside a loop.");
                }
            }
        }
    }

    private CompileResult compileBody(CompileResult target, boolean pollute, BreakpointType bp) {
        for (var i = 0; i < args.length; i++) {
            for (var j = 0; j < i; j++) {
                if (args[i].equals(args[j])) {
                    throw new SyntaxException(loc(), "Duplicate parameter '" + args[i] + "'.");
                }
            }
        }

        var subtarget = new CompileResult(target.scope.child());

        subtarget.scope.define("this");
        var argsVar = subtarget.scope.define("arguments");

        if (args.length > 0) {
            for (var i = 0; i < args.length; i++) {
                subtarget.add(Instruction.loadVar(argsVar));
                subtarget.add(Instruction.pushValue(i));
                subtarget.add(Instruction.loadMember());
                subtarget.add(Instruction.storeVar(subtarget.scope.define(args[i])));
            }
        }

        if (!statement && this.varName != null) {
            subtarget.add(Instruction.storeSelfFunc((int)subtarget.scope.define(this.varName))).setLocationAndDebug(loc(), bp);
        }

        body.declare(subtarget);
        body.compile(subtarget, false);
        subtarget.length = args.length;
        subtarget.add(Instruction.ret()).setLocation(end);
        checkBreakAndCont(subtarget, 0);

        if (pollute) target.add(Instruction.loadFunc(target.children.size(), subtarget.scope.getCaptures()));
        return target.addChild(subtarget);
    }

    public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (this.varName != null) name = this.varName;

        var hasVar = this.varName != null && statement;
        var hasName = name != null;

        compileBody(target, pollute || hasVar || hasName, bp);

        if (hasName) {
            if (pollute || hasVar) target.add(Instruction.dup());
            target.add(Instruction.pushValue("name"));
            target.add(Instruction.pushValue(name));
            target.add(Instruction.storeMember());
        }

        if (hasVar) {
            var key = target.scope.getKey(this.varName);

            if (key instanceof String) target.add(Instruction.makeVar((String)key));
            target.add(Instruction.storeVar(target.scope.getKey(this.varName), false));
        }
    }
    public void compile(CompileResult target, boolean pollute, String name) {
        compile(target, pollute, name, BreakpointType.NONE);
    }
    @Override public void compile(CompileResult target, boolean pollute, BreakpointType bp) {
        compile(target, pollute, (String)null, bp);
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, (String)null, BreakpointType.NONE);
    }

    public FunctionStatement(Location loc, Location end, String varName, String[] args, boolean statement, CompoundStatement body) {
        super(loc);

        this.end = end;
        this.varName = varName;
        this.statement = statement;

        this.args = args;
        this.body = body;
    }

    public static void compileWithName(Statement stm, CompileResult target, boolean pollute, String name) {
        if (stm instanceof FunctionStatement) ((FunctionStatement)stm).compile(target, pollute, name);
        else stm.compile(target, pollute);
    }
    public static void compileWithName(Statement stm, CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (stm instanceof FunctionStatement) ((FunctionStatement)stm).compile(target, pollute, name, bp);
        else stm.compile(target, pollute, bp);
    }

    public static ParseRes<FunctionStatement> parseFunction(Source src, int i, boolean statement) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "function")) return ParseRes.failed();
        n += 8;

        var nameRes = Parsing.parseIdentifier(src, i + n);
        if (!nameRes.isSuccess() && statement) return ParseRes.error(src.loc(i + n), "A statement function requires a name");
        n += nameRes.n;
        n += Parsing.skipEmpty(src, i + n);

        var args = Parsing.parseParamList(src, i + n);
        if (!args.isSuccess()) return args.chainError(src.loc(i + n), "Expected a parameter list");
        n += args.n;

        var res = CompoundStatement.parse(src, i + n);
        if (!res.isSuccess()) res.chainError(src.loc(i + n), "Expected a compound statement for function.");
        n += res.n;

        return ParseRes.res(new FunctionStatement(
            loc, src.loc(i + n - 1),
            nameRes.result, args.result.toArray(String[]::new),
            statement, res.result
        ), n);
    }
}
