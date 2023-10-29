package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

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
        if (key instanceof String) target.add(Instruction.makeVar((String)key));

        if (varValue != null) {
            varValue.compile(target, scope, true);
            target.add(Instruction.storeVar(scope.getKey(varName)));
        }

        object.compileWithDebug(target, scope, true);
        target.add(Instruction.keys(true));

        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.loadValue(null));
        target.add(Instruction.operation(Operation.EQUALS));
        int mid = target.size();
        target.add(Instruction.nop());

        target.add(Instruction.loadMember("value").locate(varLocation));
        target.setDebug();
        target.add(Instruction.storeVar(key));

        body.compileWithDebug(target, scope, false);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.add(Instruction.discard());
        target.set(mid, Instruction.jmpIf(end - mid + 1));
        if (pollute) target.add(Instruction.loadValue(null));
        target.get(first).locate(loc());
        target.setDebug(first);
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
