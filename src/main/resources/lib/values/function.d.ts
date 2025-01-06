declare interface Function {
	prototype: object;

	name: string;
	length: number;

	valueOf(): this;
	toString(): string;

	apply<Args extends readonly any[], Ret, Self>(this: (this: Self, ...args: Args) => Ret, self: Self, args: Args): Ret;
	apply(this: Function, self: any, args: any[]): any;
	call<Args extends readonly any[], Ret, Self>(this: (this: Self, ...args: Args) => Ret, self: Self, ...args: Args): Ret;
	call(this: Function, self: any, ...args: any): any;
	bind<T extends (...args: any[]) => any>(this: T): T;
	bind<
		Bound extends readonly any[],
		Args extends readonly any[],
		Ret, Self
	>(this: (this: Self, ...args: [...Bound, ...Args]) => Ret, self: Self, ...bound: Bound): (this: void, ...args: Args) => Ret;
}
declare interface CallableFunction extends Function {
	(...args: unknown[]): unknown;
}
declare interface NewableFunction extends Function {
	new (...args: unknown[]): unknown;
}
declare interface FunctionConstructor {
	new (val?: string): (this: unknown, ...args: unknown[]) => unknown;
	(val?: string): (this: unknown, ...args: unknown[]) => unknown;
}
