import { object } from "../primordials.ts";

function _defineProperties(target, arr) {
	if (!arr) return;
	for (var i = 0; i < arr.length; i++) {
		var desc = arr[i];
		var res;
		var w, e, c;

		c = desc.configurable;
		if (c == null) c = true;
		e = desc.enumerable;
		if (e == null) e = false;

		if ("value" in desc) {
			w = desc.writable;
			if (w == null) w = true;

			if (desc.writable == null) 
			res = object.defineField(target, desc.key, { w: !!w, e: !!e, c: !!c, v: desc.value });
		}
		else {
			res = object.defineProperty(target, desc.key, { e: !!e, c: !!c, g: desc.get, s: desc.set });
		}

		if (!res) throw "Couldn't set property";
	}
}

export default function _createClass(clazz, instance, nonInstance) {
	_defineProperties(clazz.prototype, instance);
	_defineProperties(clazz, nonInstance);

	return clazz;
}