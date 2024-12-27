declare interface String {
	readonly length: number;
	[key: number]: string;

	toString(): string;
	valueOf(): string;

	at(index: number): string | undefined;
	charAt(i: number): string | undefined;
	charCodeAt(i: number): string;
	codePointAt(i: number): string;

	includes(search: string, offset?: number): number;
	indexOf(search: string, offset?: number): number;
	lastIndexOf(search: string, offset?: number): number;

	trim(): string;
	trimStart(): string;
	trimEnd(): string;
	toLowerCase(): string;
	toUpperCase(): string;

	split(val?: any, limit?: number): string[];
	replace(val: any, replacer: any): string;
	replaceAll(val: any, replacer: any): string;

	slice(start?: number, end?: number): string;
	substring(start?: number, end?: number): string;
	substr(start?: number, count?: number): string;

	[Symbol.iterator](): IterableIterator<string>;
}
declare interface StringConstructor {
	new (val: unknown): String;
	(val: unknown): string;

	fromCharCode(...chars: readonly number[]): string;
	fromCodePoint(...points: readonly number[]): string;
}
