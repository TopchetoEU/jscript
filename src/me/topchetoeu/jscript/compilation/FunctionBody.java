package me.topchetoeu.jscript.compilation;

public class FunctionBody {
    public final Instruction[] instructions;
    public final String[] captureNames, localNames;

    public FunctionBody(Instruction[] instructions, String[] captureNames, String[] localNames) {
        this.instructions = instructions;
        this.captureNames = captureNames;
        this.localNames = localNames;
    }
    public FunctionBody(Instruction[] instructions) {
        this.instructions = instructions;
        this.captureNames = new String[0];
        this.localNames = new String[0];
    }
}
