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

Set.prototype[Symbol.iterator] = function() {
    return this.values();
};

(() => {
    var entries = Set.prototype.entries;
    var keys = Set.prototype.keys;
    var values = Set.prototype.values;

    Set.prototype.entries = function() {
        var it = entries.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Set.prototype.keys = function() {
        var it = keys.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Set.prototype.values = function() {
        var it = values.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
})();