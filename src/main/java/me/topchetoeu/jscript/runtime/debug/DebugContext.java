package me.topchetoeu.jscript.runtime.debug;

import java.util.HashMap;
import java.util.WeakHashMap;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.mapping.FunctionMap;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;

public class DebugContext {
	public static final Key<DebugContext> KEY = new Key<>();
	public static final Key<Void> IGNORE = new Key<>();

	private HashMap<Filename, String> sources;
	private WeakHashMap<FunctionBody, FunctionMap> maps;
	private DebugHandler debugger;

	public synchronized boolean attachDebugger(DebugHandler debugger) {
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
		return debugger;
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

	public void onFramePop(Environment env, Frame frame) {
		if (debugger != null) debugger.onFramePop(env, frame);
	}
	public void onFramePush(Environment env, Frame frame) {
		if (debugger != null) debugger.onFramePush(env, frame);
	}

	public boolean onInstruction(Environment env, Frame frame, Instruction instruction, Value returnVal, EngineException error, boolean caught) {
		if (debugger != null) return debugger.onInstruction(env, frame, instruction, returnVal, error, caught);
		else return false;
	}
	public boolean onInstruction(Environment env, Frame frame, Instruction instruction) {
		if (debugger != null) return debugger.onInstruction(env, frame, instruction, null, null, false);
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

	public static boolean enabled(Environment exts) {
		return exts != null && exts.hasNotNull(KEY) && !exts.has(IGNORE);
	}
	public static DebugContext get(Environment exts) {
		if (enabled(exts)) return exts.get(KEY);
		else return new DebugContext(false);
	}
}
