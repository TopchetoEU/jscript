import { now, number, symbol } from "../primordials.ts";

export const Date = (() => {
	class Date {
		#time: number;

		public constructor() {
			this.#time = number.NaN;
		}

		public static now() {
			return now();
		}
	};

	return Date as any as typeof Date & ((val?: unknown) => string);
})();
export type Date = InstanceType<typeof Date>;
