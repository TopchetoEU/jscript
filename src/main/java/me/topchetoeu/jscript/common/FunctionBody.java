package me.topchetoeu.jscript.common;

public class FunctionBody {
    public final FunctionBody[] children;
    public final Instruction[] instructions;
    public final int localsN, capturesN, argsN, length;

    public FunctionBody(int localsN, int capturesN, int length, int argsN, Instruction[] instructions, FunctionBody[] children) {
        this.children = children;
        this.length = length;
        this.argsN = argsN;
        this.localsN = localsN;
        this.capturesN = capturesN;
        this.instructions = instructions;
    }
}
