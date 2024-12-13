import { func, object, symbol } from "./primordials.ts";
import { symbols, unwrapThis, valueKey } from "./utils.ts";

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

		declare public static readonly asyncIterator: unique symbol;
		declare public static readonly iterator: unique symbol;
		declare public static readonly match: unique symbol;
		declare public static readonly matchAll: unique symbol;
		declare public static readonly replace: unique symbol;
		declare public static readonly search: unique symbol;
		declare public static readonly split: unique symbol;
		declare public static readonly toStringTag: unique symbol;
	};

	func.setCallable(Symbol, true);
	func.setConstructable(Symbol, false);

	object.defineField(Symbol, "asyncIterator", { c: false, e: false, w: false, v: symbols.asyncIterator });
	object.defineField(Symbol, "iterator", { c: false, e: false, w: false, v: symbols.iterator });
	object.defineField(Symbol, "match", { c: false, e: false, w: false, v: symbols.match });
	object.defineField(Symbol, "matchAll", { c: false, e: false, w: false, v: symbols.matchAll });
	object.defineField(Symbol, "replace", { c: false, e: false, w: false, v: symbols.replace });
	object.defineField(Symbol, "search", { c: false, e: false, w: false, v: symbols.search });
	object.defineField(Symbol, "split", { c: false, e: false, w: false, v: symbols.split });
	object.defineField(Symbol, "toStringTag", { c: false, e: false, w: false, v: symbols.toStringTag });
	object.defineField(Symbol, "isConcatSpreadable", { c: false, e: false, w: false, v: symbols.isConcatSpreadable });
	
	return Symbol as any as typeof Symbol & ((name?: string) => ReturnType<SymbolConstructor>);
})();
export type Symbol = InstanceType<typeof Symbol>;

