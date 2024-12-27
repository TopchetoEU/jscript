import { createDocumentRegistry, createLanguageService, ModuleKind, ScriptSnapshot, ScriptTarget, type Diagnostic, type CompilerOptions, type IScriptSnapshot, flattenDiagnosticMessageText, CompilerHost, LanguageService } from "typescript";
import { SourceMap } from "./map.ts";

declare function getResource(name: string): string | undefined;
declare function print(...args: any[]): void;
declare function register(factory: CompilerFactory): void;
declare function registerSource(filename: string, src: string): void;

type CompilerFactory = (next: Compiler) => Compiler;
type Compiler = (filename: string, src: string, mapper: SourceMap) => Function;

const resources: Record<string, string | undefined> = {};


function resource(name: string) {
	if (name in resources) return resources[name];
	else return resources[name] = getResource(name);
}

register(next => {
	const files: Record<string, IScriptSnapshot> = {};
	const versions: Record<string, number> = {};
	let declI = 0;

	const settings: CompilerOptions = {
		target: ScriptTarget.ES5,
		module: ModuleKind.Preserve,
	
		allowImportingTsExtensions: true,
		verbatimModuleSyntax: true,
		
		strict: false,
		skipLibCheck: true,
		forceConsistentCasingInFileNames: true,
		declaration: true,
		sourceMap: true,
	};

	let service: LanguageService;

	measure(() => {
		service = createLanguageService({
			getCurrentDirectory: () => "/",
			getDefaultLibFileName: () => "/lib.d.ts",
			getScriptFileNames: () => {
				const res = ["/src.ts", "/lib.d.ts"];
				for (let i = 0; i < declI; i++) res.push("/src." + i + ".d.ts");
				return res;
			},
			getCompilationSettings: () => settings,
			log: print,
			fileExists: filename => filename in files || resource(filename) != null,

			getScriptSnapshot: (filename) => {
				if (filename in files) return files[filename];
				else {
					const src = resource(filename);
					if (src == null) return undefined;
					return files[filename] = ScriptSnapshot.fromString(src);
				}
			},
			getScriptVersion: (filename) => String(versions[filename] || 0),
		
			readFile: () => { throw "no"; },
			writeFile: () => { throw "no"; },
		}, createDocumentRegistry());
	});
	measure(() => {
		service.getEmitOutput("/lib.d.ts");
	});
	print("Loaded typescript!");
	
	return (filename, code, prevMap) => {
		files["/src.ts"] = ScriptSnapshot.fromString(code);
		versions["/src.ts"] ??= 0;
		versions["/src.ts"]++;

		const emit = service.getEmitOutput("/src.ts");

		const diagnostics = new Array<Diagnostic>()
			.concat(service.getSyntacticDiagnostics("/src.ts"))
			.concat(service.getSemanticDiagnostics("/src.ts"))
			.map(diagnostic => {
				const message = flattenDiagnosticMessageText(diagnostic.messageText, "\n");

				if (diagnostic.file != null) {
					let file = diagnostic.file.fileName.substring(1);
					if (file === "src.ts") file = filename;

					if (diagnostic.start == null) return file;

					const pos = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
					return file + ":" + (pos.line + 1) + ":" + (pos.character + 1) + ": " + message;
				}
				else return message;
			});

		if (diagnostics.length > 0) {
			throw new SyntaxError(diagnostics.join("\n"));
		}

		const outputs: Record<string, string> = {};

		for (const el of emit.outputFiles) {
			outputs[el.name] = el.text;
		}

		const rawMap = JSON.parse(outputs["/src.js.map"]);
		const map = SourceMap.parse({
			file: "ts-internal://" + filename,
			mappings: rawMap.mappings,
			sources: [filename],
		});
		const result = outputs["/src.js"];
		const declaration = outputs["/src.d.ts"];

		const compiled = next("ts-internal://" + filename, result, SourceMap.chain(prevMap, map));
		registerSource(filename, code);

		return function (this: any) {
			const res = compiled.apply(this, arguments);
			if (declaration !== '') files["/src." + declI++ + ".d.ts"] = ScriptSnapshot.fromString(declaration);
			return res;
		};
	};
});
