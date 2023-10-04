package me.topchetoeu.jscript.compilation;

import java.util.Map;
import java.util.Vector;

public class CompileTarget {
    public final Vector<Instruction> target = new Vector<>();
    public final Map<Long, Instruction[]> functions;

    public Instruction add(Instruction instr) {
        target.add(instr);
        return instr;
    }
    public Instruction set(int i, Instruction instr) {
        return target.set(i, instr);
    }
    public Instruction get(int i) {
        return target.get(i);
    }
    public int size() { return target.size(); }

    public Instruction[] array() { return target.toArray(Instruction[]::new); }

    public CompileTarget(Map<Long, Instruction[]> functions) {
        this.functions = functions;
    }
}
