declare interface Array<T> {
	length: number;
	[i: number]: T;

	forEach(this: T[], cb: (val: T, i: number, self: this) => void, self?: any): void;
	join(this: T[], delim?: string): void;

	push(this: T[], ...elements: T[]): number;
	pop(this: T[]): T | undefined;

	unshift(this: T[], ...elements: T[]): number;
	shift(this: T[]): T | undefined;

	concat(this: T[], ...elements: (T | T[])[]): T | undefined;
	slice(this: T[], start?: number, end?: number): T | undefined;
	splice(this: T[], start?: number, count?: number): T[];
	splice(this: T[], start: number | undefined, count: number | undefined, ...elements: T[]): T[];

	map<T2>(this: T[], cb: (val: T, i: number, self: this) => T2, self?: any): T2[];
	filter(this: T[], cb: (val: T, i: number, self: this) => boolean, self?: any): T[];
	some(this: T[], cb: (val: T, i: number, self: this) => boolean, self?: any): boolean;
	find(this: T[], cb: (val: T, i: number, self: this) => boolean, self?: any): T | undefined;
	indexOf(this: T[], el: T, start?: number): number;
	lastIndexOf(this: T[], el: T, start?: number): number;

	sort(this: T[], cb?: (a: T, b: T) => number): T[];

	[Symbol.iterator](): IterableIterator<T>;
}
declare interface ArrayConstructor {
	new <T>(len: number): T[];
	new <T>(...elements: T[]): T[];

	<T>(len: number): T[];
	<T>(...elements: T[]): T[];

	isArray(val: any): val is any[];
	// from(val: any): val is any[];
}
