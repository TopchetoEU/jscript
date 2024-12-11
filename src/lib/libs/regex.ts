import { func, regex } from "./primordials.ts";
import { String } from "./string.ts";
import { Symbol } from "./symbol.ts";

const regexKey: unique symbol = Symbol("RegExp.impl") as any;

export class RegExp {
	private [regexKey]: InstanceType<typeof regex>;

	public readonly source: string;
	public readonly flags: string;
	public lastIndex = 0;

	public constructor(source: any, flags: string) {
		source = this.source = String(typeof source === "object" && "source" in source ? source.source : source);
		flags = String(flags);

		const _regex = this[regexKey] = new regex(source);
	
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
	}
}
func.setCallable(RegExp, false);
