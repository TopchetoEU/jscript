package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class FunctionStatement extends Statement {
    public final CompoundStatement body;
    public final String name;
    public final String[] args;

    @Override
    public boolean pure() { return name == null; }
    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void declare(ScopeRecord scope) {
        if (name != null) scope.define(name);
    }

    public static void checkBreakAndCont(List<Instruction> target, int start) {
        for (int i = start; i < target.size(); i++) {
            if (target.get(i).type == Type.NOP) {
                if (target.get(i).is(0, "break") || target.get(i).is(0, "try_break")) {
                    throw new SyntaxException(target.get(i).location, "Break was placed outside a loop.");
                }
                if (target.get(i).is(0, "cont") || target.get(i).is(0, "try_cont")) {
                    throw new SyntaxException(target.get(i).location, "Continue was placed outside a loop.");
                }
            }
        }
    }

    public void compile(List<Instruction> target, ScopeRecord scope, String name, boolean isStatement) {
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

        target.add(Instruction.nop());
        subscope.define("this");

        var argsVar = subscope.define("arguments");
        if (args.length > 0) {
            target.add(Instruction.loadVar(argsVar).locate(loc()));
            if (args.length != 1) target.add(Instruction.dup(args.length - 1).locate(loc()));

            for (var i = 0; i < args.length; i++) {
                target.add(Instruction.loadMember(i).locate(loc()));
                target.add(Instruction.storeVar(subscope.define(args[i])).locate(loc()));
            }
        }

        if (!isStatement && this.name != null) {
            target.add(Instruction.storeSelfFunc((int)subscope.define(this.name)));
        }

        body.declare(subscope);
        target.add(Instruction.debugVarNames(subscope.locals()));
        body.compile(target, subscope);

        checkBreakAndCont(target, start);

        if (!(body instanceof CompoundStatement)) target.add(Instruction.ret().locate(loc()));

        target.set(start, Instruction.loadFunc(target.size() - start, subscope.localsCount(), args.length, subscope.getCaptures()).locate(loc()));

        if (name == null) name = this.name;

        if (name != null) {
            target.add(Instruction.dup(1).locate(loc()));
            target.add(Instruction.loadValue("name").locate(loc()));
            target.add(Instruction.loadValue(name).locate(loc()));
            target.add(Instruction.storeMember().locate(loc()));
        }

        if (this.name != null && isStatement) {
            var key = scope.getKey(this.name);

            if (key instanceof String) target.add(Instruction.makeVar((String)key).locate(loc()));
            target.add(Instruction.storeVar(scope.getKey(this.name), true).locate(loc()));
        }
    }
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        compile(target, scope, null, false);
    }

    public FunctionStatement(Location loc, String name, String[] args, CompoundStatement body) {
        super(loc);
        this.name = name;

        this.args = args;
        this.body = body;
    }
}
