import { func, regex, symbol } from "./primordials.ts";
import { String } from "./string.ts";
import { ReplaceRange } from "./utils.ts";
import { applyReplaces } from "./utils.ts";
import { applySplits } from "./utils.ts";
import { symbols } from "./utils.ts";

const regexKey: unique symbol = symbol.makeSymbol("RegExp.impl") as any;

export class RegExp {
	private [regexKey]: InstanceType<typeof regex>;

	public readonly source: string;
	public readonly flags: string;
	public lastIndex = 0;

	public readonly indices: boolean;
	public readonly global: boolean;
	public readonly ignoreCase: boolean;
	public readonly multiline: boolean;
	public readonly dotall: boolean;
	public readonly unicode: boolean;
	public readonly unicodeSets: boolean;
	public readonly sticky: boolean;

	public constructor(source: any, flags: string) {
		source = this.source = String(typeof source === "object" && "source" in source ? source.source : source);
		flags = String(flags);
	
		let indices = false;
		let global = false;
		let ignoreCase = false;
		let multiline = false;
		let dotall = false;
		let unicode = false;
		let unicodeSets = false;
		let sticky = false;
	
		for (let i = 0; i < flags.length; i++) {
			switch (flags[i]) {
				case "d": indices = true; break;
				case "g": global = true; break;
				case "i": ignoreCase = true; break;
				case "m": multiline = true; break;
				case "s": dotall = true; break;
				case "u": unicode = true; break;
				case "v": unicodeSets = true; break;
				case "y": sticky = true; break;
			}
		}
	
		flags = "";
		if (indices) flags += "d";
		if (global) flags += "g";
		if (ignoreCase) flags += "i";
		if (multiline) flags += "m";
		if (dotall) flags += "s";
		if (unicode) flags += "u";
		if (unicodeSets) flags += "v";
		if (sticky) flags += "y";
		this.flags = flags;
		this.indices = indices;
		this.global = global;
		this.ignoreCase = ignoreCase;
		this.multiline = multiline;
		this.dotall = dotall;
		this.unicode = unicode;
		this.unicodeSets = unicodeSets;
		this.sticky = sticky;

		this[regexKey] = new regex(source, multiline, ignoreCase, dotall, unicode, unicodeSets);
	}

	public exec(target: string) {
		const useLast = this.global || this.sticky;
		const start = useLast ? this.lastIndex : 0;

		const match = this[regexKey].exec(target, start, this.indices);
		if (match != null && !(this.sticky && match.matches.index !== start)) {
			if (useLast) this.lastIndex = match.end;
			return match.matches;
		}

		if (useLast) this.lastIndex = 0;
		return null;
	}
	public test(target: string) {
		return this.exec(target) != null;
	}

	public [symbols.split](target: string, limit?: number) {
		return applySplits(target, limit, offset => {
			const val = this[regexKey].exec(target, offset, false);
			if (val == null) return undefined;

			return { start: val.matches.index!, end: val.end };
		});
	}
	public [symbols.replace](target: string, replacer: any) {
		const matches: ReplaceRange[] = [];
		const regex = this[regexKey];

		if (this.global) {
			let offset = 0;

			while (true) {
				const match = regex.exec(target, offset, false);
				if (match == null) break;

				const start = match.matches.index;
				const end = match.end;
				const arr: string[] = [];
				for (let i = 0; i < match.matches.length; i++) {
					arr[i] = match.matches[i];
				}

				matches[matches.length] = { start: match.matches.index!, end: match.end, matches: arr };

				if (start === end) offset = start + 1;
				else offset = end;
			}

			return applyReplaces(target, matches, replacer, regex.groupCount() + 1);
		}
		else {
			const match = this.exec(target);
			if (match != null) matches[0] = {
				start: match.index!,
				end: match.index! + match[0].length,
				matches: match,
			}
		}

		return applyReplaces(target, matches, replacer, regex.groupCount() + 1);
	}
}
func.setCallable(RegExp, false);
