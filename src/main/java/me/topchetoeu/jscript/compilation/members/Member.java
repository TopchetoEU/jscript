package me.topchetoeu.jscript.compilation.members;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;

public interface Member {
    Location loc();

	void compileFunctions(CompileResult target);
    void compile(CompileResult target, boolean pollute);
}
