export interface SymbolPrimordials {
	makeSymbol(name: string): symbol;
	getSymbol(name: string): symbol;
	getSymbolKey(symbol: symbol): string | undefined;
	getSymbolDescription(symbol: symbol): string;
}
export interface NumberPrimordials {
	parseInt(raw: string | number, radix?: number): number;
	parseFloat(raw: string | number): number;
	isNaN(num: number): boolean;
	NaN: number;
	Infinity: number;

	pow(a: number, b: number): number;
}
export interface StringPrimordials {
	stringBuild(parts: string[]): string;
	fromCharCode(char: number): string;
	fromCodePoint(char: number): string;
	toCharCode(char: string): number;
	toCodePoint(char: string, i: number): number;
	indexOf(str: string, search: string, start: number, reverse?: boolean): number;
	substring(str: string, start: number, end: number): string;
	lower(str: string): string;
	upper(str: string): string;
}
export interface ObjectPrimordials {
	defineProperty(obj: object, key: string | number | symbol, conf: { g?: Function, s?: Function, e?: boolean, c?: boolean }): boolean;
	defineField(obj: object, key: string | number | symbol, conf: { v?: any, e?: boolean, c?: boolean, w?: boolean }): boolean;
	getOwnMember(obj: object, key: any): PropertyDescriptor | undefined;
	getOwnMembers(obj: object, onlyEnumerable: boolean): string[];
	getOwnSymbolMembers(obj: object, onlyEnumerable: boolean): symbol[];
	getPrototype(obj: object): object | undefined;
	setPrototype(obj: object, proto?: object): object;
	isArray(obj: any[]): boolean;

	preventExt(obj: object): void;
	seal(obj: object): void;
	freeze(obj: object): void;

	memcpy(src: any[], dst: any[], srcI: number, dstI: number, n: number): void;
	sort(arr: any[], cb: Function): any[];
}
export interface FunctionPrimordials {
	invokeType(args: IArguments, self: any): "new" | "call";
	invokeTypeInfer(): "new" | "call";
	target(): Function | null | undefined;
	setConstructable(func: Function, flag: boolean): void;
	setCallable(func: Function, flag: boolean): void;
	invoke(func: Function, self: any, args: any[]): any;
	construct(func: Function, self: any, args: any[]): any;
}
export interface JSONPrimordials {
	parse(data: string): any;
	stringify(data: any): string;
}

export interface Primordials {
	symbol: SymbolPrimordials;
	number: NumberPrimordials;
	string: StringPrimordials;
	object: ObjectPrimordials;
	function: FunctionPrimordials;
	json: JSONPrimordials;
	map: new (weak?: boolean) => {
		get(key: any): any;
		has(key: any): boolean;
		set(key: any, val: any): void;
		delete(key: any): void;
		keys(): any[];
		clear(): void;
		size(): number;
	};

	regex: new (source: string, multiline?: boolean, noCase?: boolean, dotall?: boolean, unicode?: boolean, unicodeClass?: boolean) => {
		exec(target: string, offset: number, indices: boolean): { matches: RegExpMatchArray, end: number } | null;
		groupCount(): number;
	};
	compile(src: string): Function;
	setGlobalPrototypes(prototype: Record<string, any>): void;
	now(): number;
	next(func: () => void): void;
	schedule(func: () => void, delay: number): () => void;
}

globalThis.undefined = void 0;
export const target = (globalThis as any).target;
export const primordials: Primordials = (globalThis as any).primordials;

export const {
	symbol,
	number,
	string,
	object,
	function: func,
	json,
	map,
	regex,
	setGlobalPrototypes,
	compile,
	now,
	next,
	schedule,
} = primordials;

export type regex = InstanceType<typeof regex>;