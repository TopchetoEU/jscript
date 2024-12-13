import { now, symbol } from "./primordials.ts";

const timeKey: unique symbol = symbol.makeSymbol("") as any;

export const Date = (() => {
	class Date {
		[timeKey]!: number;

		public constructor() {

		}

		public static now() {
			return now();
		}
	};

	return Date as any as typeof Date & ((val?: unknown) => string);
})();
export type Date = InstanceType<typeof Date>;
