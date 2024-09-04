package me.topchetoeu.jscript.runtime.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.CodeFunction;
import me.topchetoeu.jscript.runtime.values.FunctionValue;

public class DebugContext {
    public static final Key<DebugContext> KEY = new Key<>();
    public static final Key<Void> IGNORE = new Key<>();

    private HashMap<Filename, String> sources;
    private WeakHashMap<FunctionBody, FunctionMap> maps;
    private DebugHandler debugger;

    public boolean attachDebugger(DebugHandler debugger) {
        if (this.debugger != null) return false;

        if (sources != null) {
            for (var source : sources.entrySet()) debugger.onSourceLoad(source.getKey(), source.getValue());
        }
        if (maps != null) {
            for (var map : maps.entrySet()) debugger.onFunctionLoad(map.getKey(), map.getValue());
        }

        this.debugger = debugger;
        return true;
    }
    public boolean detachDebugger(DebugHandler debugger) {
        if (this.debugger != debugger) return false;
        return detachDebugger();
    }
    public boolean detachDebugger() {
        this.debugger = null;
        return true;
    }

    public DebugHandler debugger() {
        if (debugger == null) return DebugHandler.empty();
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
        if (maps == null || !(func instanceof CodeFunction)) return FunctionMap.EMPTY;
        return getMapOrEmpty(((CodeFunction)func).body);
    }

    public void onFramePop(Context ctx, Frame frame) {
        if (debugger != null) debugger.onFramePop(ctx, frame);
    }
    public void onFramePush(Context ctx, Frame frame) {
        if (debugger != null) debugger.onFramePush(ctx, frame);
    }
    public boolean onInstruction(Context ctx, Frame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (debugger != null) return debugger.onInstruction(ctx, frame, instruction, returnVal, error, caught);
        else return false;
    }
    public void onSource(Filename filename, String source) {
        if (debugger != null) debugger.onSourceLoad(filename, source);
        if (sources != null) sources.put(filename, source);
    }
    public void onFunctionLoad(FunctionBody func, FunctionMap map) {
        if (maps != null) maps.put(func, map);
        if (debugger != null) debugger.onFunctionLoad(func, map);
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
        return exts != null && exts.hasNotNull(KEY) && !exts.has(IGNORE);
    }
    public static DebugContext get(Extensions exts) {
        if (enabled(exts)) return exts.get(KEY);
        else return new DebugContext(false);
    }

    public static List<String> stackTrace(Context ctx) {
        var res = new ArrayList<String>();
        var dbgCtx = get(ctx);

        for (var el : ctx.frames()) {
            var name = el.function.name;

            var map = dbgCtx.getMapOrEmpty(el.function);
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
