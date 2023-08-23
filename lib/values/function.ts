interface Function {
    apply(this: Function, thisArg: any, argArray?: any): any;
    call(this: Function, thisArg: any, ...argArray: any[]): any;
    bind(this: Function, thisArg: any, ...argArray: any[]): Function;

    toString(): string;

    prototype: any;
    constructor: FunctionConstructor;
    readonly length: number;
    name: string;
}
interface FunctionConstructor extends Function {
    (...args: string[]): (...args: any[]) => any;
    new (...args: string[]): (...args: any[]) => any;
    prototype: Function;
    async<ArgsT extends any[], RetT>(func: (await: <T>(val: T) => Awaited<T>, args: ArgsT) => RetT): Promise<RetT>;
}

interface CallableFunction extends Function {
    (...args: any[]): any;
    apply<ThisArg, Args extends any[], RetT>(this: (this: ThisArg, ...args: Args) => RetT, thisArg: ThisArg, argArray?: Args): RetT;
    call<ThisArg, Args extends any[], RetT>(this: (this: ThisArg, ...args: Args) => RetT, thisArg: ThisArg, ...argArray: Args): RetT;
    bind<ThisArg, Args extends any[], Rest extends any[], RetT>(this: (this: ThisArg, ...args: [ ...Args, ...Rest ]) => RetT, thisArg: ThisArg, ...argArray: Args): (this: void, ...args: Rest) => RetT;
}
interface NewableFunction extends Function {
    new(...args: any[]): any;
    apply<Args extends any[], RetT>(this: new (...args: Args) => RetT, thisArg: any, argArray?: Args): RetT;
    call<Args extends any[], RetT>(this: new (...args: Args) => RetT, thisArg: any, ...argArray: Args): RetT;
    bind<Args extends any[], RetT>(this: new (...args: Args) => RetT, thisArg: any, ...argArray: Args): new (...args: Args) => RetT;
}

declare var Function: FunctionConstructor;

gt.Function = function() {
    throw 'Using the constructor Function() is forbidden.';
} as unknown as FunctionConstructor;

Function.prototype = (Function as any).__proto__ as Function;
setConstr(Function.prototype, Function);

setProps(Function.prototype, {
    apply(thisArg, args) {
        if (typeof args !== 'object') throw 'Expected arguments to be an array-like object.';
        var len = args.length - 0;
        var newArgs: any[] = [];

        while (len >= 0) {
            len--;
            newArgs[len] = args[len];
        }

        return internals.apply(this, thisArg, newArgs);
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
setProps(Function, {
    async(func) {
        if (typeof func !== 'function') throw new TypeError('Expected func to be function.');
        return internals.makeAsync(func);
    }
})