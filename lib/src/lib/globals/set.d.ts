interface Set<V> {
	readonly size: number;
	add(val: V): this;
	has(key: V): boolean;
	delete(key: V): boolean;
	clear(): void;

	keys(): V[];
	values(): V[];
	entries(): [V, V][];

	forEach(cb: (val: V, key: V, map: this) => void, self?: any): void;

	[Symbol.iterator](): Iterator<V>;
}
interface SetConstructor {
	new <V>(iterable?: Iterable<V>): Set<V>;
}
