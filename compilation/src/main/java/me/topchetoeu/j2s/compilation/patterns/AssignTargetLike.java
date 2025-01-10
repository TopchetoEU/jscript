package me.topchetoeu.j2s.compilation.patterns;

/**
 * Represents all nodes that can be converted to assign targets
 */
public interface AssignTargetLike {
	AssignTarget toAssignTarget();
}
