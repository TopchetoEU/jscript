import { print, self, symbol } from "../stdlib/primordials.ts";
import { Object } from "../stdlib/values/object.ts";

self.Object = {
	defineProperty: function (obj, key, desc) {
		if (obj == null) return obj;
		Object.defineProperty(obj, key, desc);
	}
};

export default function _classPrivateFieldLooseKey(key) {
	return symbol.makeSymbol(key);
}