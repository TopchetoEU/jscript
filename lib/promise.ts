define("promise", () => {
    var Promise = env.global.Promise = internals.promise
    return;

    // const syms = {
    //     callbacks: internals.symbol('Promise.callbacks'),
    //     state: internals.symbol('Promise.state'),
    //     value: internals.symbol('Promise.value'),
    //     handled: internals.symbol('Promise.handled'),
    // } as {
    //     readonly callbacks: unique symbol,
    //     readonly state: unique symbol,
    //     readonly value: unique symbol,
    //     readonly handled: unique symbol,
    // }

    // type Callback<T> = [ PromiseFulfillFunc<T>, PromiseRejectFunc ];
    // enum State {
    //     Pending,
    //     Fulfilled,
    //     Rejected,
    // }

    // function isAwaitable(val: unknown): val is Thenable<any> {
    //     return (
    //         typeof val === 'object' &&
    //         val !== null &&
    //         'then' in val &&
    //         typeof val.then === 'function'
    //     );
    // }
    // function resolve(promise: Promise<any>, v: any, state: State) {
    //     if (promise[syms.state] === State.Pending) {
    //         if (typeof v === 'object' && v !== null && 'then' in v && typeof v.then === 'function') {
    //             v.then(
    //                 (res: any) => resolve(promise, res, state),
    //                 (res: any) => resolve(promise, res, State.Rejected)
    //             );
    //             return;
    //         }
    //         promise[syms.value] = v;
    //         promise[syms.state] = state;

    //         for (let i = 0; i < promise[syms.callbacks]!.length; i++) {
    //             promise[syms.handled] = true;
    //             promise[syms.callbacks]![i][state - 1](v);
    //         }

    //         promise[syms.callbacks] = undefined;

    //         internals.pushMessage(true, internals.setEnv(() => {
    //             if (!promise[syms.handled] && state === State.Rejected) {
    //                 log('Uncaught (in promise) ' + promise[syms.value]);
    //             }
    //         }, env), undefined, []);
    //     }
    // }

    // class _Promise<T> {
    //     public static isAwaitable(val: unknown): val is Thenable<any> {
    //         return isAwaitable(val);
    //     }

    //     public static resolve<T>(val: T): Promise<Awaited<T>> {
    //         return new Promise(res => res(val as any));
    //     }
    //     public static reject<T>(val: T): Promise<Awaited<T>> {
    //         return new Promise((_, rej) => rej(val as any));
    //     }

    //     public static race<T>(vals: T[]): Promise<Awaited<T>> {
    //         if (typeof vals.length !== 'number') throw new env.global.TypeError('vals must be an array. Note that Promise.race is not variadic.');
    //         return new Promise((res, rej) => {
    //             for (let i = 0; i < vals.length; i++) {
    //                 const val = vals[i];
    //                 if (this.isAwaitable(val)) val.then(res, rej);
    //                 else res(val as any);
    //             }
    //         });
    //     }
    //     public static any<T>(vals: T[]): Promise<Awaited<T>> {
    //         if (typeof vals.length !== 'number') throw new env.global.TypeError('vals must be an array. Note that Promise.any is not variadic.');
    //         return new Promise((res, rej) => {
    //             let n = 0;

    //             for (let i = 0; i < vals.length; i++) {
    //                 const val = vals[i];
    //                 if (this.isAwaitable(val)) val.then(res, (err) => {
    //                     n++;
    //                     if (n === vals.length) throw Error('No promise resolved.');
    //                 });
    //                 else res(val as any);
    //             }

    //             if (vals.length === 0) throw Error('No promise resolved.');
    //         });
    //     }
    //     public static all(vals: any[]): Promise<any[]> {
    //         if (typeof vals.length !== 'number') throw new env.global.TypeError('vals must be an array. Note that Promise.all is not variadic.');
    //         return new Promise((res, rej) => {
    //             const result: any[] = [];
    //             let n = 0;

    //             for (let i = 0; i < vals.length; i++) {
    //                 const val = vals[i];
    //                 if (this.isAwaitable(val)) val.then(
    //                     val => {
    //                         n++;
    //                         result[i] = val;
    //                         if (n === vals.length) res(result);
    //                     },
    //                     rej
    //                 );
    //                 else {
    //                     n++;
    //                     result[i] = val;
    //                 }
    //             }

    //             if (vals.length === n) res(result);
    //         });
    //     }
    //     public static allSettled(vals: any[]): Promise<any[]> {
    //         if (typeof vals.length !== 'number') throw new env.global.TypeError('vals must be an array. Note that Promise.allSettled is not variadic.');
    //         return new Promise((res, rej) => {
    //             const result: any[] = [];
    //             let n = 0;

    //             for (let i = 0; i < vals.length; i++) {
    //                 const value = vals[i];
    //                 if (this.isAwaitable(value)) value.then(
    //                     value => {
    //                         n++;
    //                         result[i] = { status: 'fulfilled', value };
    //                         if (n === vals.length) res(result);
    //                     },
    //                     reason => {
    //                         n++;
    //                         result[i] = { status: 'rejected', reason };
    //                         if (n === vals.length) res(result);
    //                     },
    //                 );
    //                 else {
    //                     n++;
    //                     result[i] = { status: 'fulfilled', value };
    //                 }
    //             }

    //             if (vals.length === n) res(result);
    //         });
    //     }

    //     [syms.callbacks]?: Callback<T>[] = [];
    //     [syms.handled] = false;
    //     [syms.state] = State.Pending;
    //     [syms.value]?: T | unknown;

    //     public then(onFulfil?: PromiseFulfillFunc<T>, onReject?: PromiseRejectFunc) {
    //         return new Promise((resolve, reject) => {
    //             onFulfil ??= v => v;
    //             onReject ??= v => v;

    //             const callback = (func: (val: any) => any) => (v: any) => {
    //                 try {
    //                     resolve(func(v));
    //                 }
    //                 catch (e) { reject(e); }
    //             }
    //             switch (this[syms.state]) {
    //                 case State.Pending:
    //                     this[syms.callbacks]![this[syms.callbacks]!.length] = [callback(onFulfil), callback(onReject)];
    //                     break;
    //                 case State.Fulfilled:
    //                     this[syms.handled] = true;
    //                     callback(onFulfil)(this[syms.value]);
    //                     break;
    //                 case State.Rejected:
    //                     this[syms.handled] = true;
    //                     callback(onReject)(this[syms.value]);
    //                     break;
    //             }
    //         })
    //     }
    //     public catch(func: PromiseRejectFunc) {
    //         return this.then(undefined, func);
    //     }
    //     public finally(func: () => void) {
    //         return this.then(
    //             v => {
    //                 func();
    //                 return v;
    //             },
    //             v => {
    //                 func();
    //                 throw v;
    //             }
    //         );
    //     }

    //     public constructor(func: PromiseFunc<T>) {
    //         internals.pushMessage(true, func, undefined, [
    //             ((v) => resolve(this, v, State.Fulfilled)) as PromiseFulfillFunc<T>,
    //             ((err) => resolve(this, err, State.Rejected)) as PromiseRejectFunc
    //         ]);
    //     }
    // }
    // env.global.Promise = Promise as any;
});
