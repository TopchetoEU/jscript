package me.topchetoeu.j2s.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The class, used to keep track of important environment-wide values
 * Supports an inheritance mechanism via parents
 * In essence a simple hashmap of flag objects to values
 */
public class Environment {
	/**
	 * The parent of this environment. Will act as a "fallback" when searching for elements.
	 * Operations this environment won't write into the parent
	 */
	public final Environment parent;
	private final Map<Key<Object>, Object> map = new HashMap<>();
	private final Set<Key<Object>> hidden = new HashSet<>();

	/**
	 * Gets the element, contained in this environment, signified by the given key
	 * @return The element, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Key<T> key) {
		if (map.containsKey(key)) return (T)map.get(key);
		else if (!hidden.contains(key) && parent != null) return parent.get(key);
		else return null;
	}
	/**
	 * Checks if the environment has the given key
	 */
	public boolean has(Key<?> key) {
		if (map.containsKey(key)) return true;
		else if (!hidden.contains(key) && parent != null) return parent.has(key);
		else return false;
	}

	/**
	 * Checks if the environment has the given key and if the value behind they key is not null
	 */
	public boolean hasNotNull(Key<?> key) {
		return get(key) != null;
	}

	/**
	 * Gets the element, contained in this environment, signified by the given key
	 * @param defaultVal The value to return if the element is not found
	 * @return The element, or "defaultVal" if not found
	 */
	public <T> T get(Key<T> key, T defaultVal) {
		if (has(key)) return get(key);
		else return defaultVal;
	}
	/**
	 * Gets the element, contained in this environment, signified by the given key
	 * @param defaultVal The supplier, from which to return if the element is not found
	 * @return The element, or the result of "defaultVal" if not found
	 */
	public <T> T getWith(Key<T> key, Supplier<T> defaultVal) {
		if (has(key)) return get(key);
		else return defaultVal.get();
	}

	/**
	 * Inserts the given value for the given key, replacing any existing value
	 * If a parent has a value with the same key, it isn't replaced, but instead - shadowed
	 * @return The inserted element
	 */
	@SuppressWarnings("unchecked")
	public <T> Environment add(Key<T> key, T val) {
		map.put((Key<Object>)key, val);
		hidden.remove(key);
		return this;
	}
	/**
	 * Adds the flag key to the environment
	 * @return The environment instance
	 */
	public Environment add(Key<Void> key) {
		return add(key, null);
	}
	/**
	 * Executes Environment.add for each pair of the map
	 * @return The environment instance
	 */
	@SuppressWarnings("all")
	public Environment addAll(Map<Key<?>, ?> map) {
		map.putAll((Map)map);
		hidden.removeAll(map.keySet());
		return this;
	}

	/**
	 * Removes the given key from the environment.
	 * If a parent has the given key, it is instead just "hidden" by this environment
	 * @return The environment instance
	 */
	@SuppressWarnings("unchecked")
	public Environment remove(Key<?> key) {
		map.remove(key);
		hidden.add((Key<Object>)key);
		return this;
	}

	/**
	 * If the key exists in the environment, returns it. Otherwise, puts the given value and returns the value
	 */
	public <T> T init(Key<T> key, T val) {
		if (!has(key)) {
			this.add(key, val);
			return val;
		}
		else return get(key);
	}
	/**
	 * If the key exists in the environment, returns it. Otherwise, puts the given value from the supplier and returns it
	 */
	public <T> T initFrom(Key<T> key, Supplier<T> val) {
		if (!has(key)) {
			var res = val.get();
			this.add(key, res);
			return res;
		}
		else return get(key);
	}

	/**
	 * Creates an environment that is a child of this environment
	 * @return
	 */
	public Environment child() {
		return new Environment(this);
	}

	public Environment(Environment parent) {
		this.parent = parent;
	}
	/**
	 * Creates an empty environment
	 */
	public Environment() {
		this.parent = null;
	}

	/**
	 * If the environment is null, returns an empty environment
	 * Otherwise, returns the given value
	 */
	public static Environment wrap(Environment env) {
		if (env == null) return empty();
		else return env;
	}

	/**
	 * Returns a newly-created environment
	 */
	public static Environment empty() {
		return new Environment();
	}
}
