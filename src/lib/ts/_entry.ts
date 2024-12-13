import { createDocumentRegistry, createLanguageService, ModuleKind, ScriptSnapshot, ScriptTarget, type Diagnostic, type CompilerOptions, type IScriptSnapshot, flattenDiagnosticMessageText, CompilerHost, LanguageService } from "typescript";

// declare function getResource(name: string): string | undefined;
declare function print(...args: any[]): void;
// declare function register(factory: CompilerFactory): void;

type CompilerFactory = (next: Compiler) => Compiler;
type Compiler = (filename: string, src: string, maps: any[]) => Function;

const resources: Record<string, string | undefined> = {};

function getResource(name: string): string | undefined {
	if (name === "/lib.d.ts") return "declare var a = 10;";
	return undefined;
}

function resource(name: string) {
	if (name in resources) return resources[name];
	else return resources[name] = getResource(name);
}

function register(factory: CompilerFactory): void {
	factory((filename, src) => Function(src));
}

register(next => {
	const files: Record<string, IScriptSnapshot> = {};
	const versions: Record<string, number> = {};
	let declI = 0;

	const settings: CompilerOptions = {
		target: ScriptTarget.ESNext,
		module: ModuleKind.Preserve,
	
		allowImportingTsExtensions: true,
		verbatimModuleSyntax: true,
		
		strict: false,
		skipLibCheck: true,
		forceConsistentCasingInFileNames: true,
		declaration: true,
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
	
	return (code, filename, mapChain) => {
		files["/src.ts"] = ScriptSnapshot.fromString(code);
		versions["/src.ts"] ??= 0;
		versions["/src.ts"]++;

		const emit = service.getEmitOutput("/src.ts");

		const diagnostics = new Array<Diagnostic>()
			.concat(service.getCompilerOptionsDiagnostics())
			.concat(service.getSyntacticDiagnostics("/src.ts"))
			.concat(service.getSemanticDiagnostics("/src.ts"))
			.map(function (diagnostic) {
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

		var map = JSON.parse(emit.outputFiles[0].text);
		var result = emit.outputFiles[1].text;
		var declaration = emit.outputFiles[2].text;

		var compiled = next(result, filename, mapChain.concat(map));

		return function (this: any) {
			const res = compiled.apply(this, arguments);
			if (declaration !== '') files["/src." + declI++ + ".d.ts"] = ScriptSnapshot.fromString(declaration);
			return res;
		};
	};
});
