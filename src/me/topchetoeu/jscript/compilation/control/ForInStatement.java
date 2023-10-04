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

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
        if (isDeclaration) globScope.define(varName);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var key = scope.getKey(varName);
        if (key instanceof String) target.add(Instruction.makeVar((String)key));

        if (varValue != null) {
            varValue.compile(target, scope, true);
            target.add(Instruction.storeVar(scope.getKey(varName)));
        }

        object.compile(target, scope, true);
        target.add(Instruction.keys());
        
        int start = target.size();
        target.add(Instruction.dup());
        target.add(Instruction.loadMember("length"));
        target.add(Instruction.loadValue(0));
        target.add(Instruction.operation(Operation.LESS_EQUALS));
        int mid = target.size();
        target.add(Instruction.nop());

        target.add(Instruction.dup());
        target.add(Instruction.dup());
        target.add(Instruction.loadMember("length"));
        target.add(Instruction.loadValue(1));
        target.add(Instruction.operation(Operation.SUBTRACT));
        target.add(Instruction.dup(1, 2));
        target.add(Instruction.loadValue("length"));
        target.add(Instruction.dup(1, 2));
        target.add(Instruction.storeMember());
        target.add(Instruction.loadMember());
        target.add(Instruction.storeVar(key));

        for (var i = start; i < target.size(); i++) target.get(i).locate(loc());

        body.compileWithDebug(target, scope, false);

        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end).locate(loc()));
        target.add(Instruction.discard().locate(loc()));
        target.set(mid, Instruction.jmpIf(end - mid + 1).locate(loc()));
        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }

    public ForInStatement(Location loc, String label, boolean isDecl, String varName, Statement varValue, Statement object, Statement body) {
        super(loc);
        this.label = label;
        this.isDeclaration = isDecl;
        this.varName = varName;
        this.varValue = varValue;
        this.object = object;
        this.body = body;
    }
}
