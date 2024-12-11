import { object } from "../primordials.ts";

export default function _defineProperty(obj, key, val) {
	if (obj == null) return;
	object.defineField(obj, key, true, true, true, val);
	return obj;
}
