package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.Map;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ObjectStatement extends Statement {
    public final Map<Object, Statement> map;
    public final Map<Object, FunctionStatement> getters;
    public final Map<Object, FunctionStatement> setters;

    @Override public boolean pure() {
        for (var el : map.values()) {
            if (!el.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.loadObj(loc()));

        for (var el : map.entrySet()) {
            target.add(Instruction.dup(loc()));
            target.add(Instruction.loadValue(loc(), el.getKey()));
            var val = el.getValue();
            FunctionStatement.compileWithName(val, target, scope, true, el.getKey().toString());
            target.add(Instruction.storeMember(loc()));
        }

        var keys = new ArrayList<Object>();
        keys.addAll(getters.keySet());
        keys.addAll(setters.keySet());

        for (var key : keys) {
            if (key instanceof String) target.add(Instruction.loadValue(loc(), (String)key));
            else target.add(Instruction.loadValue(loc(), (Double)key));

            if (getters.containsKey(key)) getters.get(key).compile(target, scope, true);
            else target.add(Instruction.loadValue(loc(), null));

            if (setters.containsKey(key)) setters.get(key).compile(target, scope, true);
            else target.add(Instruction.loadValue(loc(), null));

            target.add(Instruction.defProp(loc()));
        }

        if (!pollute) target.add(Instruction.discard(loc()));
    }

    public ObjectStatement(Location loc, Map<Object, Statement> map, Map<Object, FunctionStatement> getters, Map<Object, FunctionStatement> setters) {
        super(loc);
        this.map = map;
        this.getters = getters;
        this.setters = setters;
    }
}
