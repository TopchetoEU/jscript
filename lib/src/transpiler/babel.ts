import { SourceMap } from "./map.ts";
import { transform, availablePlugins } from "@babel/standalone";

export default function babel(next: Compiler): Compiler {
	print("Loaded babel!");

	return (filename, code, prevMap) => {
		const res = transform(code, {
			filename,
			sourceMaps: true,
			assumptions: {
				arrayLikeIsIterable: true,
				constantSuper: true,
				ignoreFunctionLength: true,
				ignoreToPrimitiveHint: true,
				mutableTemplateObject: true,
				noDocumentAll: true,
				noNewArrows: true,
				noUninitializedPrivateFieldAccess: true,
				privateFieldsAsSymbols: true,
			},
			plugins: [
				// ES2022
				availablePlugins["transform-class-properties"],
				availablePlugins["transform-class-static-block"],
				availablePlugins["transform-private-methods"],
				availablePlugins["transform-private-property-in-object"],
				// "syntax-top-level-await",
			
				// ES2021
				availablePlugins["transform-logical-assignment-operators"],
				availablePlugins["transform-numeric-separator"],
			
				// ES2020
				availablePlugins["transform-optional-chaining"],
				availablePlugins["transform-nullish-coalescing-operator"],
			
				// ES2018
				availablePlugins["transform-async-generator-functions"],
				availablePlugins["transform-object-rest-spread"],
				availablePlugins["transform-unicode-property-regex"],
			
				// ES2017
				availablePlugins["transform-async-to-generator"],
			
				// ES2016
				availablePlugins["transform-exponentiation-operator"],
			
				// ES2015
				availablePlugins["transform-block-scoping"],
				availablePlugins["transform-classes"],
				availablePlugins["transform-computed-properties"],
				availablePlugins["transform-destructuring"],
				availablePlugins["transform-duplicate-keys"],
				availablePlugins["transform-for-of"],
				availablePlugins["transform-function-name"],
				availablePlugins["transform-literals"],
				availablePlugins["transform-new-target"],
				availablePlugins["transform-object-super"],
				availablePlugins["transform-parameters"],
				availablePlugins["transform-shorthand-properties"],
				availablePlugins["transform-spread"],
				availablePlugins["transform-sticky-regex"],
				availablePlugins["transform-template-literals"],
				availablePlugins["transform-unicode-escapes"],
				availablePlugins["transform-unicode-regex"],
			],
		});

		const map = SourceMap.parse({
			file: "babel-internal://" + filename,
			mappings: res.map!.mappings,
			sources: [filename],
		});

		registerSource(filename, code);
		const func = next("babel-internal://" + filename, res.code!, SourceMap.chain(map, prevMap));
		func.name = filename;
		return func;
	};
}

