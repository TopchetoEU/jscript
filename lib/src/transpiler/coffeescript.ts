import { compile } from "coffeescript";
import { SourceMap } from "./map.ts";

export default function coffee(next: Compiler): Compiler {
	print("Loaded coffeescript!");
	
	return (filename, code, prevMap) => {
		const {
			js: result,
			v3SourceMap: rawMap,
		} = compile(code, {
			filename,
			sourceMap: true,
			bare: true,
		});

		const map = SourceMap.parse({
			file: "coffee-internal://" + filename,
			mappings: JSON.parse(rawMap).mappings,
			sources: [filename],
		});

		registerSource(filename, code);
		return next("coffee-internal://" + filename, result, SourceMap.chain(map, prevMap));
	};
}

