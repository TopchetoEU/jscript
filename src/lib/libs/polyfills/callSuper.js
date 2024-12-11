import { func, object } from "../primordials.ts";

export default function _callSuper(self, constr, args) {
	return func.construct(object.getPrototype(constr), func.target(1), args || []);
}
