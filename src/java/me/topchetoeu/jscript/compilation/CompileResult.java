package me.topchetoeu.jscript.compilation;

import java.util.List;
import java.util.LinkedList;
import java.util.Vector;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.common.mapping.FunctionMap.FunctionMapBuilder;
import me.topchetoeu.jscript.core.scope.LocalScopeRecord;

public class CompileResult {
    public final Vector<Instruction> instructions = new Vector<>();
    public final List<CompileResult> children = new LinkedList<>();
    public final FunctionMapBuilder map = FunctionMap.builder();
    public final LocalScopeRecord scope;
    public int length = 0;

    public int temp() {
        instructions.add(null);
        return instructions.size() - 1;
    }

    public CompileResult add(Instruction instr) {
        instructions.add(instr);
        return this;
    }
    public CompileResult set(int i, Instruction instr) {
        instructions.set(i, instr);
        return this;
    }
    public Instruction get(int i) {
        return instructions.get(i);
    }
    public int size() { return instructions.size(); }

    public void setDebug(Location loc, BreakpointType type) {
        map.setDebug(loc, type);
    }
    public void setLocation(int i, Location loc) {
        map.setLocation(i, loc);
    }
    public void setLocationAndDebug(int i, Location loc, BreakpointType type) {
        map.setLocationAndDebug(i, loc, type);
    }
    public void setDebug(BreakpointType type) {
        setDebug(map.last(), type);
    }
    public void setLocation(Location type) {
        setLocation(instructions.size() - 1, type);
    }
    public void setLocationAndDebug(Location loc, BreakpointType type) {
        setLocationAndDebug(instructions.size() - 1, loc, type);
    }

    public CompileResult addChild(CompileResult child) {
        this.children.add(child);
        return child;
    }

    public FunctionMap map() {
        return map.build(scope);
    }
    public FunctionBody body() {
        var builtChildren = new FunctionBody[children.size()];

        for (var i = 0; i < children.size(); i++) builtChildren[i] = children.get(i).body();

        return new FunctionBody(scope.localsCount(), length, instructions.toArray(Instruction[]::new), builtChildren);
    }

    public CompileResult(LocalScopeRecord scope) {
        this.scope = scope;
    }
}
