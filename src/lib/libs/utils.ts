import { func, string, symbol } from "./primordials.ts";

export const valueKey: unique symbol = symbol.makeSymbol("Primitive.value") as any;
export namespace symbols {
	export const asyncIterator: unique symbol = symbol.makeSymbol("Symbol.asyncIterator") as any;
	export const iterator: unique symbol = symbol.makeSymbol("Symbol.iterator") as any;
	export const match: unique symbol = symbol.makeSymbol("Symbol.match") as any;
	export const matchAll: unique symbol = symbol.makeSymbol("Symbol.matchAll") as any;
	export const replace: unique symbol = symbol.makeSymbol("Symbol.replace") as any;
	export const search: unique symbol = symbol.makeSymbol("Symbol.search") as any;
	export const split: unique symbol = symbol.makeSymbol("Symbol.split") as any;
	export const toStringTag: unique symbol = symbol.makeSymbol("Symbol.toStringTag") as any;
	export const isConcatSpreadable: unique symbol = symbol.makeSymbol("Symbol.isConcatSpreadable") as any;
}

export interface TypeMap {
	undefined: undefined;
	boolean: boolean;
	string: string;
	number: number;
	symbol: symbol;
	object: null | object;
	function: Function;
}

export function unwrapThis<T extends keyof TypeMap>(self: any, type: T, constr: Function, name: string, arg = "this", defaultVal?: TypeMap[T]): TypeMap[T] {
	if (typeof self === type) return self;
	if (self instanceof constr && valueKey in self) self = (self as any)[valueKey];
	if (typeof self === type) return self;
	if (defaultVal !== undefined) return defaultVal;
	throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
}
export function wrapI(i: number, length: number) {
	if (i < 0) return (i + length) | 0;
	else return i | 0;
}
export function limitI(i: number, max: number) {
	i |= 0;
	if (i < 0) return 0;
	else if (i > max) return max;
	else return i;
}

export type ReplaceRange = { start: number; end: number; matches: string[]; groups?: Record<string, string>; };
type ReplaceLiteral = (string | ((_: { groups: string[]; prev: () => string; next: () => string; }) => string))[];

function parseReplacer(replacer: string, groupN: number) {
	const parts: ReplaceLiteral = [];
	let lastI = 0;
	let lastSlice = 0;

	while (true) {
		const i = string.indexOf(replacer, "$", lastI);
		if (i < 0 || i + 1 >= replacer.length) break;
		lastI = i + 1;

		switch (replacer[i + 1]) {
			case "$":
				parts[parts.length] = string.substring(replacer, lastSlice, i);
				parts[parts.length] = "$";
				lastSlice = i + 2;
				continue;
			case "&":
				parts[parts.length] = string.substring(replacer, lastSlice, i);
				parts[parts.length] = ({ groups }) => groups[0];
				lastSlice = i + 2;
				continue;
			case "`":
				parts[parts.length] = string.substring(replacer, lastSlice, i);
				parts[parts.length] = ({ prev }) => prev();
				lastSlice = i + 2;
				continue;
			case "'":
				parts[parts.length] = string.substring(replacer, lastSlice, i);
				parts[parts.length] = ({ next }) => next();
				lastSlice = i + 2;
				continue;
		}

		let groupI = 0;
		let hasGroup = false;
		let consumedN = 1;

		while (i + consumedN < replacer.length) {
			const code = string.toCharCode(replacer[i + consumedN]);
			if (code >= 48 && code <= 57) {
				const newGroupI = groupI * 10 + code - 48;
				if (newGroupI < 1 || newGroupI >= groupN) break;

				groupI = newGroupI;
				hasGroup = true;
			}
			consumedN++;
		}

		if (hasGroup) {
			parts[parts.length] = string.substring(replacer, lastSlice, i);
			parts[parts.length] = ({ groups }) => groups[groupI];
			lastSlice = i + consumedN;
			continue;
		}
		
	}

	if (lastSlice === 0) return [replacer];
	else parts[parts.length] = string.substring(replacer, lastSlice, replacer.length);

	return parts;
}
function executeReplacer(text: string, match: ReplaceRange, literal: ReplaceLiteral, prevEnd?: number, nextStart?: number) {
	const res = [];

	for (let i = 0; i < literal.length; i++) {
		const curr = literal[i];
		if (typeof curr === "function") res[i] = curr({
			groups: match.matches,
			next: () => string.substring(text, prevEnd ?? 0, match.start),
			prev: () => string.substring(text, match.end, nextStart ?? 0),
		});
		else res[i] = curr;
	}

	return string.stringBuild(res);
}
export function applyReplaces(text: string, ranges: ReplaceRange[], replace: any, groupN?: number | false) {
	if (ranges.length === 0) return text;

	const res: string[] = [];
	let offset = 0;

	if (groupN !== false && typeof replace === "string") {
		if (groupN == null) {
			for (let i = 0; i < ranges.length; i++) {
				const prevEnd = i - 1 >= 0 ? ranges[i - 1].end : undefined;
				const nextStart = i + 1 < ranges.length ? ranges[i + 1].start : undefined;
				const range = ranges[i];
				res[res.length] = string.substring(text, offset, range.start);
				res[res.length] = executeReplacer(text, range, parseReplacer(replace, range.matches.length), prevEnd, nextStart);
				offset = range.end;
			}

			res[res.length] = string.substring(text, offset, text.length);
		}
		else {
			const literal = parseReplacer(replace, groupN);

			for (let i = 0; i < ranges.length; i++) {
				const prevEnd = i - 1 >= 0 ? ranges[i - 1].end : undefined;
				const nextStart = i + 1 < ranges.length ? ranges[i + 1].start : undefined;
				const range = ranges[i];
				res[res.length] = string.substring(text, offset, range.start);
				res[res.length] = executeReplacer(text, range, literal, prevEnd, nextStart);
				offset = range.end;
			}

			res[res.length] = string.substring(text, offset, text.length);
		}
		return string.stringBuild(res);
	}

	if (typeof replace === "string") {
		for (let i = 0; i < ranges.length; i++) {
			const range = ranges[i];
			res[res.length] = string.substring(text, offset, range.start);
			res[res.length] = replace;
			offset = range.end;
		}

		res[res.length] = string.substring(text, offset, text.length);
	}
	else {
		for (let i = 0; i < ranges.length; i++) {
			const range = ranges[i];
			const args: any[] = range.matches;
			args[args.length] = range.start;
			args[args.length] = text;
			args[args.length] = range.groups;

			res[res.length] = string.substring(text, offset, range.start);
			res[res.length] = func.invoke(replace, undefined, args);
			offset = range.end;
		}

		res[res.length] = string.substring(text, offset, text.length);
	}

	return string.stringBuild(res);
}

export function applySplits(text: string, limit: number | undefined, next: (offset: number) => { start: number; end: number; } | undefined) {
	let lastEnd = 0;
	let lastEmpty = true;
	let offset = 0;

	const res: string[] = [];

	while (true) {
		if (limit != null && limit >= 0 && res.length >= limit) break;

		const curr = next(offset);

		if (curr == null) {
			if (!lastEmpty || !res.length) res[res.length] = string.substring(text, lastEnd, text.length);
			break;
		}

		const { start, end } = curr;
		const empty = start === end;

		if (offset > 0 || !empty) res[res.length] = string.substring(text, lastEnd, start);

		lastEnd = end;
		offset = empty ? end + 1 : end;
		lastEmpty = empty;
	}

	return res;
}
