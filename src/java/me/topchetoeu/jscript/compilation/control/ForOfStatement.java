package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ForOfStatement extends Statement {
    public final String varName;
    public final boolean isDeclaration;
    public final Statement iterable, body;
    public final String label;
    public final Location varLocation;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
        if (isDeclaration) target.scope.define(varName);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var key = target.scope.getKey(varName);

        if (key instanceof String) target.add(Instruction.makeVar((String)key));

        iterable.compile(target, true, BreakpointType.STEP_OVER);
        target.add(Instruction.dup());
        target.add(Instruction.loadVar("Symbol"));
        target.add(Instruction.pushValue("iterator"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());

        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.dup());
        target.add(Instruction.pushValue("next"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        target.add(Instruction.call(0)).setLocation(iterable.loc());
        target.add(Instruction.dup());
        target.add(Instruction.pushValue("done"));
        target.add(Instruction.loadMember()).setLocation(iterable.loc());
        int mid = target.temp();

        target.add(Instruction.pushValue("value"));
        target.add(Instruction.loadMember()).setLocation(varLocation);
        target.add(Instruction.storeVar(key)).setLocationAndDebug(iterable.loc(), BreakpointType.STEP_OVER);

        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.add(Instruction.discard());
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIf(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForOfStatement(Location loc, Location varLocation, String label, boolean isDecl, String varName, Statement object, Statement body) {
        super(loc);
        this.varLocation = varLocation;
        this.label = label;
        this.isDeclaration = isDecl;
        this.varName = varName;
        this.iterable = object;
        this.body = body;
    }
}
