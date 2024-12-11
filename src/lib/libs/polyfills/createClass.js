import { object } from "../primordials.ts";

function _defineProperties(target, arr) {
	if (!arr) return;
	for (var t = 0; t < arr.length; t++) {
		var desc = arr[t];

		if ("value" in desc) {
			object.defineField(target, desc.key, desc.writable || true, desc.enumerable || false, desc.configurable || true, desc.value);
		}
		else {
			object.defineProperty(target, desc.key, desc.enumerable || false, desc.configurable || true, desc.get, desc.set);
		}
	}
}

/* __#PURE__ */
export default function _createClass(clazz, instance, nonInstance) {
	if (instance) {
		_defineProperties(clazz.prototype, instance);
		_defineProperties(clazz, nonInstance);
	}

	return clazz;
}