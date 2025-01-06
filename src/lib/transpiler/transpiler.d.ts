type Location = readonly [file: string, line: number, start: number];
type SourceMap = (loc: Location) => Location | undefined;

type CompilerFactory = (next: Compiler) => Compiler;
type Compiler = (filename: string, src: string, mapper: SourceMap) => (this: any, ...args: any[]) => any;

declare function getResource(name: string): string | undefined;
declare function print(...args: any[]): void;
declare function register(factory: CompilerFactory): void;
declare function registerSource(filename: string, src: string): void;
