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
}
export interface StringPrimordials {
	stringBuild(parts: string[]): string;
	fromCharCode(char: number): string;
	fromCodePoint(char: number): string;
}
export interface ObjectPrimordials {
	defineProperty(obj: object, key: string | number | symbol, enumerable: boolean, configurable: boolean, get?: Function, set?: Function): boolean;
	defineField(obj: object, key: string | number | symbol, writable: boolean, enumerable: boolean, configurable: boolean, value: any): boolean;
	getOwnMember(): any;
	getOwnSymbolMember(): any;
	getOwnMembers(obj: object, onlyEnumerable: boolean): string[];
	getOwnSymbolMembers(obj: object, onlyEnumerable: boolean): symbol[];
	getPrototype(obj: object): object | undefined;
	setPrototype(obj: object, proto?: object): object;
	isArray(obj: any[]): boolean;
}
export interface FunctionPrimordials {
	invokeType(args: IArguments, self: any): "new" | "call";
	invokeTypeInfer(): "new" | "call";
	target(): Function | null | undefined;
	setConstructable(func: Function, flag: boolean): void;
	setCallable(func: Function, flag: boolean): void;
	invoke(func: Function, self: any, args: any[]): void;
	construct(func: Function, self: any, args: any[]): void;
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
	map: new () => {
		get(key: any): any;
		has(key: any): boolean;
		set(key: any, val: any): void;
		delete(key: any): void;
		keys(): any[];
	};
	regex: new (source: string) => {
		exec(target: string, offset: number, indices: false): {
			
		};
	};
	compile(src: string): Function;
	setGlobalPrototypes(prototype: Record<string, any>): void;
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
} = primordials;
