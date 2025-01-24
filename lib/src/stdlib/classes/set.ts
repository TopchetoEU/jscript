import { Array } from "../values/array.ts";
import { func, map } from "../primordials.ts";
import { symbols } from "../utils.ts";

export class Set<T> {
	#map: InstanceType<typeof map>;

	public get size() {
		return this.#map.size();
	}

	public has(key: T): boolean {
		return this.#map.has(key);
	}
	public add(val: T) {
		this.#map.set(val, true);
		return this;
	}
	public delete(val: T): boolean {
		if (!this.#map.has(val)) return false;
		else {
			this.#map.delete(val);
			return true;
		}
	}
	public clear() {
		this.#map.clear();
	}

	public keys(): T[] {
		return this.#map.keys();
	}
	public values(): T[] {
		return this.#map.keys();
	}
	public entries(): [T, T][] {
		const res = this.#map.keys();

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
		const _map = this.#map = new map();

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
	#map: InstanceType<typeof map>;

	public has(key: T): boolean {
		return this.#map.has(key);
	}
	public add(val: T) {
		this.#map.set(val, true);
		return this;
	}
	public delete(val: T): boolean {
		if (!this.#map.has(val)) return false;
		else {
			this.#map.delete(val);
			return true;
		}
	}
	public clear() {
		this.#map.clear();
	}

	public constructor(iterable?: Iterable<T>) {
		const _map = this.#map = new map(true);

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
