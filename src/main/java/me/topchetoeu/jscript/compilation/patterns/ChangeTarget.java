package me.topchetoeu.jscript.compilation.patterns;

import me.topchetoeu.jscript.compilation.CompileResult;

public interface ChangeTarget extends AssignTarget {
    void beforeChange(CompileResult target);
}
