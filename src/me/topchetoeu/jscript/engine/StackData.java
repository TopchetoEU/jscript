package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.topchetoeu.jscript.engine.debug.Debugger;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.exceptions.EngineException;

public class StackData {
    public static final DataKey<ArrayList<CodeFrame>> FRAMES = new DataKey<>();
    public static final DataKey<Integer> MAX_FRAMES = new DataKey<>();
    public static final DataKey<Debugger> DEBUGGER = new DataKey<>();

    public static void pushFrame(Context ctx, CodeFrame frame) {
        var frames = ctx.data.get(FRAMES, new ArrayList<>());
        frames.add(frame);
        if (frames.size() > ctx.data.get(MAX_FRAMES, 10000)) throw EngineException.ofRange("Stack overflow!");
        ctx.pushEnv(frame.function.environment);
    }
    public static boolean popFrame(Context ctx, CodeFrame frame) {
        var frames = ctx.data.get(FRAMES, new ArrayList<>());
        if (frames.size() == 0) return false;
        if (frames.get(frames.size() - 1) != frame) return false;
        frames.remove(frames.size() - 1);
        ctx.popEnv();
        var dbg = getDebugger(ctx);
        if (dbg != null) dbg.onFramePop(ctx, frame);
        return true;
    }
    public static CodeFrame peekFrame(Context ctx) {
        var frames = ctx.data.get(FRAMES, new ArrayList<>());
        if (frames.size() == 0) return null;
        return frames.get(frames.size() - 1);
    }

    public static List<CodeFrame> frames(Context ctx) {
        return Collections.unmodifiableList(ctx.data.get(FRAMES, new ArrayList<>()));
    }
    public static List<String> stackTrace(Context ctx) {
        var res = new ArrayList<String>();
        var frames = frames(ctx);

        for (var i = frames.size() - 1; i >= 0; i--) {
            var el = frames.get(i);
            var name = el.function.name;
            var loc = el.function.loc();
            var trace = "";

            if (loc != null) trace += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) trace += "in " + name + " ";

            trace = trace.trim();

            if (!trace.equals("")) res.add(trace);
        }

        return res;
    }

    public static Debugger getDebugger(Context ctx) {
        return ctx.data.get(DEBUGGER);
    }
}
