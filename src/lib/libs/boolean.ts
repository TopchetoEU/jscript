import { func } from "./primordials.ts";
import { unwrapThis, valueKey } from "./utils.ts";

export const Boolean = (() => {
	class Boolean {
		[valueKey]!: boolean;

		public toString() {
			return "" + unwrapThis(this, "boolean", Boolean, "Boolean.prototype.toString");
		}
		public valueOf() {
			return unwrapThis(this, "boolean", Boolean, "Boolean.prototype.valueOf");
		}
	
		public constructor(value?: unknown) {
			if (func.invokeType(arguments, this) === "call") {
				if (arguments.length === 0) return false as any;
				else return !!value as any;
			}
			this[valueKey] = (Boolean as any)(value);
		}
	};

	func.setCallable(Boolean, true);
	func.setConstructable(Boolean, true);

	return Boolean as any as typeof Boolean & ((value?: unknown) => symbol);
})();
export type Boolean = InstanceType<typeof Boolean>;
