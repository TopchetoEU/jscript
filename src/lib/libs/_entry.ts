import { object, setGlobalPrototypes, target } from "./primordials.ts";
import { Error, RangeError, SyntaxError, TypeError } from "./errors.ts";
import { Boolean } from "./boolean.ts";
import { Function } from "./function.ts";
import { Number } from "./number.ts";
import { Object } from "./object.ts";
import { String } from "./string.ts";
import { Symbol } from "./symbol.ts";
import { Array } from "./array.ts";
import { Map, WeakMap } from "./map.ts";
import { RegExp } from "./regex.ts";
import { Date } from "./date.ts";
import { Math as _Math } from "./math.ts";
import { Set, WeakSet } from "./set.ts";
import { JSON } from "./json.ts";
import { encodeURI, encodeURIComponent } from "./url.ts";

declare global {
	function print(...args: any[]): void;
	function measure(func: Function): void;
}

function fixup<T extends Function>(clazz: T) {
	object.setPrototype(clazz, Function.prototype);
	object.setPrototype(clazz.prototype, Object.prototype);
	return clazz;
}

object.defineField(target, "undefined", { e: false, c: false, w: false, v: void 0 });

target.Symbol = fixup(Symbol);
target.Number = fixup(Number);
target.String = fixup(String);
target.Boolean = fixup(Boolean);

target.Object = Object;
target.Function = fixup(Function);
target.Array = fixup(Array);

target.Error = fixup(Error);
target.RangeError = RangeError;
target.SyntaxError = SyntaxError;
target.TypeError = TypeError;

target.Map = fixup(Map);
target.WeakMap = fixup(WeakMap);
target.Set = fixup(Set);
target.WeakSet = fixup(WeakSet);
target.RegExp = fixup(RegExp);
target.Date = fixup(Date);
target.Math = object.setPrototype(_Math, Object.prototype);
target.JSON = object.setPrototype(JSON, Object.prototype);

target.parseInt = Number.parseInt;
target.parseFloat = Number.parseFloat;
target.NaN = Number.NaN;
target.Infinity = Number.POSITIVE_INFINITY;
target.encodeURI = encodeURI;
target.encodeURIComponent = encodeURIComponent;

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
