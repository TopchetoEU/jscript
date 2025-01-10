import { object } from "../stdlib/primordials";

export default function _setPrototypeOf(obj, proto) {
	object.setPrototype(obj, proto);
}