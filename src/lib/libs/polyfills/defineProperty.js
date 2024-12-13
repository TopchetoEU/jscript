import { object } from "../primordials.ts";

export default function _defineProperty(obj, key, val) {
	if (obj == null) return;
	object.defineField(obj, key, { c: true, e: true, w: true, v: val });
	return obj;
}
