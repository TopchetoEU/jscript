import { type SourceMap } from "./map.ts";

declare global {
	type CompilerFactory = (next: Compiler) => Compiler;
	type Compiler = (filename: string, src: string, mapper: SourceMap) => Function;

	function print(...args: any[]): void;
	function register(factory: CompilerFactory): void;
	function registerSource(filename: string, src: string): void;
}
