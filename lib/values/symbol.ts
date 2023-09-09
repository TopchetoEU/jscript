define("values/symbol", () => {
    const symbols: Record<string, symbol> = { };

    var Symbol = env.global.Symbol = function(this: any, val?: string) {
        if (this !== undefined && this !== null) throw new TypeError("Symbol may not be called with 'new'.");
        if (typeof val !== 'string' && val !== undefined) throw new TypeError('val must be a string or undefined.');
        return internals.symbol(val);
    } as SymbolConstructor;

    env.setProto('symbol', Symbol.prototype);
    setConstr(Symbol.prototype, Symbol);

    setProps(Symbol, {
        for(key) {
            if (typeof key !== 'string' && key !== undefined) throw new TypeError('key must be a string or undefined.');
            if (key in symbols) return symbols[key];
            else return symbols[key] = internals.symbol(key);
        },
        keyFor(sym) {
            if (typeof sym !== 'symbol') throw new TypeError('sym must be a symbol.');
            return internals.symbolToString(sym);
        },

        typeName: Symbol("Symbol.name") as any,
        replace: Symbol('Symbol.replace') as any,
        match: Symbol('Symbol.match') as any,
        matchAll: Symbol('Symbol.matchAll') as any,
        split: Symbol('Symbol.split') as any,
        search: Symbol('Symbol.search') as any,
        iterator: Symbol('Symbol.iterator') as any,
        asyncIterator: Symbol('Symbol.asyncIterator') as any,
    });

    internals.defineField(env.global.Object.prototype, Symbol.typeName, 'Object', false, false, false);
    internals.defineField(env.global, Symbol.typeName, 'Window', false, false, false);
});