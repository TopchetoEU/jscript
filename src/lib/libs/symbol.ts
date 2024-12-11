import { func, object, symbol } from "./primordials.ts";
import { unwrapThis, valueKey } from "./utils.ts";

export const Symbol = (() => {
	class Symbol {
		[valueKey]!: symbol;

		get description() {
			return symbol.getSymbolDescription(unwrapThis(this, "symbol", Symbol, "Symbol.prototype.description"));
		}
	
		public toString() {
			return "Symbol(" + unwrapThis(this, "symbol", Symbol, "Symbol.prototype.toString").description + ")";
		}
		public valueOf() {
			return unwrapThis(this, "symbol", Symbol, "Symbol.prototype.valueOf");
		}
	
		public constructor(name = "") {
			return symbol.makeSymbol(name + "") as any;
		}
	
		public static for(name: string) {
			return symbol.getSymbol(name + "");
		}

		public static readonly asyncIterator: unique symbol;
		public static readonly iterator: unique symbol;
		public static readonly match: unique symbol;
		public static readonly matchAll: unique symbol;
		public static readonly replace: unique symbol;
		public static readonly search: unique symbol;
		public static readonly split: unique symbol;
		public static readonly toStringTag: unique symbol;
	};

	func.setCallable(Symbol, true);
	func.setConstructable(Symbol, false);

	object.defineField(Symbol, "asyncIterator", false, false, false, symbol.getSymbol("Symbol.asyncIterator"));
	object.defineField(Symbol, "iterator", false, false, false, symbol.getSymbol("Symbol.iterator"));
	object.defineField(Symbol, "match", false, false, false, symbol.getSymbol("Symbol.match"));
	object.defineField(Symbol, "matchAll", false, false, false, symbol.getSymbol("Symbol.matchAll"));
	object.defineField(Symbol, "replace", false, false, false, symbol.getSymbol("Symbol.replace"));
	object.defineField(Symbol, "search", false, false, false, symbol.getSymbol("Symbol.search"));
	object.defineField(Symbol, "split", false, false, false, symbol.getSymbol("Symbol.split"));
	object.defineField(Symbol, "toStringTag", false, false, false, symbol.getSymbol("Symbol.toStringTag"));
	
	return Symbol as any as typeof Symbol & ((name?: string) => symbol);
})();
export type Symbol = InstanceType<typeof Symbol>;

