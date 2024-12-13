import { object } from "../primordials.ts";

export default function _inherits(t, e) {
	if (e == null) {
		object.setPrototype(t.prototype, undefined);
	}
	else {
		object.setPrototype(t.prototype, e.prototype);
		object.setPrototype(t, e);
	}
}
