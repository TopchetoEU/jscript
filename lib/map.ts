define("map", () => {
    const syms = { values: internals.symbol('Map.values') } as { readonly values: unique symbol };
    const Object = env.global.Object;

    class Map<KeyT, ValueT> {
        [syms.values]: any = {};

        public [env.global.Symbol.iterator](): IterableIterator<[KeyT, ValueT]> {
            return this.entries();
        }

        public clear() {
            this[syms.values] = {};
        }
        public delete(key: KeyT) {
            if ((key as any) in this[syms.values]) {
                delete this[syms.values];
                return true;
            }
            else return false;
        }

        public entries(): IterableIterator<[KeyT, ValueT]> {
            const keys = internals.ownPropKeys(this[syms.values]);
            let i = 0;

            return {
                next: () => {
                    if (i >= keys.length) return { done: true };
                    else return { done: false, value: [ keys[i], this[syms.values][keys[i++]] ] }
                },
                [env.global.Symbol.iterator]() { return this; }
            }
        }
        public keys(): IterableIterator<KeyT> {
            const keys = internals.ownPropKeys(this[syms.values]);
            let i = 0;

            return {
                next: () => {
                    if (i >= keys.length) return { done: true };
                    else return { done: false, value: keys[i] }
                },
                [env.global.Symbol.iterator]() { return this; }
            }
        }
        public values(): IterableIterator<ValueT> {
            const keys = internals.ownPropKeys(this[syms.values]);
            let i = 0;

            return {
                next: () => {
                    if (i >= keys.length) return { done: true };
                    else return { done: false, value: this[syms.values][keys[i++]] }
                },
                [env.global.Symbol.iterator]() { return this; }
            }
        }

        public get(key: KeyT) {
            return this[syms.values][key];
        }
        public set(key: KeyT, val: ValueT) {
            this[syms.values][key] = val;
            return this;
        }
        public has(key: KeyT) {
            return (key as any) in this[syms.values][key];
        }

        public get size() {
            return internals.ownPropKeys(this[syms.values]).length;
        }

        public forEach(func: (key: KeyT, val: ValueT, map: Map<KeyT, ValueT>) => void, thisArg?: any) {
            const keys = internals.ownPropKeys(this[syms.values]);

            for (let i = 0; i < keys.length; i++) {
                func(keys[i], this[syms.values][keys[i]], this);
            }
        }

        public constructor(iterable: Iterable<[KeyT, ValueT]>) {
            const it = iterable[env.global.Symbol.iterator]();

            for (let el = it.next(); !el.done; el = it.next()) {
                this[syms.values][el.value[0]] = el.value[1];
            }
        }
    }

    env.global.Map = Map;
});
