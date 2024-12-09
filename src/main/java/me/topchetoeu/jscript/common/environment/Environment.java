package me.topchetoeu.jscript.common.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class Environment {
	public final Environment parent;
	private final Map<Key<Object>, Object> map = new HashMap<>();
	private final Set<Key<Object>> hidden = new HashSet<>();

	@SuppressWarnings("unchecked")
	public <T> T get(Key<T> key) {
		if (map.containsKey(key)) return (T)map.get(key);
		else if (!hidden.contains(key) && parent != null) return parent.get(key);
		else return null;
	}
	public boolean has(Key<?> key) {
		if (map.containsKey(key)) return true;
		else if (!hidden.contains(key) && parent != null) return parent.has(key);
		else return false;
	}

	public boolean hasNotNull(Key<?> key) {
		return get(key) != null;
	}

	public <T> T get(Key<T> key, T defaultVal) {
		if (has(key)) return get(key);
		else return defaultVal;
	}
	public <T> T getWith(Key<T> key, Supplier<T> defaultVal) {
		if (has(key)) return get(key);
		else return defaultVal.get();
	}

	@SuppressWarnings("unchecked")
	public <T> Environment add(Key<T> key, T val) {
		map.put((Key<Object>)key, val);
		hidden.remove(key);
		return this;
	}
	public Environment add(Key<Void> key) {
		return add(key, null);
	}
	@SuppressWarnings("all")
	public Environment addAll(Map<Key<?>, ?> map, boolean iterableAsMulti) {
		map.putAll((Map)map);
		hidden.removeAll(map.keySet());
		return this;
	}
	public Environment addAll(Map<Key<?>, ?> map) {
		return addAll(map, true);
	}

	@SuppressWarnings("unchecked")
	public Environment remove(Key<?> key) {
		map.remove(key);
		hidden.add((Key<Object>)key);
		return this;
	}

	public <T> T init(Key<T> key, T val) {
		if (!has(key)) this.add(key, val);
		return val;
	}
	public <T> T initFrom(Key<T> key, Supplier<T> val) {
		if (!has(key)) {
			var res = val.get();
			this.add(key, res);
			return res;
		}
		else return get(key);
	}

	public Environment child() {
		return new Environment(this);
	}

	public Environment(Environment parent) {
		this.parent = parent;
	}
	public Environment() {
		this.parent = null;
	}

	public static Environment wrap(Environment env) {
		if (env == null) return empty();
		else return env;
	}

	public static Environment empty() {
		return new Environment();
	}
}
