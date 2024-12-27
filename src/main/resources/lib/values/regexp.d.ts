declare interface RegExpMatchArray extends Array<string> {
	0: string;

	index?: number;
	input?: string;

	groups?: { [key: string]: string };
	indices?: [start: number, end: number][] & {
		[key: string]: [start: number, end: number]
	}
}

declare interface RegExp {
	lastIndex: number;

	readonly source: string;
	readonly flags: string;

	readonly indices: boolean;
	readonly global: boolean;
	readonly ignoreCase: boolean;
	readonly multiline: boolean;
	readonly dotall: boolean;
	readonly unicode: boolean;
	readonly unicodeSets: boolean;
	readonly sticky: boolean;

	exec(target: string): RegExpMatchArray | null;
	text(target: string): boolean
	[Symbol.split](target: string, limit?: number): string[];
	[Symbol.replace](target: string, replacer: any): string;
}
declare interface RegExpConstructor {
	new (val: string, flags?: string): String;
	(val: string, flags?: string): String;
}
