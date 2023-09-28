interface Environment {
    global: typeof globalThis & Record<string, any>;
    proto(name: string): object;
    setProto(name: string, val: object): void;
    symbol(name: string): symbol;
}
interface Internals {
    object: ObjectConstructor;
    function: FunctionConstructor;
    array: ArrayConstructor;
    promise: PromiseConstructor;
    bool: BooleanConstructor;
    number: NumberConstructor;
    string: StringConstructor;
    symbol: SymbolConstructor;
    error: ErrorConstructor;
    syntax: SyntaxErrorConstructor;
    type: TypeErrorConstructor;
    range: RangeErrorConstructor;

    regexp: typeof RegExp;
    map: typeof Map;
    set: typeof Set;

    timers: {
        setTimeout: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number,
        setInterval: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number,
        clearTimeout: (id: number) => void,
        clearInterval: (id: number) => void,
    }

    markSpecial(...funcs: Function[]): void;
    getEnv(func: Function): Environment | undefined;
    setEnv<T>(func: T, env: Environment): T;
    apply(func: Function, thisArg: any, args: any[], env?: Environment): any;
    bind(func: Function, thisArg: any): any;
    delay(timeout: number, callback: Function): () => void;
    pushMessage(micro: boolean, func: Function, thisArg: any, args: any[]): void;

    strlen(val: string): number;
    char(val: string): number;
    stringFromStrings(arr: string[]): string;
    stringFromChars(arr: number[]): string;
    getSymbol(name?: string): symbol;
    symbolToString(sym: symbol): string;

    isArray(obj: any): boolean;
    generator(func: (_yield: <T>(val: T) => unknown) => (...args: any[]) => unknown): GeneratorFunction;
    defineField(obj: object, key: any, val: any, writable: boolean, enumerable: boolean, configurable: boolean): boolean;
    defineProp(obj: object, key: any, get: Function | undefined, set: Function | undefined, enumerable: boolean, configurable: boolean): boolean;
    keys(obj: object, onlyString: boolean): any[];
    ownProp(obj: any, key: string): PropertyDescriptor<any, any>;
    ownPropKeys(obj: any): any[];
    lock(obj: object, type: 'ext' | 'seal' | 'freeze'): void;
    extensible(obj: object): boolean;

    sort(arr: any[], comaprator: (a: any, b: any) => number): void;

    log(...args: any[]): void;
}

var env: Environment = arguments[0], internals: Internals = arguments[1];

try {
    const Array = env.global.Array = internals.array;
    env.global.Object = internals.object;
    env.global.Function = internals.function;
    env.global.Promise = internals.promise;
    env.global.Boolean = internals.bool;
    env.global.Number = internals.number;
    env.global.String = internals.string;
    env.global.Symbol = internals.symbol;
    env.global.Error = internals.error;
    env.global.SyntaxError = internals.syntax;
    env.global.TypeError = internals.type;
    env.global.RangeError = internals.range;
    env.global.RegExp = internals.regexp;
    env.global.Map = internals.map;
    env.global.Set = internals.set;
    env.global.setInterval = internals.bind(internals.timers.setInterval, internals.timers);
    env.global.setTimeout = internals.bind(internals.timers.setTimeout, internals.timers);
    env.global.clearInterval = internals.bind(internals.timers.clearInterval, internals.timers);
    env.global.clearTimeout = internals.bind(internals.timers.clearTimeout, internals.timers);
    const log = env.global.log = internals.bind(internals.log, internals);

    env.setProto('object', env.global.Object.prototype);
    env.setProto('function', env.global.Function.prototype);
    env.setProto('array', env.global.Array.prototype);
    env.setProto('number', env.global.Number.prototype);
    env.setProto('string', env.global.String.prototype);
    env.setProto('symbol', env.global.Symbol.prototype);
    env.setProto('bool', env.global.Boolean.prototype);

    env.setProto('error', env.global.Error.prototype);
    env.setProto('rangeErr', env.global.RangeError.prototype);
    env.setProto('typeErr', env.global.TypeError.prototype);
    env.setProto('syntaxErr', env.global.SyntaxError.prototype);

    (env.global.Object.prototype as any).__proto__ = null;

    log('Loaded polyfills!');
}
catch (e: any) {
    let err = 'Uncaught error while loading polyfills: ';

    if (typeof Error !== 'undefined' && e instanceof Error && e.toString !== {}.toString) err += e;
    else if ('message' in e) {
        if ('name' in e) err += e.name + ": " + e.message;
        else err += 'Error: ' + e.message;
    }
    else err += "[unknown]";

    internals.log(err);
}
