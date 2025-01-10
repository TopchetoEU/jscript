declare type Timer = number;

declare function setTimeout(func: () => void, time: number): Timer;
declare function setTimer(func: () => void, time: number): Timer;
declare function setImmediate(func: () => void): void;

declare function clearTimeout(timer: Timer): void;
declare function clearTimer(timer: Timer): void;

declare type Awaited<T> = T extends { then(fn?: (val: infer Res) => void) } ? Awaited<Res> : T;

declare interface Thenable<T> {
	then<Res = T, Rej = never>(onFulfill?: (val: T) => Res, onReject?: (err: any) => Rej): Promise<Res | Rej>;
}
declare interface Promise<T> extends Thenable<T> {
	catch<Res = T>(onReject?: (err: any) => Res): Promise<Res>;
	finally(fn?: () => void): Promise<T>;
}

declare interface PromiseConstructor {
	new <T>(fn: (res: (val: T) => void, rej: (err: any) => void) => void): Promise<T>;
	resolve<T>(val: T): Promise<Awaited<T>>;
	reject<T>(err: unknown): Promise<T>;
}
