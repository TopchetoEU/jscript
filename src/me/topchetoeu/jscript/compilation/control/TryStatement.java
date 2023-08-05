package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class TryStatement extends Statement {
    public final Statement tryBody;
    public final Statement catchBody;
    public final Statement finallyBody;
    public final String name;

    @Override
    public boolean pollutesStack() { return false; }

    @Override
    public void declare(ScopeRecord globScope) {
        tryBody.declare(globScope);
        if (catchBody != null) catchBody.declare(globScope);
        if (finallyBody != null) finallyBody.declare(globScope);
    }

    private void compileBody(List<Instruction> target, ScopeRecord scope, Statement body, String arg) {
        var subscope = scope.child();
        int start = target.size();

        target.add(Instruction.nop());

        subscope.define("this");
        var argsVar = subscope.define("<catchargs>");

        if (arg != null) {
            target.add(Instruction.loadVar(argsVar));
            target.add(Instruction.loadMember(0));
            target.add(Instruction.storeVar(subscope.define(arg)));
        }

        int bodyStart = target.size();
        body.compile(target, subscope);
        target.add(Instruction.signal("no_return"));

        target.get(bodyStart).locate(body.loc());


        target.set(start, Instruction.loadFunc(target.size() - start, subscope.localsCount(), 0, subscope.getCaptures()));
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        int start = target.size();

        compileBody(target, scope, tryBody, null);

        if (catchBody != null) {
            compileBody(target, scope, catchBody, name);
        }
        if (finallyBody != null) {
            compileBody(target, scope, finallyBody, null);
        }

        for (int i = start; i < target.size(); i++) {
            if (target.get(i).type == Type.NOP) {
                var instr = target.get(i);
                if (instr.is(0, "break")) {
                    target.set(i, Instruction.nop("try_break", instr.get(1), target.size()).locate(instr.location));
                }
                else if (instr.is(0, "cont")) {
                    target.set(i, Instruction.nop("try_cont", instr.get(1), target.size()).locate(instr.location));
                }
            }
        }

        target.add(Instruction.tryInstr(catchBody != null, finallyBody != null).locate(loc()));
    }

    public TryStatement(Location loc, Statement tryBody, Statement catchBody, Statement finallyBody, String name) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.name = name;
    }
}
