import { Array } from "./array.ts";
import { func, map, symbol } from "./primordials.ts";
import { symbols } from "./utils.ts";

const mapKey: unique symbol = symbol.makeSymbol("Set.impl") as any;

export class Set<T> {
	private [mapKey]: InstanceType<typeof map>;

	public get size() {
		return this[mapKey].size();
	}

	public has(key: T): boolean {
		return this[mapKey].has(key);
	}
	public add(val: T) {
		this[mapKey].set(val, true);
		return this;
	}
	public delete(val: T): boolean {
		if (!this[mapKey].has(val)) return false;
		else {
			this[mapKey].delete(val);
			return true;
		}
	}
	public clear() {
		this[mapKey].clear();
	}

	public keys(): T[] {
		return this[mapKey].keys();
	}
	public values(): T[] {
		return this[mapKey].keys();
	}
	public entries(): [T, T][] {
		const res = this[mapKey].keys();

		for (let i = 0; i < res.length; i++) {
			res[i] = [res[i], res[i]];
		}
		return res;
	}

	public forEach(cb: Function, self?: any) {
		const vals = this.values();

		for (let i = 0; i < vals.length; i++) {
			func.invoke(cb, self, [vals[i], vals[i], this]);
		}
	}

	public [symbols.iterator](): Iterator<T> {
		return func.invoke(Array.prototype[symbols.iterator], this.values(), []) as any;
	}

	public constructor(iterable?: Iterable<T>) {
		const _map = this[mapKey] = new map();

		if (iterable != null) {
			if (Array.isArray(iterable)) {
				for (let i = 0; i < iterable.length; i++) {
					if (!(i in iterable)) continue;
					_map.set(iterable[i], true);
				}
			}
			else {
				const it = (iterable as any)[symbols.iterator]();
				for (let val = it.next(); !val.done; val = it.next()) {
					_map.set(val.value, true);
				}
			}
		}
	}
}

export class WeakSet<T> {
	private [mapKey]: InstanceType<typeof map>;

	public has(key: T): boolean {
		return this[mapKey].has(key);
	}
	public add(val: T) {
		this[mapKey].set(val, true);
		return this;
	}
	public delete(val: T): boolean {
		if (!this[mapKey].has(val)) return false;
		else {
			this[mapKey].delete(val);
			return true;
		}
	}
	public clear() {
		this[mapKey].clear();
	}

	public constructor(iterable?: Iterable<T>) {
		const _map = this[mapKey] = new map(true);

		if (iterable != null) {
			if (Array.isArray(iterable)) {
				for (let i = 0; i < iterable.length; i++) {
					if (!(i in iterable)) continue;
					_map.set(iterable[i], true);
				}
			}
			else {
				const it = (iterable as any)[symbols.iterator]();
				for (let val = it.next(); !val.done; val = it.next()) {
					_map.set(val.value, true);
				}
			}
		}
	}
}

func.setCallable(Set, false);
func.setCallable(WeakSet, false);
