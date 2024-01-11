package me.topchetoeu.jscript.core.compilation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.Environment;
import me.topchetoeu.jscript.core.engine.values.CodeFunction;

public class CompileTarget {
    public final Vector<Instruction> target = new Vector<>();
    public final Map<Long, FunctionBody> functions;
    public final TreeSet<Location> breakpoints;
    private final HashMap<Location, Instruction> bpToInstr = new HashMap<>();

    public Instruction add(Instruction instr) {
        target.add(instr);
        return instr;
    }
    public Instruction set(int i, Instruction instr) {
        return target.set(i, instr);
    }
    public void setDebug(int i, BreakpointType type) {
        var instr = target.get(i);
        instr.breakpoint = type;

        if (type == BreakpointType.NONE) {
            breakpoints.remove(target.get(i).location);
            bpToInstr.remove(instr.location, instr);
        }
        else {
            breakpoints.add(target.get(i).location);

            var old = bpToInstr.put(instr.location, instr);
            if (old != null) old.breakpoint = BreakpointType.NONE;
        }
    }
    public void setDebug(BreakpointType type) {
        setDebug(target.size() - 1, type);
    }
    public Instruction get(int i) {
        return target.get(i);
    }
    public int size() { return target.size(); }
    public Location lastLoc(Location fallback) {
        if (target.size() == 0) return fallback;
        else return target.get(target.size() - 1).location;
    }

    public Instruction[] array() { return target.toArray(Instruction[]::new); }

    public FunctionBody body() {
        return functions.get(0l);
    }
    public CodeFunction func(Environment env) {
        return new CodeFunction(env, "", body());
    }

    public CompileTarget(Map<Long, FunctionBody> functions, TreeSet<Location> breakpoints) {
        this.functions = functions;
        this.breakpoints = breakpoints;
    }
}
