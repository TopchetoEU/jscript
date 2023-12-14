package me.topchetoeu.jscript.compilation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.CodeFunction;

public class CompileTarget {
    public final Vector<Instruction> target = new Vector<>();
    public final Map<Long, FunctionBody> functions;
    public final TreeSet<Location> breakpoints;
    private final HashMap<Location, Instruction> bpToInstr = new HashMap<>();
    private BreakpointType queueType = BreakpointType.NONE;
    private Location queueLoc = null;

    public Instruction add(Instruction instr) {
        target.add(instr);
        if (queueType != BreakpointType.NONE) setDebug(queueType);
        if (queueLoc != null) instr.locate(queueLoc);
        queueType = BreakpointType.NONE;
        queueLoc = null;
        return instr;
    }
    public Instruction set(int i, Instruction instr) {
        return target.set(i, instr);
    }
    public void setDebug(int i, BreakpointType type) {
        var instr = target.get(i);
        instr.breakpoint = type;
        breakpoints.add(target.get(i).location);

        var old = bpToInstr.put(instr.location, instr);
        if (old != null) old.breakpoint = BreakpointType.NONE;
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

    public void queueDebug(BreakpointType type) {
        queueType = type;
    }
    public void queueDebug(BreakpointType type, Location loc) {
        queueType = type;
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
