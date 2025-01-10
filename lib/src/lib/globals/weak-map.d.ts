interface WeakMap<K, V> {
	readonly size: number;
	get(key: K): V;
	set(key: K, val: V): this;
	has(key: K): boolean;
	delete(key: K): boolean;
	clear(): void;
}
interface WeakMapConstructor {
	new <K, V>(iterable?: Iterable<[K, V]>): WeakMap<K, V>;
}
