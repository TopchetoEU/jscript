package me.topchetoeu.jscript.compilation;

import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import me.topchetoeu.jscript.Location;

public class CompileTarget {
    public final Vector<Instruction> target = new Vector<>();
    public final Map<Long, Instruction[]> functions;
    public final TreeSet<Location> breakpoints;

    public Instruction add(Instruction instr) {
        target.add(instr);
        return instr;
    }
    public Instruction set(int i, Instruction instr) {
        return target.set(i, instr);
    }
    public void setDebug(int i) {
        breakpoints.add(target.get(i).location);
    }
    public void setDebug() {
        setDebug(target.size() - 1);
    }
    public Instruction get(int i) {
        return target.get(i);
    }
    public int size() { return target.size(); }

    public Instruction[] array() { return target.toArray(Instruction[]::new); }

    public CompileTarget(Map<Long, Instruction[]> functions, TreeSet<Location> breakpoints) {
        this.functions = functions;
        this.breakpoints = breakpoints;
    }
}
