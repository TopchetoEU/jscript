declare interface Record<Key, Val> {
	[key: Key]: Val;
}
declare type InstanceType<T> = T extends new (...args: any[]) => infer T ? T : never;
declare type ReturnType<T> = T extends (...args: any[]) => infer T ? T : never;
declare type Arguments<T> =
	T extends (...args: infer T) => any ? T :
	T extends new (...args: infer T) => any ? T :
	never;
