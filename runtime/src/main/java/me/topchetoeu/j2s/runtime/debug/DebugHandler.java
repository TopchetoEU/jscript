package me.topchetoeu.j2s.runtime.debug;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.FunctionBody;
import me.topchetoeu.j2s.common.FunctionMap;
import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.runtime.Frame;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;

public interface DebugHandler {
	/**
	 * Called when a script has been loaded
	 * @param filename The name of the source
	 * @param source The name of the source
	 * @param breakpoints A set of all the breakpointable locations in this source
	 * @param map The source map associated with this file. null if this source map isn't mapped
	 */
	void onSourceLoad(Filename filename, String source);

	/**
	 * Called when a function body has been loaded
	 * @param body The body loaded
	 * @param map The map of the function
	 */
	void onFunctionLoad(FunctionBody body, FunctionMap map);

	/**
	 * Called immediatly before an instruction is executed, as well as after an instruction, if it has threw or returned.
	 * This function might pause in order to await debugging commands.
	 * @param env The context of execution
	 * @param frame The frame in which execution is occuring
	 * @param instruction The instruction which was or will be executed
	 * @param returnVal The return value of the instruction, Values.NO_RETURN if none
	 * @param error The error that the instruction threw, null if none
	 * @param caught Whether or not the error has been caught
	 * @return Whether or not the frame should restart (currently does nothing)
	 */
	boolean onInstruction(Environment env, Frame frame, Instruction instruction, Value returnVal, EngineException error, boolean caught);

	/**
	 * Called immediatly before a frame has been pushed on the frame stack.
	 * This function might pause in order to await debugging commands.
	 * @param env The context of execution
	 * @param frame The code frame which was pushed
	 */
	void onFramePush(Environment env, Frame frame);
	/**
	 * Called immediatly after a frame has been popped out of the frame stack.
	 * This function might pause in order to await debugging commands.
	 * @param env The context of execution
	 * @param frame The code frame which was popped out
	 */
	void onFramePop(Environment env, Frame frame);
}
