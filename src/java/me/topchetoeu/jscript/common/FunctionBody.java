package me.topchetoeu.jscript.common;

public class FunctionBody {
    public final FunctionBody[] children;
    public final Instruction[] instructions;
    public final int localsN, capturesN, argsN;

    public FunctionBody(int localsN, int capturesN, int argsN, Instruction[] instructions, FunctionBody[] children) {
        this.children = children;
        this.argsN = argsN;
        this.localsN = localsN;
        this.capturesN = capturesN;
        this.instructions = instructions;
    }
}
