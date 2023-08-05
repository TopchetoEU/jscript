interface Symbol {
    valueOf(): symbol;
    constructor: SymbolConstructor;
}
interface SymbolConstructor {
    (val?: any): symbol;
    prototype: Symbol;
    for(key: string): symbol;
    keyFor(sym: symbol): string;
    readonly typeName: unique symbol;
}

declare var Symbol: SymbolConstructor;

gt.Symbol = function(this: any, val?: string) {
    if (this !== undefined && this !== null) throw new TypeError("Symbol may not be called with 'new'.");
    if (typeof val !== 'string' && val !== undefined) throw new TypeError('val must be a string or undefined.');
    return internals.symbol(val, true);
} as SymbolConstructor;

Symbol.prototype = internals.symbolProto;
setConstr(Symbol.prototype, Symbol);
(Symbol as any).typeName = Symbol("Symbol.name");

setProps(Symbol, {
    for(key) {
        if (typeof key !== 'string' && key !== undefined) throw new TypeError('key must be a string or undefined.');
        return internals.symbol(key, false);
    },
    keyFor(sym) {
        if (typeof sym !== 'symbol') throw new TypeError('sym must be a symbol.');
        return internals.symStr(sym);
    },
    typeName: Symbol('Symbol.name') as any,
});

Object.defineProperty(Object.prototype, Symbol.typeName, { value: 'Object' });
Object.defineProperty(gt, Symbol.typeName, { value: 'Window' });
