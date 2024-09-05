package me.topchetoeu.jscript.common;

public class FunctionBody {
    public final FunctionBody[] children;
    public final Instruction[] instructions;
    public final int localsN, capturablesN, capturesN, length;

    public FunctionBody(int localsN, int capturablesN, int capturesN, int length,  Instruction[] instructions, FunctionBody[] children) {
        this.children = children;
        this.length = length;
        this.localsN = localsN;
        this.capturablesN = capturablesN;
        this.capturesN = capturesN;
        this.instructions = instructions;
    }
}
