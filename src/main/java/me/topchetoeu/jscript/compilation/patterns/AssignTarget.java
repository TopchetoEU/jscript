package me.topchetoeu.jscript.compilation.patterns;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;

/**
 * Represents all nodes that can be assign targets
 */
public interface AssignTarget extends AssignTargetLike {
	Location loc();

	/**
	 * Called to perform calculations before the assigned value is calculated
	 */
	default void beforeAssign(CompileResult target) {}
	/**
	 * Called to perform the actual assignemnt. Between the `beforeAssign` and this call a single value will have been pushed to the stack
	 * @param pollute Whether or not to leave the original value on the stack
	 */
	void afterAssign(CompileResult target, boolean pollute);

	default void assign(CompileResult target, boolean pollute) {
		afterAssign(target, pollute);
	}

	@Override default AssignTarget toAssignTarget() { return this; }
}
