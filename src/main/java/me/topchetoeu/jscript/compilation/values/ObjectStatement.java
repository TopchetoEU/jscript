package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.Map;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ObjectStatement extends Statement {
    public final Map<String, Statement> map;
    public final Map<String, FunctionStatement> getters;
    public final Map<String, FunctionStatement> setters;

    @Override public boolean pure() {
        for (var el : map.values()) {
            if (!el.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadObj());

        for (var el : map.entrySet()) {
            target.add(Instruction.dup());
            target.add(Instruction.pushValue(el.getKey()));
            var val = el.getValue();
            FunctionStatement.compileWithName(val, target, true, el.getKey().toString());
            target.add(Instruction.storeMember());
        }

        var keys = new ArrayList<Object>();
        keys.addAll(getters.keySet());
        keys.addAll(setters.keySet());

        for (var key : keys) {
            target.add(Instruction.pushValue((String)key));

            if (getters.containsKey(key)) getters.get(key).compile(target, true);
            else target.add(Instruction.pushUndefined());

            if (setters.containsKey(key)) setters.get(key).compile(target, true);
            else target.add(Instruction.pushUndefined());

            target.add(Instruction.defProp());
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ObjectStatement(Location loc, Map<String, Statement> map, Map<String, FunctionStatement> getters, Map<String, FunctionStatement> setters) {
        super(loc);
        this.map = map;
        this.getters = getters;
        this.setters = setters;
    }
}
