package me.topchetoeu.jscript.core.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.Operation;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class ForInStatement extends Statement {
    public final String varName;
    public final boolean isDeclaration;
    public final Statement varValue, object, body;
    public final String label;
    public final Location varLocation;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
        if (isDeclaration) globScope.define(varName);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var key = scope.getKey(varName);

        int first = target.size();
        if (key instanceof String) target.add(Instruction.makeVar(loc(), (String)key));

        if (varValue != null) {
            varValue.compile(target, scope, true);
            target.add(Instruction.storeVar(loc(), scope.getKey(varName)));
        }

        object.compile(target, scope, true, BreakpointType.STEP_OVER);
        target.add(Instruction.keys(loc(), true));

        int start = target.size();
        target.add(Instruction.dup(loc()));
        target.add(Instruction.loadValue(loc(), null));
        target.add(Instruction.operation(loc(), Operation.EQUALS));
        int mid = target.size();
        target.add(Instruction.nop(loc()));

        target.add(Instruction.loadMember(varLocation, "value"));
        target.add(Instruction.storeVar(object.loc(), key));
        target.setDebug(BreakpointType.STEP_OVER);

        body.compile(target, scope, false, BreakpointType.STEP_OVER);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(loc(), start - end));
        target.add(Instruction.discard(loc()));
        target.set(mid, Instruction.jmpIf(loc(), end - mid + 1));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
        target.get(first).locate(loc());
    }

    public ForInStatement(Location loc, Location varLocation, String label, boolean isDecl, String varName, Statement varValue, Statement object, Statement body) {
        super(loc);
        this.varLocation = varLocation;
        this.label = label;
        this.isDeclaration = isDecl;
        this.varName = varName;
        this.varValue = varValue;
        this.object = object;
        this.body = body;
    }
}
