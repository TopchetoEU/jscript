package me.topchetoeu.j2s.compilation.members;

import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;

public interface Member {
	Location loc();

	void compileFunctions(CompileResult target);
	void compile(CompileResult target, boolean pollute);
}
