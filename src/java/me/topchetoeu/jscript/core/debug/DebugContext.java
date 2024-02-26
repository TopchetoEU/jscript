package me.topchetoeu.jscript.core.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.FunctionBody;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.mapping.FunctionMap;
import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Extensions;
import me.topchetoeu.jscript.core.Frame;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.Symbol;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public class DebugContext implements DebugController {
    public static final Symbol ENV_KEY = Symbol.get("Engine.debug");
    public static final Symbol IGNORE = Symbol.get("Engine.ignoreDebug");

    private HashMap<Filename, String> sources;
    private WeakHashMap<FunctionBody, FunctionMap> maps;
    private DebugController debugger;

    public boolean attachDebugger(DebugController debugger) {
        if (this.debugger != null) return false;

        if (sources != null) {
            for (var source : sources.entrySet()) debugger.onSource(source.getKey(), source.getValue());
        }

        this.debugger = debugger;
        return true;
    }
    public boolean detachDebugger() {
        this.debugger = null;
        return true;
    }

    public DebugController debugger() {
        if (debugger == null) return DebugController.empty();
        else return debugger;
    }

    public FunctionMap getMap(FunctionBody func) {
        if (maps == null) return null;
        return maps.get(func);
    }
    public FunctionMap getMap(FunctionValue func) {
        if (maps == null || !(func instanceof CodeFunction)) return null;
        return getMap(((CodeFunction)func).body);
    }
    public FunctionMap getMapOrEmpty(FunctionBody func) {
        if (maps == null) return FunctionMap.EMPTY;
        var res = maps.get(func);
        if (res == null) return FunctionMap.EMPTY;
        else return res;
    }
    public FunctionMap getMapOrEmpty(FunctionValue func) {
        if (maps == null || !(func instanceof CodeFunction)) return null;
        return getMapOrEmpty(((CodeFunction)func).body);
    }

    @Override public void onFramePop(Context ctx, Frame frame) {
        if (debugger != null) debugger.onFramePop(ctx, frame);
    }
    @Override public void onFramePush(Context ctx, Frame frame) {
        if (debugger != null) debugger.onFramePush(ctx, frame);
    }
    @Override public boolean onInstruction(Context ctx, Frame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (debugger != null) return debugger.onInstruction(ctx, frame, instruction, returnVal, error, caught);
        else return false;
    }
    @Override public void onSource(Filename filename, String source) {
        if (debugger != null) debugger.onSource(filename, source);
        if (sources != null) sources.put(filename, source);
    }

    private DebugContext(boolean enabled) {
        if (enabled) {
            sources = new HashMap<>();
            maps = new WeakHashMap<>();
        }
    }

    public DebugContext() {
        this(true);
    }

    public static boolean enabled(Extensions exts) {
        return exts.hasNotNull(ENV_KEY) && !exts.has(IGNORE);
    }
    public static DebugContext get(Extensions exts) {
        if (enabled(exts)) return exts.get(ENV_KEY);
        else return new DebugContext(false);
    }

    public static List<String> stackTrace(Context ctx) {
        var res = new ArrayList<String>();
        var dbgCtx = get(ctx);

        for (var el : ctx.frames()) {
            var name = el.function.name;

            var map = dbgCtx.getMap(el.function);
            Location loc = null;

            if (map != null) {
                loc = map.toLocation(el.codePtr, true);
                if (loc == null) loc = map.start();
            }

            var trace = "";

            if (loc != null) trace += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) trace += "in " + name + " ";

            trace = trace.trim();

            if (!trace.equals("")) res.add(trace);
        }

        return res;
    }
}
