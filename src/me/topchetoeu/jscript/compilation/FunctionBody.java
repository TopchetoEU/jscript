package me.topchetoeu.jscript.compilation;

public class FunctionBody {
    public final Instruction[] instructions;
    public final String[] captureNames, localNames;
    public final int localsN, argsN;

    public FunctionBody(int localsN, int argsN, Instruction[] instructions, String[] captureNames, String[] localNames) {
        this.argsN = argsN;
        this.localsN = localsN;
        this.instructions = instructions;
        this.captureNames = captureNames;
        this.localNames = localNames;
    }
    public FunctionBody(int localsN, int argsN, Instruction[] instructions) {
        this.argsN = argsN;
        this.localsN = localsN;
        this.instructions = instructions;
        this.captureNames = new String[0];
        this.localNames = new String[0];
    }
    public FunctionBody(Instruction... instructions) {
        this.argsN = 0;
        this.localsN = 2;
        this.instructions = instructions;
        this.captureNames = new String[0];
        this.localNames = new String[0];
    }
}
