type PromiseFulfillFunc<T> = (val: T) => void;
type PromiseThenFunc<T, NextT> = (val: T) => NextT;
type PromiseRejectFunc = (err: unknown) => void;
type PromiseFunc<T> = (resolve: PromiseFulfillFunc<T>, reject: PromiseRejectFunc) => void;

type PromiseResult<T> ={ type: 'fulfilled'; value: T; } | { type: 'rejected'; reason: any; }

interface Thenable<T> {
    then<NextT>(this: Promise<T>, onFulfilled: PromiseThenFunc<T, NextT>, onRejected?: PromiseRejectFunc): Promise<Awaited<NextT>>;
    then(this: Promise<T>, onFulfilled: undefined, onRejected?: PromiseRejectFunc): Promise<T>;
}

// wippidy-wine, this code is now mine :D
type Awaited<T> =
    T extends null | undefined ? T : // special case for `null | undefined` when not in `--strictNullChecks` mode
        T extends object & { then(onfulfilled: infer F, ...args: infer _): any } ? // `await` only unwraps object types with a callable `then`. Non-object types are not unwrapped
            F extends ((value: infer V, ...args: infer _) => any) ? // if the argument to `then` is callable, extracts the first argument
                Awaited<V> : // recursively unwrap the value
                never : // the argument to `then` was not callable
        T;

interface PromiseConstructor {
    prototype: Promise<any>;

    new <T>(func: PromiseFunc<T>): Promise<Awaited<T>>;
    resolve<T>(val: T): Promise<Awaited<T>>;
    reject(val: any): Promise<never>;

    any<T>(promises: (Promise<T>|T)[]): Promise<T>;
    race<T>(promises: (Promise<T>|T)[]): Promise<T>;
    all<T extends any[]>(promises: T): Promise<{ [Key in keyof T]: Awaited<T[Key]> }>;
    allSettled<T extends any[]>(...promises: T): Promise<[...{ [P in keyof T]: PromiseResult<Awaited<T[P]>>}]>;
}

interface Promise<T> extends Thenable<T> {
    constructor: PromiseConstructor;
    catch(func: PromiseRejectFunc): Promise<T>;
    finally(func: () => void): Promise<T>;
}

declare var Promise: PromiseConstructor;

(Promise.prototype as any)[Symbol.typeName] = 'Promise';
