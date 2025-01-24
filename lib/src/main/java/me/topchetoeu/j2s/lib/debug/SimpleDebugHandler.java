package me.topchetoeu.j2s.lib.debug;

import java.util.HashMap;
import java.util.WeakHashMap;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.FunctionBody;
import me.topchetoeu.j2s.common.FunctionMap;
import me.topchetoeu.j2s.runtime.Frame;
import me.topchetoeu.j2s.runtime.debug.DebugHandler;
import me.topchetoeu.j2s.runtime.exceptions.EngineException;
import me.topchetoeu.j2s.runtime.values.Value;

public class SimpleDebugHandler implements DebugHandler {
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

	public FunctionMap getMap(Environment env, FunctionBody func) {
		if (maps == null) return null;
		return maps.get(func);
	}

	public void onFramePop(Environment env, Frame frame) {
		if (debugger != null) debugger.onFramePop(env, frame);
	}
	public void onFramePush(Environment env, Frame frame) {
		if (debugger != null) debugger.onFramePush(env, frame);
	}

	@Override public boolean onInstruction(Environment env, Frame frame, Instruction instruction, Value returnVal, EngineException error, boolean caught) {
		if (debugger != null) return debugger.onInstruction(env, frame, instruction, returnVal, error, caught);
		else return false;
	}
	@Override public void onSourceLoad(Filename filename, String source) {
		if (debugger != null) debugger.onSourceLoad(filename, source);
		if (sources != null) sources.put(filename, source);
	}
	@Override public void onFunctionLoad(FunctionBody func, FunctionMap map) {
		if (maps != null) maps.put(func, map);
		if (debugger != null) debugger.onFunctionLoad(func, map);
	}

	public SimpleDebugHandler() {
		sources = new HashMap<>();
		maps = new WeakHashMap<>();
	}
}
