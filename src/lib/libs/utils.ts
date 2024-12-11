import { symbol } from "./primordials.ts";

export const valueKey: unique symbol = symbol.makeSymbol("Primitive.value") as any;

export interface TypeMap {
	undefined: undefined;
	boolean: boolean;
	string: string;
	number: number;
	symbol: symbol;
	object: null | object;
	function: Function;
}

export function unwrapThis<T extends keyof TypeMap>(self: any, type: T, constr: Function, name: string, arg = "this", defaultVal?: TypeMap[T]): TypeMap[T] {
	if (typeof self === type) return self;
	if (self instanceof constr && valueKey in self) self = (self as any)[valueKey];
	if (typeof self === type) return self;
	if (defaultVal !== undefined) return defaultVal;
	throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
}