import { func, map } from "./primordials.ts";
import { Symbol } from "./symbol.ts";

const mapKey: unique symbol = Symbol("Map.impl") as any;

export class Map<K, V> {
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

	public [Symbol.iterator](): Iterator<[K, V]> {
		return func.invoke(Array.prototype[Symbol.iterator as any], this.entries(), []) as any;
	}

	public constructor(iterable?: Iterable<[K, V]>) {
		const _map = this[mapKey] = new map();

		if (iterable != null) {
			const it = (iterable as any)[Symbol.iterator]();
			for (let val = it.next(); !val.done; val = it.next()) {
				_map.set(val.value[0], val.value[1]);
			}
		}
	}
}

func.setCallable(Map, false);
