interface Map<K, V> {
	readonly size: number;
	get(key: K): V;
	has(key: K): boolean;
	set(key: K, val: V): this;
	delete(key: K): boolean;
	clear(): void;

	keys(): K[];
	values(): V[];
	entries(): [K, V][];

	forEach(cb: (val: V, key: K, map: this) => void, self?: any): void;

	[Symbol.iterator](): Iterator<[K, V]>;
}
interface MapConstructor {
	new <K, V>(iterable?: Iterable<[K, V]>): Map<K, V>;
}
