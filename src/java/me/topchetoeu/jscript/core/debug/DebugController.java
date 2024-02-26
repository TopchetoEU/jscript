package me.topchetoeu.jscript.core.debug;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Frame;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public interface DebugController {
    /**
     * Called when a script has been loaded
     * @param filename The name of the source
     * @param source The name of the source
     * @param breakpoints A set of all the breakpointable locations in this source
     * @param map The source map associated with this file. null if this source map isn't mapped
     */
    void onSource(Filename filename, String source);

    /**
     * Called immediatly before an instruction is executed, as well as after an instruction, if it has threw or returned.
     * This function might pause in order to await debugging commands.
     * @param ctx The context of execution
     * @param frame The frame in which execution is occuring
     * @param instruction The instruction which was or will be executed
     * @param returnVal The return value of the instruction, Values.NO_RETURN if none
     * @param error The error that the instruction threw, null if none
     * @param caught Whether or not the error has been caught
     * @return Whether or not the frame should restart (currently does nothing)
     */
    boolean onInstruction(Context ctx, Frame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught);

    /**
     * Called immediatly before a frame has been pushed on the frame stack.
     * This function might pause in order to await debugging commands.
     * @param ctx The context of execution
     * @param frame The code frame which was pushed
     */
    void onFramePush(Context ctx, Frame frame);
    /**
     * Called immediatly after a frame has been popped out of the frame stack.
     * This function might pause in order to await debugging commands.
     * @param ctx The context of execution
     * @param frame The code frame which was popped out
     */
    void onFramePop(Context ctx, Frame frame);

    public static DebugController empty() {
        return new DebugController () {
            @Override public void onFramePop(Context ctx, Frame frame) { }
            @Override public void onFramePush(Context ctx, Frame frame) { }
            @Override public boolean onInstruction(Context ctx, Frame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
                return false;
            }
            @Override public void onSource(Filename filename, String source) { }
        };
    }
}
