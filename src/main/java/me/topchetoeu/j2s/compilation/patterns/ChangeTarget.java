package me.topchetoeu.j2s.compilation.patterns;

import me.topchetoeu.j2s.compilation.CompileResult;

public interface ChangeTarget extends AssignTarget {
	void beforeChange(CompileResult target);
}
