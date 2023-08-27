define("values/function", () => {
    var Function = env.global.Function = function() {
        throw 'Using the constructor Function() is forbidden.';
    } as unknown as FunctionConstructor;

    Function.prototype = (Function as any).__proto__ as Function;
    setConstr(Function.prototype, Function, env);

    setProps(Function.prototype, env, {
        apply(thisArg, args) {
            if (typeof args !== 'object') throw 'Expected arguments to be an array-like object.';
            var len = args.length - 0;
            let newArgs: any[];
            if (Array.isArray(args)) newArgs = args;
            else {
                newArgs = [];

                while (len >= 0) {
                    len--;
                    newArgs[len] = args[len];
                }
            }

            return env.internals.apply(this, thisArg, newArgs);
        },
        call(thisArg, ...args) {
            return this.apply(thisArg, args);
        },
        bind(thisArg, ...args) {
            var func = this;

            var res = function() {
                var resArgs = [];

                for (var i = 0; i < args.length; i++) {
                    resArgs[i] = args[i];
                }
                for (var i = 0; i < arguments.length; i++) {
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
    setProps(Function, env, {
        async(func) {
            if (typeof func !== 'function') throw new TypeError('Expected func to be function.');

            return function (this: any) {
                const args = arguments;

                return new Promise((res, rej) => {
                    const gen = Function.generator(func as any).apply(this, args as any);

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

            return function(this: any) {
                const gen = Function.generator<any[], ['await' | 'yield', any]>((_yield) => func(
                    val => _yield(['await', val]) as any,
                    val => _yield(['yield', val])
                )).apply(this, arguments as any);

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
                    [Symbol.asyncIterator]() { return this; }
                }
            }
        },
        generator(func) {
            if (typeof func !== 'function') throw new TypeError('Expected func to be function.');
            const gen = env.internals.makeGenerator(func);
            return (...args: any[]) => {
                const it = gen(args);

                return {
                    next: it.next,
                    return: it.return,
                    throw: it.throw,
                    [Symbol.iterator]() { return this; }
                }
            }
        }
    })
});