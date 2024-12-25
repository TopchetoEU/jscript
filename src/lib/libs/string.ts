import { func, number, regex, string } from "./primordials.ts";
import { RegExp } from "./regex.ts";
import { applyReplaces, applySplits, limitI, ReplaceRange, symbols, unwrapThis, valueKey, wrapI } from "./utils.ts";

const trimStartRegex = new regex("^\\s+", false, false, false, false, false);
const trimEndRegex = new regex("\\s+$", false, false, false, false, false);

export const String = (() => {
	class String {
		[valueKey]!: string;

		public at(index: number) {
			throw "Not implemented :/";
			return unwrapThis(this, "string", String, "String.prototype.at")[index];
		};
		public toString() {
			return unwrapThis(this, "string", String, "String.prototype.toString");
		}
		public valueOf() {
			return unwrapThis(this, "string", String, "String.prototype.valueOf");
		}

		public includes(search: string, offset = 0) {
			const self = unwrapThis(this, "string", String, "String.prototype.indexOf");
			return string.indexOf(self, (String as any)(search), +offset, false) >= 0;
		}

		public indexOf(search: string, offset = 0) {
			const self = unwrapThis(this, "string", String, "String.prototype.indexOf");
			offset = +offset;
			return string.indexOf(self, search, offset, false);
		}
		public lastIndexOf(search: string, offset = 0) {
			const self = unwrapThis(this, "string", String, "String.prototype.lastIndexOf");
			offset = +offset;
			return string.indexOf(self, search, offset, true);
		}

		public trim() {
			const self = unwrapThis(this, "string", String, "String.prototype.trim");
			const start = trimStartRegex.exec(self, 0, false);
			const end = trimEndRegex.exec(self, 0, false);

			const startI = start == null ? 0 : start.end;
			const endI = end == null ? self.length : end.matches.index!;

			return string.substring(self, startI, endI);
		}
		public trimStart() {
			const self = unwrapThis(this, "string", String, "String.prototype.trim");
			const start = trimStartRegex.exec(self, 0, false);
			const startI = start == null ? 0 : start.end;

			return string.substring(self, startI, self.length);
		}
		public trimEnd() {
			const self = unwrapThis(this, "string", String, "String.prototype.trim");
			const end = trimEndRegex.exec(self, 0, false);
			const endI = end == null ? self.length : end.matches.index!;

			return string.substring(self, 0, endI);
		}

		public charAt(i: number) {
			const self = unwrapThis(this, "string", String, "String.prototype.charAt");
			return self[i];
		}
		public charCodeAt(i: number) {
			const self = unwrapThis(this, "string", String, "String.prototype.charCodeAt");
			return self[i] ? string.toCharCode(self[i]) : number.NaN;
		}
		public codePointAt(i: number) {
			const self = unwrapThis(this, "string", String, "String.prototype.charCodeAt");
			return i >= 0 && i < self.length ? string.toCodePoint(self, i) : number.NaN;
		}

		public split(val?: any, limit?: number) {
			const self = unwrapThis(this, "string", String, "String.prototype.split");
			if (val === undefined) return [self];
			if (val !== null && typeof val === "object" && symbols.split in val) {
				return val[symbols.split](self, limit);
			}

			val = (String as any)(val);

			return applySplits(self, limit, offset => {
				const start = string.indexOf(self, val, offset, false);
				if (start < 0) return undefined;
				else return { start, end: start + val.length };
			});
		}
		public replace(val: any, replacer: any) {
			const self = unwrapThis(this, "string", String, "String.prototype.replace");
			if (val !== null && typeof val === "object" && symbols.replace in val) {
				return val[symbols.replace](self, replacer);
			}
			else val = (String as any)(val);

			const i = string.indexOf(self, val, 0);
			return applyReplaces(self, [{ start: i, end: i + val.length, matches: [val] }], replacer, false);
		}
		public replaceAll(val: any, replacer: any) {
			const self = unwrapThis(this, "string", String, "String.prototype.replaceAll");
			if (val !== null && typeof val === "object" && symbols.replace in val) {
				if (val instanceof RegExp && !val.global) throw new TypeError("replaceAll must be called with a global RegExp");
				return val[symbols.replace](self, replacer);
			}
			else val = (String as any)(val);

			let offset = 0;
			const matches: ReplaceRange[] = [];
			const add = val.length === 0 ? 1 : val.length;

			while (true) {
				const i = string.indexOf(self, val, offset);
				if (i < 0) break;

				matches[matches.length] = { start: i, end: i + val.length, matches: [val] };
				if (val.length === 0)
				offset = i + add;
			}

			return applyReplaces(self, matches, replacer, false);
		}

		public slice(this: string, start = 0, end = this.length) {
			const self = unwrapThis(this, "string", String, "String.prototype.slice");
			start = limitI(wrapI(start, this.length), this.length);
			end = limitI(wrapI(end, this.length), this.length);

			if (end <= start) return "";
			return string.substring(self, start, end);
		}
		public substring(this: string, start = 0, end = this.length) {
			const self = unwrapThis(this, "string", String, "String.prototype.substring");
			start = limitI(start, this.length);
			end = limitI(end, this.length);

			if (end <= start) return "";
			return string.substring(self, start, end);
		}
		public substr(this: string, start = 0, count = this.length - start) {
			const self = unwrapThis(this, "string", String, "String.prototype.substr");
			start = limitI(start, this.length);
			count = limitI(count, this.length - start);

			if (count <= 0) return "";
			return string.substring(self, start, count + start);
		}

		public toLowerCase(this: string) {
			const self = unwrapThis(this, "string", String, "String.prototype.toLowerCase");
			return string.lower(self);
		}
		public toUpperCase(this: string) {
			const self = unwrapThis(this, "string", String, "String.prototype.toLowerCase");
			return string.upper(self);
		}

		public [symbols.iterator]() {
			var i = 0;
			var arr: string | undefined = unwrapThis(this, "string", String, "String.prototype[Symbol.iterator]");

			return {
				next () {
					if (arr == null) return { done: true, value: undefined };
					if (i > arr.length) {
						arr = undefined;
						return { done: true, value: undefined };
					}
					else {
						var val = arr[i++];
						if (i >= arr.length) arr = undefined;
						return { done: false, value: val };
					}
				},
				[symbols.iterator]() { return this; }
			};
		}

		public constructor (value?: unknown) {
			if (func.invokeType(arguments, this) === "call") {
				if (arguments.length === 0) return "" as any;
				else if (typeof value === "symbol") return value.toString() as any;
				else return (value as any) + "" as any;
			}
			this[valueKey] = (String as any)(value);
		}

		public static fromCharCode() {
			const res: string[] = [];
			res[arguments.length] = "";

			for (let i = 0; i < arguments.length; i++) {
				res[i] = string.fromCharCode(+arguments[i]);
			}

			return string.stringBuild(res);
		}
		public static fromCodePoint() {
			const res: string[] = [];
			res[arguments.length] = "";

			for (var i = 0; i < arguments.length; i++) {
				res[i] = string.fromCodePoint(+arguments[i]);
			}
			return string.stringBuild(res);
		}
	}

	func.setCallable(String, true);
	func.setConstructable(String, true);

	return String as any as typeof String & ((value?: unknown) => string);
})();
export type String = InstanceType<typeof String>;
