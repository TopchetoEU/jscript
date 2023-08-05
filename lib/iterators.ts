interface SymbolConstructor {
    readonly iterator: unique symbol;
    readonly asyncIterator: unique symbol;
}

type IteratorYieldResult<TReturn> =
    { done?: false; } &
    (TReturn extends undefined ? { value?: undefined; } : { value: TReturn; });

type IteratorReturnResult<TReturn> =
    { done: true } &
    (TReturn extends undefined ? { value?: undefined; } : { value: TReturn; });

type IteratorResult<T, TReturn = any> = IteratorYieldResult<T> | IteratorReturnResult<TReturn>;

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

interface Generator<T = unknown, TReturn = any, TNext = unknown> extends Iterator<T, TReturn, TNext> {
    // NOTE: 'next' is defined using a tuple to ensure we report the correct assignability errors in all places.
    next(...args: [] | [TNext]): IteratorResult<T, TReturn>;
    return(value: TReturn): IteratorResult<T, TReturn>;
    throw(e: any): IteratorResult<T, TReturn>;
    [Symbol.iterator](): Generator<T, TReturn, TNext>;
}

interface GeneratorFunction {
    /**
     * Creates a new Generator object.
     * @param args A list of arguments the function accepts.
     */
    new (...args: any[]): Generator;
    /**
     * Creates a new Generator object.
     * @param args A list of arguments the function accepts.
     */
    (...args: any[]): Generator;
    /**
     * The length of the arguments.
     */
    readonly length: number;
    /**
     * Returns the name of the function.
     */
    readonly name: string;
    /**
     * A reference to the prototype.
     */
    readonly prototype: Generator;
}

interface GeneratorFunctionConstructor {
    /**
     * Creates a new Generator function.
     * @param args A list of arguments the function accepts.
     */
    new (...args: string[]): GeneratorFunction;
    /**
     * Creates a new Generator function.
     * @param args A list of arguments the function accepts.
     */
    (...args: string[]): GeneratorFunction;
    /**
     * The length of the arguments.
     */
    readonly length: number;
    /**
     * Returns the name of the function.
     */
    readonly name: string;
    /**
     * A reference to the prototype.
     */
}

interface AsyncIterator<T, TReturn = any, TNext = undefined> {
    // NOTE: 'next' is defined using a tuple to ensure we report the correct assignability errors in all places.
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

interface AsyncGenerator<T = unknown, TReturn = any, TNext = unknown> extends AsyncIterator<T, TReturn, TNext> {
    // NOTE: 'next' is defined using a tuple to ensure we report the correct assignability errors in all places.
    next(...args: [] | [TNext]): Promise<IteratorResult<T, TReturn>>;
    return(value: TReturn | Thenable<TReturn>): Promise<IteratorResult<T, TReturn>>;
    throw(e: any): Promise<IteratorResult<T, TReturn>>;
    [Symbol.asyncIterator](): AsyncGenerator<T, TReturn, TNext>;
}

interface AsyncGeneratorFunction {
    /**
     * Creates a new AsyncGenerator object.
     * @param args A list of arguments the function accepts.
     */
    new (...args: any[]): AsyncGenerator;
    /**
     * Creates a new AsyncGenerator object.
     * @param args A list of arguments the function accepts.
     */
    (...args: any[]): AsyncGenerator;
    /**
     * The length of the arguments.
     */
    readonly length: number;
    /**
     * Returns the name of the function.
     */
    readonly name: string;
    /**
     * A reference to the prototype.
     */
    readonly prototype: AsyncGenerator;
}

interface AsyncGeneratorFunctionConstructor {
    /**
     * Creates a new AsyncGenerator function.
     * @param args A list of arguments the function accepts.
     */
    new (...args: string[]): AsyncGeneratorFunction;
    /**
     * Creates a new AsyncGenerator function.
     * @param args A list of arguments the function accepts.
     */
    (...args: string[]): AsyncGeneratorFunction;
    /**
     * The length of the arguments.
     */
    readonly length: number;
    /**
     * Returns the name of the function.
     */
    readonly name: string;
    /**
     * A reference to the prototype.
     */
    readonly prototype: AsyncGeneratorFunction;
}


interface Array<T> extends IterableIterator<T> {
    entries(): IterableIterator<[number, T]>;
    values(): IterableIterator<T>;
    keys(): IterableIterator<number>;
}

setProps(Symbol, {
    iterator: Symbol("Symbol.iterator") as any,
    asyncIterator: Symbol("Symbol.asyncIterator") as any,
});

setProps(Array.prototype, {
    [Symbol.iterator]: function() {
        return this.values();
    },

    values() {
        var i = 0;

        return {
            next: () => {
                while (i < this.length) {
                    if (i++ in this) return { done: false, value: this[i - 1] };
                }
                return { done: true, value: undefined };
            },
            [Symbol.iterator]() { return this; }
        };
    },
    keys() {
        var i = 0;

        return {
            next: () => {
                while (i < this.length) {
                    if (i++ in this) return { done: false, value: i - 1 };
                }
                return { done: true, value: undefined };
            },
            [Symbol.iterator]() { return this; }
        };
    },
    entries() {
        var i = 0;

        return {
            next: () => {
                while (i < this.length) {
                    if (i++ in this) return { done: false, value: [i - 1, this[i - 1]] };
                }
                return { done: true, value: undefined };
            },
            [Symbol.iterator]() { return this; }
        };
    },
});
