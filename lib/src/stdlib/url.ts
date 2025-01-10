import { regex, string } from "./primordials";

function escaper(matcher: regex) {
	return (text: string) => {
		const parts: string[] = [];
		let i = 0;

		while (true) {
			const match = matcher.exec(text, i, false);
			if (match == null) break;

			const char = match.matches[0];
			const code = string.toCharCode(char);
			parts[parts.length] = string.substring(text, i, match.matches.index!);
			parts[parts.length] = "%" + code;
			i = match.end;
		}

		parts[parts.length] = string.substring(text, i, text.length);

		return string.stringBuild(parts);
	};
}

export const encodeURI = escaper(new regex("[^A-Za-z0-9\\-+.!~*'()]"));
export const encodeURIComponent = escaper(new regex("[^A-Za-z0-9\\-+.!~*'();/?:@&=+$,#]"));