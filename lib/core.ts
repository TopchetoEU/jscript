type PropertyDescriptor<T, ThisT> = {
    value: any;
    writable?: boolean;
    enumerable?: boolean;
    configurable?: boolean;
} | {
    get?(this: ThisT): T;
    set(this: ThisT, val: T): void;
    enumerable?: boolean;
    configurable?: boolean;
} | {
    get(this: ThisT): T;
    set?(this: ThisT, val: T): void;
    enumerable?: boolean;
    configurable?: boolean;
};
type Exclude<T, U> = T extends U ? never : T;
type Extract<T, U> = T extends U ? T : never;
type Record<KeyT extends string | number | symbol, ValT> = { [x in KeyT]: ValT }

interface IArguments {
    [i: number]: any;
    length: number;
}

interface MathObject {
    readonly E: number;
    readonly PI: number;
    readonly SQRT2: number;
    readonly SQRT1_2: number;
    readonly LN2: number;
    readonly LN10: number;
    readonly LOG2E: number;
    readonly LOG10E: number;

    asin(x: number): number;
    acos(x: number): number;
    atan(x: number): number;
    atan2(y: number, x: number): number;
    asinh(x: number): number;
    acosh(x: number): number;
    atanh(x: number): number;
    sin(x: number): number;
    cos(x: number): number;
    tan(x: number): number;
    sinh(x: number): number;
    cosh(x: number): number;
    tanh(x: number): number;
    sqrt(x: number): number;
    cbrt(x: number): number;
    hypot(...vals: number[]): number;
    imul(a: number, b: number): number;
    exp(x: number): number;
    expm1(x: number): number;
    pow(x: number, y: number): number;
    log(x: number): number;
    log10(x: number): number;
    log1p(x: number): number;
    log2(x: number): number;
    ceil(x: number): number;
    floor(x: number): number;
    round(x: number): number;
    fround(x: number): number;
    trunc(x: number): number;
    abs(x: number): number;
    max(...vals: number[]): number;
    min(...vals: number[]): number;
    sign(x: number): number;
    random(): number;
    clz32(x: number): number;
}


//@ts-ignore
declare const arguments: IArguments;
declare const Math: MathObject;

declare var setTimeout: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number;
declare var setInterval: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number;

declare var clearTimeout: (id: number) => void;
declare var clearInterval: (id: number) => void;

/** @internal */
declare var internals: any;
/** @internal */
declare function run(file: string, pollute?: boolean): void;

/** @internal */
type ReplaceThis<T, ThisT> = T extends ((...args: infer ArgsT) => infer RetT) ?
    ((this: ThisT, ...args: ArgsT) => RetT) :
    T;

/** @internal */
declare var setProps: <
    TargetT extends object,
    DescT extends { [x in Exclude<keyof TargetT, 'constructor'> ]?: ReplaceThis<TargetT[x], TargetT> }
>(target: TargetT, desc: DescT) => void;
/** @internal */
declare var setConstr: <ConstrT, T extends { constructor: ConstrT }>(target: T, constr: ConstrT) => void;

declare function log(...vals: any[]): void;
/** @internal */
declare var lgt: typeof globalThis, gt: typeof globalThis;

declare function assert(condition: () => unknown, message?: string): boolean;

gt.assert = (cond, msg) => {
    try {
        if (!cond()) throw 'condition not satisfied';
        log('Passed ' + msg);
        return true;
    }
    catch (e) {
        log('Failed ' + msg + ' because of: ' + e);
        return false;
    }
}

try {
    lgt.setProps = (target, desc) => {
        var props = internals.keys(desc, false);
        for (var i = 0; i < props.length; i++) {
            var key = props[i];
            internals.defineField(
                target, key, (desc as any)[key],
                true, // writable
                false, // enumerable
                true // configurable
            );
        }
    }
    lgt.setConstr = (target, constr) => {
        internals.defineField(
            target, 'constructor', constr,
            true, // writable
            false, // enumerable
            true // configurable
        );
    }
    
    run('values/object.js');
    run('values/symbol.js');
    run('values/function.js');
    run('values/errors.js');
    run('values/string.js');
    run('values/number.js');
    run('values/boolean.js');
    run('values/array.js');
    
    internals.special(Object, Function, Error, Array);
    
    gt.setTimeout = (func, delay, ...args) => {
        if (typeof func !== 'function') throw new TypeError("func must be a function.");
        delay = (delay ?? 0) - 0;
        return internals.setTimeout(() => func(...args), delay)
    };
    gt.setInterval = (func, delay, ...args) => {
        if (typeof func !== 'function') throw new TypeError("func must be a function.");
        delay = (delay ?? 0) - 0;
        return internals.setInterval(() => func(...args), delay)
    };
    
    gt.clearTimeout = (id) => {
        id = id | 0;
        internals.clearTimeout(id);
    };
    gt.clearInterval = (id) => {
        id = id | 0;
        internals.clearInterval(id);
    };
    
    
    run('iterators.js');
    run('promise.js');
    run('map.js', true);
    run('set.js', true);
    run('regex.js');
    run('require.js');
    
    log('Loaded polyfills!');
}
catch (e: any) {
    var err = 'Uncaught error while loading polyfills: ';
    if (typeof Error !== 'undefined' && e instanceof Error && e.toString !== {}.toString) err += e;
    else if ('message' in e) {
        if ('name' in e) err += e.name + ": " + e.message;
        else err += 'Error: ' + e.message;
    }
    else err += e;
}
