import { Array } from "./array.ts";
import { func, map, object, symbol } from "./primordials.ts";
import { symbols } from "./utils.ts";

const mapKey: unique symbol = symbol.makeSymbol("Map.impl") as any;

export class Map<K, V> {
	private [mapKey]: InstanceType<typeof map>;

	public get size() {
		return this[mapKey].size();
	}

	public get(key: K): V {
		return this[mapKey].get(key);
	}
	public has(key: K): boolean {
		return this[mapKey].has(key);
	}
	public set(key: K, val: V) {
		this[mapKey].set(key, val);
		return this;
	}
	public delete(key: K): boolean {
		if (!this[mapKey].has(key)) return false;
		else {
			this[mapKey].delete(key);
			return true;
		}
	}
	public clear() {
		this[mapKey].clear();
	}

	public keys(): K[] {
		return this[mapKey].keys();
	}
	public values(): V[] {
		const res = this[mapKey].keys();
		for (let i = 0; i < res.length; i++) {
			res[i] = this[mapKey].get(res[i]);
		}
		return res;
	}
	public entries(): [K, V][] {
		const res = this[mapKey].keys();
		for (let i = 0; i < res.length; i++) {
			res[i] = [res[i], this[mapKey].get(res[i])];
		}
		return res;
	}

	public forEach(cb: Function, self?: any) {
		const entries = this.entries();
		for (let i = 0; i < entries.length; i++) {
			func.invoke(cb, self, [entries[i][1], entries[i][0], this]);
		}
	}

	public [symbols.iterator](): Iterator<[K, V]> {
		return func.invoke(Array.prototype[symbols.iterator], this.entries(), []) as any;
	}

	public constructor(iterable?: Iterable<[K, V]>) {
		const _map = this[mapKey] = new map();

		if (iterable != null) {
			if (Array.isArray(iterable)) {
				for (let i = 0; i < iterable.length; i++) {
					if (!(i in iterable)) continue;
					_map.set(iterable[i][0], iterable[i][1]);
				}
			}
			else {
				const it = (iterable as any)[symbols.iterator]();
				for (let val = it.next(); !val.done; val = it.next()) {
					_map.set(val.value[0], val.value[1]);
				}
			}
		}
	}
}
export class WeakMap<K, V> {
	private [mapKey]: InstanceType<typeof map>;

	public get(key: K): V {
		return this[mapKey].get(key);
	}
	public has(key: K): boolean {
		return this[mapKey].has(key);
	}
	public set(key: K, val: V) {
		this[mapKey].set(key, val);
		return this;
	}
	public delete(key: K): boolean {
		if (!this[mapKey].has(key)) return false;
		else {
			this[mapKey].delete(key);
			return true;
		}
	}
	public clear() {
		this[mapKey].clear();
	}

	public constructor(iterable?: Iterable<[K, V]>) {
		const _map = this[mapKey] = new map(true);

		if (iterable != null) {
			if (Array.isArray(iterable)) {
				for (let i = 0; i < iterable.length; i++) {
					if (!(i in iterable)) continue;
					_map.set(iterable[i][0], iterable[i][1]);
				}
			}
			else {
				const it = (iterable as any)[symbols.iterator]();
				for (let val = it.next(); !val.done; val = it.next()) {
					_map.set(val.value[0], val.value[1]);
				}
			}
		}
	}
}

func.setCallable(Map, false);
func.setCallable(WeakMap, false);
