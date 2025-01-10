import { object } from "../stdlib/primordials.ts";

export default function _getPrototypeOf(obj) {
	return object.getPrototype(obj) || null;
}