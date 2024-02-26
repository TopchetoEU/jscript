package me.topchetoeu.jscript.compilation;

public class FunctionBody {
    public final FunctionBody[] children;
    public final Instruction[] instructions;
    public final int localsN, argsN;

    public FunctionBody(int localsN, int argsN, Instruction[] instructions, FunctionBody[] children) {
        this.children = children;
        this.argsN = argsN;
        this.localsN = localsN;
        this.instructions = instructions;
    }
}
