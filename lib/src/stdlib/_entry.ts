import { now, object, print, setGlobalPrototypes, setIntrinsic, target } from "./primordials.ts";
import { Error, RangeError, SyntaxError, TypeError } from "./values/errors.ts";
import { Boolean } from "./values/boolean.ts";
import { Function } from "./values/function.ts";
import { Number } from "./values/number.ts";
import { Object } from "./values/object.ts";
import { String } from "./values/string.ts";
import { Symbol } from "./values/symbol.ts";
import { Array } from "./values/array.ts";
import { Map, WeakMap } from "./classes/map.ts";
import { RegExp } from "./values/regex.ts";
import { Date } from "./classes/date.ts";
import { Math as _Math } from "./namespaces/math.ts";
import { Set, WeakSet } from "./classes/set.ts";
import { JSON } from "./namespaces/json.ts";
import { console } from "./namespaces/console.ts";
import { encodeURI, encodeURIComponent } from "./url.ts";
import { Promise } from "./classes/promise.ts";
import { ArrayBuffer } from "./arrays/ArrayBuffer.ts";
import { Uint8Array } from "./arrays/Uint8Array.ts";
import { Int32Array } from "./arrays/Int32Array.ts";
import { TypedArray } from "./arrays/TypedArray.ts";

function fixup<T extends Function>(clazz: T) {
	object.setPrototype(clazz, Function.prototype);
	object.setPrototype(clazz.prototype as any, Object.prototype);
	return clazz;
}

object.setPrototype(target, Object.prototype);

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

fixup(TypedArray);
target.ArrayBuffer = fixup(ArrayBuffer);
target.Uint8Array = Uint8Array;
target.Int32Array = Int32Array;

target.Map = fixup(Map);
target.WeakMap = fixup(WeakMap);
target.Set = fixup(Set);
target.WeakSet = fixup(WeakSet);
target.RegExp = fixup(RegExp);
target.Date = fixup(Date);
target.Promise = fixup(Promise);
target.Math = object.setPrototype(_Math, Object.prototype);
target.JSON = object.setPrototype(JSON, Object.prototype);
target.console = object.setPrototype(console, Object.prototype);

target.parseInt = Number.parseInt;
target.parseFloat = Number.parseFloat;
target.NaN = Number.NaN;
target.Infinity = Number.POSITIVE_INFINITY;
target.encodeURI = encodeURI;
target.encodeURIComponent = encodeURIComponent;
target.print = print;
target.measure = (func: () => void) => {
	const start = now();
	try {
		return func();
	}
	finally {
		print(`Took ${now() - start}ms`);
	}
};

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
	uint8: Uint8Array.prototype,
	int32: Int32Array.prototype,
});
setIntrinsic("regex", RegExp);
setIntrinsic("keys", (obj: object, own: boolean, onlyEnumerable: boolean) => {
	const members = object.getMembers(obj, own, onlyEnumerable);

	let i = 0;
	return () => {
		if (i >= members.length) return { done: true };
		else return { value: members[i++] };
	};
});
setIntrinsic("defGetter", (obj: object, key: any, func: Function) => {
	object.defineProperty(obj, key, { g: func, e: true, c: true });
});
setIntrinsic("defSetter", (obj: object, key: any, func: Function) => {
	object.defineProperty(obj, key, { s: func, e: true, c: true });
});