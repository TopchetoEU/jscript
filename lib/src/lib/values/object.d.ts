type AssignResult<Arr extends readonly object[]> =
	Arr extends [...infer Rest extends object[], infer B extends object] ? {
		[x in keyof B | keyof AssignResult<Rest>]:
			x extends keyof B ? B[x] :
			x extends keyof AssignResult<Rest> ? AssignResult<Rest>[x] :
			never
	} : {};

declare interface PropertyDescriptor {
	configurable?: boolean;
	enumerable?: boolean;
	value?: any;
	writable?: boolean;
	get?(): any;
	set?(v: any): void;
}
declare interface PropertyDescriptorMap {
	[key: PropertyKey]: PropertyDescriptor;
}

declare interface Object {
	valueOf(): number;
	toString(): string;
}
declare interface ObjectConstructor {
	new (val: string): String;
	(val: string): String;
	new (val: number): Number;
	new (val: number): Number;
	(val: number): Number;
	new (val: boolean): Boolean;
	(val: boolean): Boolean;
	new (val: symbol): Symbol;
	(val: symbol): Symbol;
	new <T extends object>(val: T): T;
	<T extends object>(val: T): T;
	new (): object;
	(): object;

	getOwnPropertyDescriptor(obj: object, key: any): PropertyDescriptor;
	defineProperty<T extends object>(obj: T, key: string | symbol, desc: PropertyDescriptor): T;
	defineProperties<T extends object>(obj: T, desc: PropertyDescriptorMap): T;

	create<T extends object>(proto: T, desc?: PropertyDescriptorMap): T;
	assign<First extends object, T extends readonly object[]>(targeT: First, ...arr: T): AssignResult<[First, ...T]>;

	setPrototypeOf<T extends object>(obj: T, proto: object | null): T
	getPrototypeOf(obj: object): object | null;

	keys<T extends object>(obj: T): (keyof T)[];
	values<T extends object>(obj: T): T[keyof T][];
	entries<T extends object>(obj: T): [key: keyof T, val: T[keyof T]][];

	preventExtensions<T>(obj: T): T;
	seal<T>(obj: T): T;
	freeze<T>(obj: T): T;
}
