define("values/symbol", () => {
    var Symbol = env.global.Symbol = function(this: any, val?: string) {
        if (this !== undefined && this !== null) throw new TypeError("Symbol may not be called with 'new'.");
        if (typeof val !== 'string' && val !== undefined) throw new TypeError('val must be a string or undefined.');
        return env.internals.symbol(val, true);
    } as SymbolConstructor;

    Symbol.prototype = env.internals.symbolProto;
    setConstr(Symbol.prototype, Symbol, env);
    (Symbol as any).typeName = Symbol("Symbol.name");
    (Symbol as any).replace = Symbol('Symbol.replace');
    (Symbol as any).match = Symbol('Symbol.match');
    (Symbol as any).matchAll = Symbol('Symbol.matchAll');
    (Symbol as any).split = Symbol('Symbol.split');
    (Symbol as any).search = Symbol('Symbol.search');
    (Symbol as any).iterator = Symbol('Symbol.iterator');
    (Symbol as any).asyncIterator = Symbol('Symbol.asyncIterator');

    setProps(Symbol, env, {
        for(key) {
            if (typeof key !== 'string' && key !== undefined) throw new TypeError('key must be a string or undefined.');
            return env.internals.symbol(key, false);
        },
        keyFor(sym) {
            if (typeof sym !== 'symbol') throw new TypeError('sym must be a symbol.');
            return env.internals.symStr(sym);
        },
        typeName: Symbol('Symbol.name') as any,
    });

    env.global.Object.defineProperty(Object.prototype, Symbol.typeName, { value: 'Object' });
    env.global.Object.defineProperty(env.global, Symbol.typeName, { value: 'Window' });
});