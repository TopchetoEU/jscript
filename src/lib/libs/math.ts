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

method("floor", function floor(val: number) {
	val = val - 0;
	if (number.isNaN(val)) return number.NaN;

	let rem = val % 1;
	if (rem < 0) rem += 1;

	return val - rem;
});