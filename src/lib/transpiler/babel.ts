import { SourceMap } from "./map.ts";
import { transform, availablePresets } from "@babel/standalone";

export default function babel(next: Compiler): Compiler {
	print("Loaded babel!");

	return (filename, code, prevMap) => {
		const res = transform(code, {
			filename,
			sourceMaps: true,
			presets: [availablePresets.env],
		});

		print(res.map!.mappings);

		const map = SourceMap.parse({
			file: "babel-internal://" + filename,
			mappings: res.map!.mappings,
			sources: [filename],
		});

		const compiled = next("babel-internal://" + filename, res.code!, SourceMap.chain(map, prevMap));
		registerSource(filename, code);
		return compiled;
	};
}

