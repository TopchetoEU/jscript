package me.topchetoeu.jscript.compilation.values;

import java.util.Random;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.FunctionBody;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class FunctionStatement extends Statement {
    public final CompoundStatement body;
    public final String name;
    public final String[] args;

    private static Random rand = new Random();

    @Override
    public boolean pure() { return name == null; }

    @Override
    public void declare(ScopeRecord scope) {
        if (name != null) scope.define(name);
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

    public void compile(CompileTarget target, ScopeRecord scope, String name, boolean isStatement) {
        for (var i = 0; i < args.length; i++) {
            for (var j = 0; j < i; j++) {
                if (args[i].equals(args[j])){
                    target.add(Instruction.throwSyntax(new SyntaxException(loc(), "Duplicate parameter '" + args[i] + "'.")));
                    return;
                }
            }
        }
        var subscope = scope.child();

        int start = target.size();
        var funcTarget = new CompileTarget(target.functions, target.breakpoints);

        subscope.define("this");
        var argsVar = subscope.define("arguments");

        if (args.length > 0) {
            for (var i = 0; i < args.length; i++) {
                funcTarget.add(Instruction.loadVar(argsVar).locate(loc()));
                funcTarget.add(Instruction.loadMember(i).locate(loc()));
                funcTarget.add(Instruction.storeVar(subscope.define(args[i])).locate(loc()));
            }
        }

        if (!isStatement && this.name != null) {
            funcTarget.add(Instruction.storeSelfFunc((int)subscope.define(this.name)));
        }

        body.declare(subscope);
        body.compile(funcTarget, subscope, false);
        funcTarget.add(Instruction.ret().locate(loc()));
        checkBreakAndCont(funcTarget, start);

        var id = rand.nextLong();

        target.add(Instruction.loadFunc(id, subscope.localsCount(), args.length, subscope.getCaptures()).locate(loc()));
        target.functions.put(id, new FunctionBody(funcTarget.array(), subscope.captures(), subscope.locals()));

        if (name == null) name = this.name;

        if (name != null) {
            target.add(Instruction.dup().locate(loc()));
            target.add(Instruction.loadValue("name").locate(loc()));
            target.add(Instruction.loadValue(name).locate(loc()));
            target.add(Instruction.storeMember().locate(loc()));
        }

        if (this.name != null && isStatement) {
            var key = scope.getKey(this.name);

            if (key instanceof String) target.add(Instruction.makeVar((String)key).locate(loc()));
            target.add(Instruction.storeVar(scope.getKey(this.name), false).locate(loc()));
        }

    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compile(target, scope, null, false);
        if (!pollute) target.add(Instruction.discard().locate(loc()));
    }

    public FunctionStatement(Location loc, String name, String[] args, CompoundStatement body) {
        super(loc);
        this.name = name;

        this.args = args;
        this.body = body;
    }
}
