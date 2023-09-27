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

    markSpecial(...funcs: Function[]): void;
    getEnv(func: Function): Environment | undefined;
    setEnv<T>(func: T, env: Environment): T;
    apply(func: Function, thisArg: any, args: any[], env?: Environment): any;
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

    const values = {
        Object: env.global.Object = internals.object,
        Function: env.global.Function = internals.function,
        Array: env.global.Array = internals.array,
        Promise: env.global.Promise = internals.promise,
        Boolean: env.global.Boolean = internals.bool,
        Number: env.global.Number = internals.number,
        String: env.global.String = internals.string,
        Symbol: env.global.Symbol = internals.symbol,
        Error: env.global.Error = internals.error,
        SyntaxError: env.global.SyntaxError = internals.syntax,
        TypeError: env.global.TypeError = internals.type,
        RangeError: env.global.RangeError = internals.range,
        RegExp: env.global.RegExp = internals.regexp,
        Map: env.global.Map = internals.map,
        Set: env.global.Set = internals.set,
    }
    const Array = values.Array;

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

    internals.getEnv(run)?.setProto('array', Array.prototype);
    globalThis.log = (...args) => internals.apply(internals.log, internals, args);

    for (const key in values) {
        (values as any)[key].prototype[env.symbol('Symbol.typeName')] = key;
        log();
    }

    run('timeout');

    env.global.log = log;
    env.global.NewObject = internals.object;

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
