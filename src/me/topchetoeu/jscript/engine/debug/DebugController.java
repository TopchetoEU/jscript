package me.topchetoeu.jscript.engine.debug;

import java.util.TreeSet;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.exceptions.EngineException;

public interface DebugController {
    /**
     * Called when a script has been loaded
     * @param breakpoints 
     */
    void onSource(Filename filename, String source, TreeSet<Location> breakpoints);

    /**
     * Called immediatly before an instruction is executed, as well as after an instruction, if it has threw or returned.
     * This function might pause in order to await debugging commands.
     * @param ctx The context of execution
     * @param frame The frame in which execution is occuring
     * @param instruction The instruction which was or will be executed
     * @param loc The most recent location the code frame has been at
     * @param returnVal The return value of the instruction, Runners.NO_RETURN if none
     * @param error The error that the instruction threw, null if none
     * @param caught Whether or not the error has been caught
     * @return Whether or not the frame should restart
     */
    boolean onInstruction(Context ctx, CodeFrame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught);

    /**
     * Called immediatly after a frame has been popped out of the frame stack.
     * This function might pause in order to await debugging commands.
     * @param ctx The context of execution
     * @param frame The code frame which was popped out
     */
    void onFramePop(Context ctx, CodeFrame frame);
}
