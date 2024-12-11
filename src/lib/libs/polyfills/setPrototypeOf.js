import { object } from "../primordials";

export default function _setPrototypeOf(obj, proto) {
	object.setPrototype(obj, proto);
}