package me.topchetoeu.jscript.core.engine.debug;

import java.util.HashMap;
import java.util.TreeSet;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.Extensions;
import me.topchetoeu.jscript.core.engine.frame.CodeFrame;
import me.topchetoeu.jscript.core.engine.values.Symbol;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.mapping.SourceMap;

public class DebugContext implements DebugController {
    public static final Symbol ENV_KEY = Symbol.get("Engine.debug");
    public static final Symbol IGNORE = Symbol.get("Engine.ignoreDebug");

    private HashMap<Filename, String> sources;
    private HashMap<Filename, TreeSet<Location>> bpts;
    private HashMap<Filename, SourceMap> maps;
    private DebugController debugger;

    public boolean attachDebugger(DebugController debugger) {
        if (this.debugger != null) return false;

        if (sources != null) {
            for (var source : sources.entrySet()) debugger.onSource(
                source.getKey(), source.getValue(),
                bpts.get(source.getKey()),
                maps.get(source.getKey())
            );
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

    @Override public void onFramePop(Context ctx, CodeFrame frame) {
        if (debugger != null) debugger.onFramePop(ctx, frame);
    }
    @Override public void onFramePush(Context ctx, CodeFrame frame) {
        if (debugger != null) debugger.onFramePush(ctx, frame);
    }
    @Override public boolean onInstruction(Context ctx, CodeFrame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (debugger != null) return debugger.onInstruction(ctx, frame, instruction, returnVal, error, caught);
        else return false;
    }
    @Override public void onSource(Filename filename, String source, TreeSet<Location> breakpoints, SourceMap map) {
        if (debugger != null) debugger.onSource(filename, source, breakpoints, map);
        if (sources != null) sources.put(filename, source);
        if (bpts != null) bpts.put(filename, breakpoints);
        if (maps != null) maps.put(filename, map);
    }

    public Location mapToCompiled(Location location) {
        if (maps == null) return location;

        var map = maps.get(location.filename());
        if (map == null) return location;
        return map.toCompiled(location);
    }
    public Location mapToOriginal(Location location) {
        if (maps == null) return location;

        var map = maps.get(location.filename());
        if (map == null) return location;
        return map.toOriginal(location);
    }

    private DebugContext(boolean enabled) {
        if (enabled) {
            sources = new HashMap<>();
            bpts = new HashMap<>();
            maps = new HashMap<>();
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
}
