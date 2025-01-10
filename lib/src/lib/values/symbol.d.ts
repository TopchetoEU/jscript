declare interface Symbol {
	readonly description: string | undefined;

	valueOf(): symbol;
	toString(): string;
}
declare interface SymbolConstructor {
	(val?: string): symbol;
	for(val: string): symbol;

	readonly asyncIterator: unique symbol;
	readonly iterator: unique symbol;
	readonly match: unique symbol;
	readonly matchAll: unique symbol;
	readonly replace: unique symbol;
	readonly search: unique symbol;
	readonly split: unique symbol;
	readonly toStringTag: unique symbol;
}
