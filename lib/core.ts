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

try {
    var env: Environment = arguments[0], internals: Internals = arguments[1];

    const Object = env.global.Object = internals.object;
    const Function = env.global.Function = internals.function;
    const Array = env.global.Array = internals.array;
    const Promise = env.global.Promise = internals.promise;
    const Boolean = env.global.Boolean = internals.bool;
    const Number = env.global.Number = internals.number;
    const String = env.global.String = internals.string;
    const Symbol = env.global.Symbol = internals.symbol;

    const Map = env.global.Map = internals.map;
    const Set = env.global.Set = internals.set;

    env.setProto('object', Object.prototype);
    env.setProto('function', Function.prototype);
    env.setProto('array', Array.prototype);
    env.setProto('number', Number.prototype);
    env.setProto('string', String.prototype);
    env.setProto('symbol', Symbol.prototype);
    env.setProto('bool', Boolean.prototype);

    (Object.prototype as any).__proto__ = null;

    internals.getEnv(run)?.setProto('array', Array.prototype);
    globalThis.log = (...args) => internals.apply(internals.log, internals, args);

    run('values/errors');
    run('regex');
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
    else err += e;

    log(e);
}