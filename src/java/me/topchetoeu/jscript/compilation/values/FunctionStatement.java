package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;
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

    public static ParseRes<FunctionStatement> parseFunction(Filename filename, List<Token> tokens, int i, boolean statement) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (!Parsing.isIdentifier(tokens, i + n++, "function")) return ParseRes.failed();

        var nameRes = Parsing.parseIdentifier(tokens, i + n);
        if (!nameRes.isSuccess() && statement) return ParseRes.error(loc, "A statement function requires a name, one is not present.");
        var name = nameRes.result;
        n += nameRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a parameter list.");

        var args = new ArrayList<String>();

        if (Parsing.isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
            n++;
        }
        else {
            while (true) {
                var argRes = Parsing.parseIdentifier(tokens, i + n);
                if (argRes.isSuccess()) {
                    args.add(argRes.result);
                    n++;
                    if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) {
                        n++;
                    }
                    if (Parsing.isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
                        n++;
                        break;
                    }
                }
                else return ParseRes.error(loc, "Expected an argument, comma or a closing brace.");
            }
        }

        var res = CompoundStatement.parse(filename, tokens, i + n);
        n += res.n;
        var end = Parsing.getLoc(filename, tokens, i + n - 1);

        if (res.isSuccess()) return ParseRes.res(new FunctionStatement(loc, end, name, args.toArray(String[]::new), statement, res.result), n);
        else return ParseRes.error(loc, "Expected a compound statement for function.", res);
    }
}
