define("set", () => {
    const syms = { values: internals.symbol('Map.values') } as { readonly values: unique symbol };
    const Object = env.global.Object;

    class Set<T> {
        [syms.values]: any = {};

        public [env.global.Symbol.iterator](): IterableIterator<[T, T]> {
            return this.entries();
        }

        public clear() {
            this[syms.values] = {};
        }
        public delete(key: T) {
            if ((key as any) in this[syms.values]) {
                delete this[syms.values];
                return true;
            }
            else return false;
        }

        public entries(): IterableIterator<[T, T]> {
            const keys = internals.ownPropKeys(this[syms.values]);
            let i = 0;

            return {
                next: () => {
                    if (i >= keys.length) return { done: true };
                    else return { done: false, value: [ keys[i], keys[i] ] }
                },
                [env.global.Symbol.iterator]() { return this; }
            }
        }
        public keys(): IterableIterator<T> {
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
        public values(): IterableIterator<T> {
            return this.keys();
        }

        public add(val: T) {
            this[syms.values][val] = undefined;
            return this;
        }
        public has(key: T) {
            return (key as any) in this[syms.values][key];
        }

        public get size() {
            return internals.ownPropKeys(this[syms.values]).length;
        }

        public forEach(func: (key: T, val: T, map: Set<T>) => void, thisArg?: any) {
            const keys = internals.ownPropKeys(this[syms.values]);

            for (let i = 0; i < keys.length; i++) {
                func(keys[i], this[syms.values][keys[i]], this);
            }
        }

        public constructor(iterable: Iterable<T>) {
            const it = iterable[env.global.Symbol.iterator]();

            for (let el = it.next(); !el.done; el = it.next()) {
                this[syms.values][el.value] = undefined;
            }
        }
    }

    env.global.Set = Set;
});
