package me.topchetoeu.jscript.compilation.destructing;

import me.topchetoeu.jscript.compilation.CompileResult;

public interface ChangeTarget extends AssignTarget {
    void beforeChange(CompileResult target);
    void afterChange(CompileResult target, boolean pollute);
}
