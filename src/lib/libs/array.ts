import { func, object } from "./primordials.ts";
import { Symbol } from "./symbol.ts";

export const Array = (() => {
	class Array {
		public forEach(this: any[], cb: (val: any, i: number, self: this[]) => void, self?: any) {
			for (let i = 0; i < this.length; i++) {
				if (i in this) func.invoke(cb, self, [this[i], i, this]);
			}
		}

		public [Symbol.iterator](this: any[]) {
			let i = 0;
			let arr: any[] | undefined = this;

			return {
				next() {
					if (arr == null) return { done: true, value: undefined };
					if (i > arr.length) {
						arr = undefined;
						return { done: true, value: undefined };
					}
					else {
						const val = arr[i++];
						if (i >= arr.length) arr = undefined;
						return { done: false, value: val };
					}
				},
				[Symbol.iterator]() { return this; }
			};
		}

		public constructor (len: unknown) {
			if (arguments.length === 1 && typeof len === "number") {
				const res: any[] = [];
				res.length = len;
				return res as any;
			}
			// TODO: Implement spreading
			else throw new Error("Spreading not implemented");
		}

		public static isArray(val: any[]) {
			object.isArray(val);
		}
	}

	func.setCallable(Array, true);
	func.setConstructable(Array, true);

	return Array as any as typeof Array & ((value?: unknown) => object);
})();
export type Array = InstanceType<typeof Array>;
