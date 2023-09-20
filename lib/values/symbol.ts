define("values/symbol", () => {
    const symbols: Record<string, symbol> = { };

    var Symbol = env.global.Symbol = function(this: any, val?: string) {
        if (this !== undefined && this !== null) throw new env.global.TypeError("Symbol may not be called with 'new'.");
        if (typeof val !== 'string' && val !== undefined) throw new env.global.TypeError('val must be a string or undefined.');
        return internals.symbol(val);
    } as SymbolConstructor;

    env.setProto('symbol', Symbol.prototype);
    setConstr(Symbol.prototype, Symbol);

    setProps(Symbol, {
        for(key) {
            if (typeof key !== 'string' && key !== undefined) throw new env.global.TypeError('key must be a string or undefined.');
            if (key in symbols) return symbols[key];
            else return symbols[key] = internals.symbol(key);
        },
        keyFor(sym) {
            if (typeof sym !== 'symbol') throw new env.global.TypeError('sym must be a symbol.');
            return internals.symbolToString(sym);
        },

        typeName: env.symbol("Symbol.typeName") as any,
        replace: env.symbol('Symbol.replace') as any,
        match: env.symbol('Symbol.match') as any,
        matchAll: env.symbol('Symbol.matchAll') as any,
        split: env.symbol('Symbol.split') as any,
        search: env.symbol('Symbol.search') as any,
        iterator: env.symbol('Symbol.iterator') as any,
        asyncIterator: env.symbol('Symbol.asyncIterator') as any,
    });

    internals.defineField(env.global.Object.prototype, Symbol.typeName, 'Object', false, false, false);
    internals.defineField(env.global, Symbol.typeName, 'Window', false, false, false);
});