interface Environment {
    global: typeof globalThis & Record<string, any>;
    proto(name: string): object;
    setProto(name: string, val: object): void;
    symbol(name: string): symbol;
}
interface Internals {
    object: ObjectConstructor;
    function: FunctionConstructor;
    promise: typeof Promise;

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
    symbol(name?: string): symbol;
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

    constructor: {
        log(...args: any[]): void;
    }
}

var env: Environment = arguments[0], internals: Internals = arguments[1];
globalThis.log = internals.constructor.log;
var i = 0.0;

try {
    var Object = env.global.Object = internals.object;
    var Function = env.global.Function = internals.function;
    var Promise = env.global.Promise = internals.promise;

    env.setProto('object', Object.prototype);
    env.setProto('function', Function.prototype);

    (Object.prototype as any).__proto__ = null;

    run('values/symbol');

    run('values/errors');
    run('values/string');
    run('values/number');
    run('values/boolean');
    run('values/array');
    run('map');
    run('set');
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