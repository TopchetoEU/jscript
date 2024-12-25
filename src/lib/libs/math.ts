import { number, object } from "./primordials";

export const Math = {};

function method(name: string, func: Function) {
	object.defineField(Math, name, { c: true, e: false, w: true, v: func });
}

method("max", function max() {
	let res = -number.Infinity;

	for (let i = 0; i < arguments.length; i++) {
		if (res < arguments[i]) res = arguments[i];
	}

	return res;
});

method("min", function min() {
	let res = +number.Infinity;

	for (let i = 0; i < arguments.length; i++) {
		if (res > arguments[i]) res = arguments[i];
	}

	return res;
});

method("abs", function abs(val: number) {
	val = +val;
	if (val < 0) return -val;
	else return val;
});

method("floor", function floor(val: number) {
	val = val - 0;
	if (number.isNaN(val)) return number.NaN;

	let rem = val % 1;
	if (rem < 0) rem += 1;

	return val - rem;
});

method("pow", function pow(a: number, b: number) {
	return number.pow(a, b);
});
