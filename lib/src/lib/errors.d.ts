interface Error {
	message: string;
	name: string;
}
interface ErrorConstructor {
	(msg?: string): Error;
	new (msg?: string): Error;
}

interface TypeError extends Error { }
interface TypeErrorConstructor {
	(msg?: string): TypeError;
	new (msg?: string): TypeError;
}


interface SyntaxError extends Error { }
interface SyntaxErrorConstructor {
	(msg?: string): SyntaxError;
	new (msg?: string): SyntaxError;
}

interface RangeError extends Error { }
interface RangeErrorConstructor {
	(msg?: string): RangeError;
	new (msg?: string): RangeError;
}