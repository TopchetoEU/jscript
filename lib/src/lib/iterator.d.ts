declare interface NormalIterationData<T> {
	value: T;
	done: true;
}
declare interface DoneIterationData<T> {
	value: T;
	done?: false;
}
declare type IterationData<T, Return> = NormalIterationData<T> | DoneIterationData<Return>;

declare interface Iterator<T, Return = unknown, Next = unknown> {
	next(): IterationData<T, Return>;
	next(val: Next): IterationData<T, Return>;
	error?(err: unknown): IterationData<T, Return>;
	return?(val: Return): IterationData<T, Return>;
}
declare interface IterableIterator<T, Return = unknown, Next = unknown> extends Iterator<T, Return, Next> {
	[Symbol.iterator](): this;
}
declare interface Iterable<T> {
	[Symbol.iterator](): Iterator<T>;
}
