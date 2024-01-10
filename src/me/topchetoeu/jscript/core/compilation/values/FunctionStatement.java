package me.topchetoeu.jscript.core.compilation.values;

import java.util.Random;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.CompoundStatement;
import me.topchetoeu.jscript.core.compilation.FunctionBody;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.compilation.Instruction.Type;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.core.exceptions.SyntaxException;

public class FunctionStatement extends Statement {
    public final CompoundStatement body;
    public final String varName;
    public final String[] args;
    public final boolean statement;
    public final Location end;

    private static Random rand = new Random();

    @Override public boolean pure() { return varName == null && statement; }

    @Override
    public void declare(ScopeRecord scope) {
        if (varName != null && statement) scope.define(varName);
    }

    public static void checkBreakAndCont(CompileTarget target, int start) {
        for (int i = start; i < target.size(); i++) {
            if (target.get(i).type == Type.NOP) {
                if (target.get(i).is(0, "break") ) {
                    throw new SyntaxException(target.get(i).location, "Break was placed outside a loop.");
                }
                if (target.get(i).is(0, "cont")) {
                    throw new SyntaxException(target.get(i).location, "Continue was placed outside a loop.");
                }
            }
        }
    }

    private long compileBody(CompileTarget target, ScopeRecord scope, boolean polute, BreakpointType bp) {
        for (var i = 0; i < args.length; i++) {
            for (var j = 0; j < i; j++) {
                if (args[i].equals(args[j])) {
                    throw new SyntaxException(loc(), "Duplicate parameter '" + args[i] + "'.");
                }
            }
        }

        var id = rand.nextLong();
        var subscope = scope.child();
        var subtarget = new CompileTarget(target.functions, target.breakpoints);

        subscope.define("this");
        var argsVar = subscope.define("arguments");

        if (args.length > 0) {
            for (var i = 0; i < args.length; i++) {
                subtarget.add(Instruction.loadVar(loc(), argsVar));
                subtarget.add(Instruction.loadMember(loc(), i));
                subtarget.add(Instruction.storeVar(loc(), subscope.define(args[i])));
            }
        }

        if (!statement && this.varName != null) {
            subtarget.add(Instruction.storeSelfFunc(loc(), (int)subscope.define(this.varName)));
            subtarget.setDebug(bp);
        }

        body.declare(subscope);
        body.compile(subtarget, subscope, false);
        subtarget.add(Instruction.ret(end));
        checkBreakAndCont(subtarget, 0);

        if (polute) target.add(Instruction.loadFunc(loc(), id, subscope.getCaptures()));
        target.functions.put(id, new FunctionBody(
            subscope.localsCount(), args.length,
            subtarget.array(), subscope.captures(), subscope.locals()
        ));

        return id;
    }

    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, String name, BreakpointType bp) {
        if (this.varName != null) name = this.varName;

        var hasVar = this.varName != null && statement;
        var hasName = name != null;

        compileBody(target, scope, pollute || hasVar || hasName, bp);

        if (hasName) {
            if (pollute || hasVar) target.add(Instruction.dup(loc()));
            target.add(Instruction.loadValue(loc(), "name"));
            target.add(Instruction.loadValue(loc(), name));
            target.add(Instruction.storeMember(loc()));
        }

        if (hasVar) {
            var key = scope.getKey(this.varName);

            if (key instanceof String) target.add(Instruction.makeVar(loc(), (String)key));
            target.add(Instruction.storeVar(loc(), scope.getKey(this.varName), false));
        }
    }
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, String name) {
        compile(target, scope, pollute, name, BreakpointType.NONE);
    }
    @Override public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType bp) {
        compile(target, scope, pollute, (String)null, bp);
    }
    @Override public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compile(target, scope, pollute, (String)null, BreakpointType.NONE);
    }

    public FunctionStatement(Location loc, Location end, String varName, String[] args, boolean statement, CompoundStatement body) {
        super(loc);

        this.end = end;
        this.varName = varName;
        this.statement = statement;

        this.args = args;
        this.body = body;
    }

    public static void compileWithName(Statement stm, CompileTarget target, ScopeRecord scope, boolean pollute, String name) {
        if (stm instanceof FunctionStatement) ((FunctionStatement)stm).compile(target, scope, pollute, name);
        else stm.compile(target, scope, pollute);
    }
    public static void compileWithName(Statement stm, CompileTarget target, ScopeRecord scope, boolean pollute, String name, BreakpointType bp) {
        if (stm instanceof FunctionStatement) ((FunctionStatement)stm).compile(target, scope, pollute, name, bp);
        else stm.compile(target, scope, pollute, bp);
    }
}
