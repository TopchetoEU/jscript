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

Map.prototype[Symbol.iterator] = function() {
    return this.entries();
};

var entries = Map.prototype.entries;
var keys = Map.prototype.keys;
var values = Map.prototype.values;

Map.prototype.entries = function() {
    var it = entries.call(this);
    it[Symbol.iterator] = () => it;
    return it;
};
Map.prototype.keys = function() {
    var it = keys.call(this);
    it[Symbol.iterator] = () => it;
    return it;
};
Map.prototype.values = function() {
    var it = values.call(this);
    it[Symbol.iterator] = () => it;
    return it;
};
