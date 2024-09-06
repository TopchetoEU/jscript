package me.topchetoeu.jscript.compilation;

public interface AssignableNode {
    public void compileBeforeAssign(CompileResult target, boolean operator);
    public void compileAfterAssign(CompileResult target, boolean operator, boolean pollute);
    public default String assignName() {
        return null;
    }
}
