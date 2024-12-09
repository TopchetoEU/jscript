package me.topchetoeu.jscript.compilation.patterns;

/**
 * Represents all nodes that can be converted to assign targets
 */
public interface AssignTargetLike {
	AssignTarget toAssignTarget();
}
