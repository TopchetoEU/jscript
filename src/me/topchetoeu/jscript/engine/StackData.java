package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.topchetoeu.jscript.engine.debug.DebugServer;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.exceptions.EngineException;

public class StackData {
    public static final DataKey<ArrayList<CodeFrame>> FRAMES = new DataKey<>();
    public static final DataKey<Integer> MAX_FRAMES = new DataKey<>();
    public static final DataKey<DebugServer> DEBUGGER = new DataKey<>();

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
        return true;
    }

    public static List<CodeFrame> frames(Context ctx) {
        return Collections.unmodifiableList(ctx.data.get(FRAMES, new ArrayList<>()));
    }
    public static List<String> stackTrace(Context ctx) {
        var res = new ArrayList<String>();

        for (var el : frames(ctx)) {
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
}
