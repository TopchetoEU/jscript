define("values/function", () => {
    var Function = env.global.Function = function() {
        throw 'Using the constructor Function() is forbidden.';
    } as unknown as FunctionConstructor;

    env.setProto('function', Function.prototype);
    setConstr(Function.prototype, Function);

    setProps(Function.prototype, {
        apply(thisArg, args) {
            if (typeof args !== 'object') throw 'Expected arguments to be an array-like object.';
            var len = args.length - 0;
            let newArgs: any[];
            if (internals.isArray(args)) newArgs = args;
            else {
                newArgs = [];

                while (len >= 0) {
                    len--;
                    newArgs[len] = args[len];
                }
            }

            return internals.apply(this, thisArg, newArgs);
        },
        call(thisArg, ...args) {
            return this.apply(thisArg, args);
        },
        bind(thisArg, ...args) {
            const func = this;
            const res = function() {
                const resArgs = [];

                for (let i = 0; i < args.length; i++) {
                    resArgs[i] = args[i];
                }
                for (let i = 0; i < arguments.length; i++) {
                    resArgs[i + args.length] = arguments[i];
                }

                return func.apply(thisArg, resArgs);
            };
            res.name = "<bound> " + func.name;
            return res;
        },
        toString() {
            return 'function (...) { ... }';
        },
    });
    setProps(Function, {
        async(func) {
            if (typeof func !== 'function') throw new TypeError('Expected func to be function.');

            return function (this: any) {
                const args = arguments;

                return new Promise((res, rej) => {
                    const gen = internals.apply(internals.generator(func as any), this, args as any);

                    (function next(type: 'none' | 'err' | 'ret', val?: any) {
                        try {
                            let result;

                            switch (type) {
                                case 'err': result = gen.throw(val); break;
                                case 'ret': result = gen.next(val); break;
                                case 'none': result = gen.next(); break;
                            }
                            if (result.done) res(result.value);
                            else Promise.resolve(result.value).then(
                                v => next('ret', v),
                                v => next('err', v)
                            )
                        }
                        catch (e) {
                            rej(e);
                        }
                    })('none');
                });
            };
        },
        asyncGenerator(func) {
            if (typeof func !== 'function') throw new TypeError('Expected func to be function.');

            return function(this: any, ...args: any[]) {
                const gen = internals.apply(internals.generator((_yield) => func(
                    val => _yield(['await', val]) as any,
                    val => _yield(['yield', val])
                )), this, args) as Generator<['await' | 'yield', any]>;

                const next = (resolve: Function, reject: Function, type: 'none' | 'val' | 'ret' | 'err', val?: any) => {
                    let res;

                    try {
                        switch (type) {
                            case 'val': res = gen.next(val); break;
                            case 'ret': res = gen.return(val); break;
                            case 'err': res = gen.throw(val); break;
                            default: res = gen.next(); break;
                        }
                    }
                    catch (e) { return reject(e); }

                    if (res.done) return { done: true, res: <any>res };
                    else if (res.value[0] === 'await') Promise.resolve(res.value[1]).then(
                        v => next(resolve, reject, 'val', v),
                        v => next(resolve, reject, 'err', v),
                    )
                    else resolve({ done: false, value: res.value[1] });
                };

                return {
                    next() {
                        const args = arguments;
                        if (arguments.length === 0) return new Promise((res, rej) => next(res, rej, 'none'));
                        else return new Promise((res, rej) => next(res, rej, 'val', args[0]));
                    },
                    return: (value) => new Promise((res, rej) => next(res, rej, 'ret', value)),
                    throw: (value) => new Promise((res, rej) => next(res, rej, 'err', value)),
                    [env.global.Symbol.asyncIterator]() { return this; }
                }
            }
        },
        generator(func) {
            if (typeof func !== 'function') throw new TypeError('Expected func to be function.');
            const gen = internals.generator(func);
            return function(this: any, ...args: any[]) {
                const it = internals.apply(gen, this, args);

                return {
                    next: (...args) => internals.apply(it.next, it, args),
                    return: (val) => internals.apply(it.next, it, [val]),
                    throw: (val) => internals.apply(it.next, it, [val]),
                    [env.global.Symbol.iterator]() { return this; }
                }
            }
        }
    })
    internals.markSpecial(Function);
});