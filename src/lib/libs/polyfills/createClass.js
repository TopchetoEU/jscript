import { object } from "../primordials.ts";

function _defineProperties(target, arr) {
	if (!arr) return;
	for (var i = 0; i < arr.length; i++) {
		var desc = arr[i];
		var res;
		
		if ("value" in desc) {
			res = object.defineField(target, desc.key, { w: desc.writable || true, e: desc.enumerable || true, c: desc.configurable || true, v: desc.value });
		}
		else {
			res = object.defineProperty(target, desc.key, { e: desc.enumerable || true, c: desc.configurable || true, g: desc.get, s: desc.set });
		}

		if (!res) throw "Couldn't set property";
	}
}

/* __#PURE__ */
export default function _createClass(clazz, instance, nonInstance) {
	_defineProperties(clazz.prototype, instance);
	_defineProperties(clazz, nonInstance);

	return clazz;
}