import { Boolean } from "./boolean.ts";
import { Error, RangeError, SyntaxError, TypeError } from "./errors.ts";
import { Function } from "./function.ts";
import { Number } from "./number.ts";
import { Object } from "./object.ts";
import { object, setGlobalPrototypes, target } from "./primordials.ts";
import { String } from "./string.ts";
import { Symbol } from "./symbol.ts";
import { Array } from "./array.ts";
import { Map } from "./map.ts";
import { RegExp } from "./regex.ts";

declare global {
	function print(...args: any[]): void;
}

object.defineField(target, "undefined", false, false, false, undefined);

target.Symbol = Symbol;
target.Number = Number;
target.String = String;
target.Boolean = Boolean;

target.Object = Object;
target.Function = Function;
target.Array = Array;

target.Error = Error;
target.RangeError = RangeError;
target.SyntaxError = SyntaxError;
target.TypeError = TypeError;

target.Map = Map;

target.parseInt = Number.parseInt;
target.parseFloat = Number.parseFloat;
target.NaN = Number.NaN;
target.Infinity = Number.POSITIVE_INFINITY;


target.RegExp = RegExp;

setGlobalPrototypes({
	string: String.prototype,
	number: Number.prototype,
	boolean: Boolean.prototype,
	symbol: Symbol.prototype,
	object: Object.prototype,
	array: Array.prototype,
	function: Function.prototype,
	error: Error.prototype,
	syntax: SyntaxError.prototype,
	range: RangeError.prototype,
	type: TypeError.prototype,
	regex: RegExp,
});
