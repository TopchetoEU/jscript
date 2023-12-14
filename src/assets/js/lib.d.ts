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
type ReplaceFunc = (match: string, ...args: any[]) => string;

type PromiseResult<T> = { type: 'fulfilled'; value: T; } | { type: 'rejected'; reason: any; }

// wippidy-wine, this code is now mine :D
type Awaited<T> =
    T extends null | undefined ? T : // special case for `null | undefined` when not in `--strictNullChecks` mode
        T extends object & { then(onfulfilled: infer F, ...args: infer _): any } ? // `await` only unwraps object types with a callable `then`. Non-object types are not unwrapped
            F extends ((value: infer V, ...args: infer _) => any) ? // if the argument to `then` is callable, extracts the first argument
                Awaited<V> : // recursively unwrap the value
                never : // the argument to `then` was not callable
        T;

type IteratorYieldResult<TReturn> =
    { done?: false; } &
    (TReturn extends undefined ? { value?: undefined; } : { value: TReturn; });

type IteratorReturnResult<TReturn> =
    { done: true } &
    (TReturn extends undefined ? { value?: undefined; } : { value: TReturn; });

type IteratorResult<T, TReturn = any> = IteratorYieldResult<T> | IteratorReturnResult<TReturn>;

interface Thenable<T> {
    then<NextT = void>(onFulfilled?: (val: T) => NextT, onRejected?: (err: any) => NextT): Promise<Awaited<NextT>>;
}

interface RegExpResultIndices extends Array<[number, number]> {
    groups?: { [name: string]: [number, number]; };
}
interface RegExpResult extends Array<string> {
    groups?: { [name: string]: string; };
    index: number;
    input: string;
    indices?: RegExpResultIndices;
    escape(raw: string, flags: string): RegExp;
}

interface Matcher {
    [Symbol.match](target: string): RegExpResult | string[] | null;
    [Symbol.matchAll](target: string): IterableIterator<RegExpResult>;
}
interface Splitter {
    [Symbol.split](target: string, limit?: number, sensible?: boolean): string[];
}
interface Replacer {
    [Symbol.replace](target: string, replacement: string | ReplaceFunc): string;
}
interface Searcher {
    [Symbol.search](target: string, reverse?: boolean, start?: number): number;
}

type FlatArray<Arr, Depth extends number> = {
    "done": Arr,
    "recur": Arr extends Array<infer InnerArr>
        ? FlatArray<InnerArr, [-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20][Depth]>
        : Arr
}[Depth extends -1 ? "done" : "recur"];

interface IArguments {
    [i: number]: any;
    length: number;
}

interface Iterator<T, TReturn = any, TNext = undefined> {
    next(...args: [] | [TNext]): IteratorResult<T, TReturn>;
    return?(value?: TReturn): IteratorResult<T, TReturn>;
    throw?(e?: any): IteratorResult<T, TReturn>;
}
interface Iterable<T> {
    [Symbol.iterator](): Iterator<T>;
}
interface IterableIterator<T> extends Iterator<T> {
    [Symbol.iterator](): IterableIterator<T>;
}

interface AsyncIterator<T, TReturn = any, TNext = undefined> {
    next(...args: [] | [TNext]): Promise<IteratorResult<T, TReturn>>;
    return?(value?: TReturn | Thenable<TReturn>): Promise<IteratorResult<T, TReturn>>;
    throw?(e?: any): Promise<IteratorResult<T, TReturn>>;
}
interface AsyncIterable<T> {
    [Symbol.asyncIterator](): AsyncIterator<T>;
}
interface AsyncIterableIterator<T> extends AsyncIterator<T> {
    [Symbol.asyncIterator](): AsyncIterableIterator<T>;
}

interface Generator<T = unknown, TReturn = unknown, TNext = unknown> extends Iterator<T, TReturn, TNext> {
    [Symbol.iterator](): Generator<T, TReturn, TNext>;
    return(value: TReturn): IteratorResult<T, TReturn>;
    throw(e: any): IteratorResult<T, TReturn>;
}
interface GeneratorFunction {
    new (...args: any[]): Generator;
    (...args: any[]): Generator;
    readonly length: number;
    readonly name: string;
    readonly prototype: Generator;
}

interface AsyncGenerator<T = unknown, TReturn = unknown, TNext = unknown> extends AsyncIterator<T, TReturn, TNext> {
    return(value: TReturn | Thenable<TReturn>): Promise<IteratorResult<T, TReturn>>;
    throw(e: any): Promise<IteratorResult<T, TReturn>>;
    [Symbol.asyncIterator](): AsyncGenerator<T, TReturn, TNext>;
}
interface AsyncGeneratorFunction {
    new (...args: any[]): AsyncGenerator;
    (...args: any[]): AsyncGenerator;
    readonly length: number;
    readonly name: string;
    readonly prototype: AsyncGenerator;
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

interface Array<T> extends IterableIterator<T> {
    [i: number]: T;

    length: number;

    toString(): string;
    // toLocaleString(): string;
    join(separator?: string): string;
    fill(val: T, start?: number, end?: number): T[];
    pop(): T | undefined;
    push(...items: T[]): number;
    concat(...items: (T | T[])[]): T[];
    concat(...items: (T | T[])[]): T[];
    join(separator?: string): string;
    reverse(): T[];
    shift(): T | undefined;
    slice(start?: number, end?: number): T[];
    sort(compareFn?: (a: T, b: T) => number): this;
    splice(start: number, deleteCount?: number | undefined, ...items: T[]): T[];
    unshift(...items: T[]): number;
    indexOf(searchElement: T, fromIndex?: number): number;
    lastIndexOf(searchElement: T, fromIndex?: number): number;
    every(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): boolean;
    some(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): boolean;
    forEach(callbackfn: (value: T, index: number, array: T[]) => void, thisArg?: any): void;
    includes(el: any, start?: number): boolean;

    map<U>(callbackfn: (value: T, index: number, array: T[]) => U, thisArg?: any): U[];
    filter(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): T[];
    find(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): T[];
    findIndex(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): number;
    findLast(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): T[];
    findLastIndex(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): number;

    flat<D extends number = 1>(depth?: D): FlatArray<T, D>;
    flatMap(func: (val: T, i: number, arr: T[]) => T | T[], thisAarg?: any): FlatArray<T[], 1>;
    sort(func?: (a: T, b: T) => number): this;

    reduce(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T): T;
    reduce(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T, initialValue: T): T;
    reduce<U>(callbackfn: (previousValue: U, currentValue: T, currentIndex: number, array: T[]) => U, initialValue: U): U;
    reduceRight(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T): T;
    reduceRight(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T, initialValue: T): T;
    reduceRight<U>(callbackfn: (previousValue: U, currentValue: T, currentIndex: number, array: T[]) => U, initialValue: U): U;

    entries(): IterableIterator<[number, T]>;
    values(): IterableIterator<T>;
    keys(): IterableIterator<number>;
}
interface ArrayConstructor {
    new <T>(arrayLength?: number): T[];
    new <T>(...items: T[]): T[];
    <T>(arrayLength?: number): T[];
    <T>(...items: T[]): T[];
    isArray(arg: any): arg is any[];
    prototype: Array<any>;
}

interface Boolean {
    toString(): string;
    valueOf(): boolean;
}
interface BooleanConstructor {
    (val: any): boolean;
    new (val: any): Boolean;
    prototype: Boolean;
}

interface Error {
    name: string;
    message: string;
    stack: string[];
    toString(): string;
}
interface ErrorConstructor {
    (msg?: any): Error;
    new (msg?: any): Error;
    prototype: Error;
}

interface TypeErrorConstructor extends ErrorConstructor {
    (msg?: any): TypeError;
    new (msg?: any): TypeError;
    prototype: Error;
}
interface TypeError extends Error {
    name: 'TypeError';
}

interface RangeErrorConstructor extends ErrorConstructor {
    (msg?: any): RangeError;
    new (msg?: any): RangeError;
    prototype: Error;
}
interface RangeError extends Error {
    name: 'RangeError';
}

interface SyntaxErrorConstructor extends ErrorConstructor {
    (msg?: any): RangeError;
    new (msg?: any): RangeError;
    prototype: Error;
}
interface SyntaxError extends Error {
    name: 'SyntaxError';
}

interface Function {
    apply(this: Function, thisArg: any, argArray?: any): any;
    call(this: Function, thisArg: any, ...argArray: any[]): any;
    bind(this: Function, thisArg: any, ...argArray: any[]): Function;

    toString(): string;

    prototype: any;
    readonly length: number;
    name: string;
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
interface FunctionConstructor extends Function {
    (...args: string[]): (...args: any[]) => any;
    new (...args: string[]): (...args: any[]) => any;
    prototype: Function;
    async<ArgsT extends any[], RetT>(
        func: (await: <T>(val: T) => Awaited<T>) => (...args: ArgsT) => RetT
    ): (...args: ArgsT) => Promise<RetT>;
    asyncGenerator<ArgsT extends any[], RetT>(
        func: (await: <T>(val: T) => Awaited<T>, _yield: <T>(val: T) => void) => (...args: ArgsT) => RetT
    ): (...args: ArgsT) => AsyncGenerator<RetT>;
    generator<ArgsT extends any[], T = unknown, RetT = unknown, TNext = unknown>(
        func: (_yield: <T>(val: T) => TNext) => (...args: ArgsT) => RetT
    ): (...args: ArgsT) => Generator<T, RetT, TNext>;
}

interface Number {
    toString(): string;
    valueOf(): number;
}
interface NumberConstructor {
    (val: any): number;
    new (val: any): Number;
    prototype: Number;
    parseInt(val: unknown): number;
    parseFloat(val: unknown): number;
}

interface Object {
    constructor: NewableFunction;
    [Symbol.typeName]: string;

    valueOf(): this;
    toString(): string;
    hasOwnProperty(key: any): boolean;
}
interface ObjectConstructor {
    (arg: string): String;
    (arg: number): Number;
    (arg: boolean): Boolean;
    (arg?: undefined | null): {};
    <T extends object>(arg: T): T;

    new (arg: string): String;
    new (arg: number): Number;
    new (arg: boolean): Boolean;
    new (arg?: undefined | null): {};
    new <T extends object>(arg: T): T;

    prototype: Object;

    assign<T extends object>(target: T, ...src: object[]): T;
    create<T extends object>(proto: T, props?: { [key: string]: PropertyDescriptor<any, T> }): T;

    keys<T extends object>(obj: T, onlyString?: true): (keyof T)[];
    keys<T extends object>(obj: T, onlyString: false): any[];
    entries<T extends object>(obj: T, onlyString?: true): [keyof T, T[keyof T]][];
    entries<T extends object>(obj: T, onlyString: false): [any, any][];
    values<T extends object>(obj: T, onlyString?: true): (T[keyof T])[];
    values<T extends object>(obj: T, onlyString: false): any[];

    fromEntries(entries: Iterable<[any, any]>): object;

    defineProperty<T, ThisT extends object>(obj: ThisT, key: any, desc: PropertyDescriptor<T, ThisT>): ThisT;
    defineProperties<ThisT extends object>(obj: ThisT, desc: { [key: string]: PropertyDescriptor<any, ThisT> }): ThisT;

    getOwnPropertyNames<T extends object>(obj: T): (keyof T)[];
    getOwnPropertySymbols<T extends object>(obj: T): (keyof T)[];
    hasOwn<T extends object, KeyT>(obj: T, key: KeyT): boolean;

    getOwnPropertyDescriptor<T extends object, KeyT extends keyof T>(obj: T, key: KeyT): PropertyDescriptor<T[KeyT], T>;
    getOwnPropertyDescriptors<T extends object>(obj: T): { [x in keyof T]: PropertyDescriptor<T[x], T> };

    getPrototypeOf(obj: any): object | null;
    setPrototypeOf<T>(obj: T, proto: object | null): T;

    preventExtensions<T extends object>(obj: T): T;
    seal<T extends object>(obj: T): T;
    freeze<T extends object>(obj: T): T;

    isExtensible(obj: object): boolean;
    isSealed(obj: object): boolean;
    isFrozen(obj: object): boolean;
}

interface String {
    [i: number]: string;

    toString(): string;
    valueOf(): string;

    charAt(pos: number): string;
    charCodeAt(pos: number): number;
    substring(start?: number, end?: number): string;
    slice(start?: number, end?: number): string;
    substr(start?: number, length?: number): string;

    startsWith(str: string, pos?: number): string;
    endsWith(str: string, pos?: number): string;

    replace(pattern: string | Replacer, val: string | ReplaceFunc): string;
    replaceAll(pattern: string | Replacer, val: string | ReplaceFunc): string;

    match(pattern: string | Matcher): RegExpResult | string[] | null;
    matchAll(pattern: string | Matcher): IterableIterator<RegExpResult>;

    split(pattern: string | Splitter, limit?: number, sensible?: boolean): string;

    concat(...others: string[]): string;
    indexOf(term: string | Searcher, start?: number): number;
    lastIndexOf(term: string | Searcher, start?: number): number;

    toLowerCase(): string;
    toUpperCase(): string;

    trim(): string;

    includes(term: string, start?: number): boolean;

    length: number;
}
interface StringConstructor {
    (val: any): string;
    new (val: any): String;

    fromCharCode(val: number): string;

    prototype: String;
}

interface Symbol {
    valueOf(): symbol;
}
interface SymbolConstructor {
    (val?: any): symbol;
    new(...args: any[]): never;
    prototype: Symbol;
    for(key: string): symbol;
    keyFor(sym: symbol): string;

    readonly typeName: unique symbol;
    readonly match: unique symbol;
    readonly matchAll: unique symbol;
    readonly split: unique symbol;
    readonly replace: unique symbol;
    readonly search: unique symbol;
    readonly iterator: unique symbol;
    readonly asyncIterator: unique symbol;
}

interface Promise<T> extends Thenable<T> {
    catch<ResT = void>(func: (err: unknown) => ResT): Promise<ResT>;
    finally(func: () => void): Promise<T>;
}
interface PromiseConstructor {
    prototype: Promise<any>;

    new <T>(func: (res: (val: T) => void, rej: (err: unknown) => void) => void): Promise<Awaited<T>>;
    resolve<T>(val: T): Promise<Awaited<T>>;
    reject(val: any): Promise<never>;

    isAwaitable(val: unknown): val is Thenable<any>;
    any<T>(promises: T[]): Promise<Awaited<T>>;
    race<T>(promises: (Promise<T>|T)[]): Promise<T>;
    all<T extends any[]>(promises: T): Promise<{ [Key in keyof T]: Awaited<T[Key]> }>;
    allSettled<T extends any[]>(...promises: T): Promise<[...{ [P in keyof T]: PromiseResult<Awaited<T[P]>>}]>;
}

interface FileStat {
    type: 'file' | 'folder';
    mode: 'r' | 'rw';
}
interface File {
    readonly pointer: Promise<number>;
    readonly length: Promise<number>;
    readonly mode: Promise<'' | 'r' | 'rw'>;

    read(n: number): Promise<number[]>;
    write(buff: number[]): Promise<void>;
    close(): Promise<void>;
    setPointer(val: number): Promise<void>;
}
interface Filesystem {
    open(path: string, mode: 'r' | 'rw'): Promise<File>;
    ls(path: string): AsyncIterableIterator<string>;
    mkdir(path: string): Promise<void>;
    mkfile(path: string): Promise<void>;
    rm(path: string, recursive?: boolean): Promise<void>;
    stat(path: string): Promise<FileStat>;
    exists(path: string): Promise<boolean>;
}

interface Encoding {
    encode(val: string): number[];
    decode(val: number[]): string;
}

declare var String: StringConstructor;
//@ts-ignore
declare const arguments: IArguments;
declare var NaN: number;
declare var Infinity: number;

declare var setTimeout: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number;
declare var setInterval: <T extends any[]>(handle: (...args: [ ...T, ...any[] ]) => void, delay?: number, ...args: T) => number;

declare var clearTimeout: (id: number) => void;
declare var clearInterval: (id: number) => void;

declare var parseInt: typeof Number.parseInt;
declare var parseFloat: typeof Number.parseFloat;

declare function log(...vals: any[]): void;

declare var Array: ArrayConstructor;
declare var Boolean: BooleanConstructor;
declare var Promise: PromiseConstructor;
declare var Function: FunctionConstructor;
declare var Number: NumberConstructor;
declare var Object: ObjectConstructor;
declare var Symbol: SymbolConstructor;
declare var Promise: PromiseConstructor;
declare var Math: MathObject;
declare var Encoding: Encoding;
declare var Filesystem: Filesystem;

declare var Error: ErrorConstructor;
declare var RangeError: RangeErrorConstructor;
declare var TypeError: TypeErrorConstructor;
declare var SyntaxError: SyntaxErrorConstructor;
declare var self: typeof globalThis;

declare class Map<KeyT, ValueT> {
    public [Symbol.iterator](): IterableIterator<[KeyT, ValueT]>;

    public clear(): void;
    public delete(key: KeyT): boolean;

    public entries(): IterableIterator<[KeyT, ValueT]>;
    public keys(): IterableIterator<KeyT>;
    public values(): IterableIterator<ValueT>;

    public get(key: KeyT): ValueT;
    public set(key: KeyT, val: ValueT): this;
    public has(key: KeyT): boolean;

    public get size(): number;

    public forEach(func: (key: KeyT, val: ValueT, map: Map<KeyT, ValueT>) => void, thisArg?: any): void;

    public constructor();
}
declare class Set<T> {
    public [Symbol.iterator](): IterableIterator<T>;

    public entries(): IterableIterator<[T, T]>;
    public keys(): IterableIterator<T>;
    public values(): IterableIterator<T>;

    public clear(): void;

    public add(val: T): this;
    public delete(val: T): boolean;
    public has(key: T): boolean;

    public get size(): number;

    public forEach(func: (key: T, set: Set<T>) => void, thisArg?: any): void;

    public constructor();
}

declare class RegExp implements Matcher, Splitter, Replacer, Searcher {
    static escape(raw: any, flags?: string): RegExp;

    prototype: RegExp;

    exec(val: string): RegExpResult | null;
    test(val: string): boolean;
    toString(): string;

    [Symbol.match](target: string): RegExpResult | string[] | null;
    [Symbol.matchAll](target: string): IterableIterator<RegExpResult>;
    [Symbol.split](target: string, limit?: number, sensible?: boolean): string[];
    [Symbol.replace](target: string, replacement: string | ReplaceFunc): string;
    [Symbol.search](target: string, reverse?: boolean, start?: number): number;

    readonly dotAll: boolean;
    readonly global: boolean;
    readonly hasIndices: boolean;
    readonly ignoreCase: boolean;
    readonly multiline: boolean;
    readonly sticky: boolean;
    readonly unicode: boolean;

    readonly source: string;
    readonly flags: string;

    lastIndex: number;

    constructor(pattern?: string, flags?: string);
    constructor(pattern?: RegExp, flags?: string);
}
