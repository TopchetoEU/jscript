define("values/object", () => {
    var Object = env.global.Object = function(arg: any) {
        if (arg === undefined || arg === null) return {};
        else if (typeof arg === 'boolean') return new Boolean(arg);
        else if (typeof arg === 'number') return new Number(arg);
        else if (typeof arg === 'string') return new String(arg);
        return arg;
    } as ObjectConstructor;

    env.setProto('object', Object.prototype);
    (Object.prototype as any).__proto__ = null;
    setConstr(Object.prototype, Object as any);

    function throwNotObject(obj: any, name: string) {
        if (obj === null || typeof obj !== 'object' && typeof obj !== 'function') {
            throw new TypeError(`Object.${name} may only be used for objects.`);
        }
    }
    function check(obj: any) {
        return typeof obj === 'object' && obj !== null || typeof obj === 'function';
    }

    setProps(Object, {
        assign(dst, ...src) {
            throwNotObject(dst, 'assign');
            for (let i = 0; i < src.length; i++) {
                const obj = src[i];
                throwNotObject(obj, 'assign');
                for (const key of Object.keys(obj)) {
                    (dst as any)[key] = (obj as any)[key];
                }
            }
            return dst;
        },
        create(obj, props) {
            props ??= {};
            return Object.defineProperties({ __proto__: obj }, props as any) as any;
        },

        defineProperty(obj, key, attrib) {
            throwNotObject(obj, 'defineProperty');
            if (typeof attrib !== 'object') throw new TypeError('Expected attributes to be an object.');

            if ('value' in attrib) {
                if ('get' in attrib || 'set' in attrib) throw new TypeError('Cannot specify a value and accessors for a property.');
                if (!internals.defineField(
                    obj, key,
                    attrib.value,
                    !!attrib.writable,
                    !!attrib.enumerable,
                    !!attrib.configurable
                )) throw new TypeError('Can\'t define property \'' + key + '\'.');
            }
            else {
                if (typeof attrib.get !== 'function' && attrib.get !== undefined) throw new TypeError('Get accessor must be a function.');
                if (typeof attrib.set !== 'function' && attrib.set !== undefined) throw new TypeError('Set accessor must be a function.');

                if (!internals.defineProp(
                    obj, key,
                    attrib.get,
                    attrib.set,
                    !!attrib.enumerable,
                    !!attrib.configurable
                )) throw new TypeError('Can\'t define property \'' + key + '\'.');
            }

            return obj;
        },
        defineProperties(obj, attrib) {
            throwNotObject(obj, 'defineProperties');
            if (typeof attrib !== 'object' && typeof attrib !== 'function') throw 'Expected second argument to be an object.';

            for (var key in attrib) {
                Object.defineProperty(obj, key, attrib[key]);
            }

            return obj;
        },

        keys(obj, onlyString) {
            return internals.keys(obj, !!(onlyString ?? true));
        },
        entries(obj, onlyString) {
            const res = [];
            const keys = internals.keys(obj, !!(onlyString ?? true));

            for (let i = 0; i < keys.length; i++) {
                res[i] = [ keys[i], (obj as any)[keys[i]] ];
            }

            return keys;
        },
        values(obj, onlyString) {
            const res = [];
            const keys = internals.keys(obj, !!(onlyString ?? true));

            for (let i = 0; i < keys.length; i++) {
                res[i] = (obj as any)[keys[i]];
            }

            return keys;
        },

        getOwnPropertyDescriptor(obj, key) {
            return internals.ownProp(obj, key) as any;
        },
        getOwnPropertyDescriptors(obj) {
            const res = [];
            const keys = internals.ownPropKeys(obj);

            for (let i = 0; i < keys.length; i++) {
                res[i] = internals.ownProp(obj, keys[i]);
            }

            return res;
        },

        getOwnPropertyNames(obj) {
            const arr = internals.ownPropKeys(obj);
            const res = [];

            for (let i = 0; i < arr.length; i++) {
                if (typeof arr[i] === 'symbol') continue;
                res[res.length] = arr[i];
            }

            return res as any;
        },
        getOwnPropertySymbols(obj) {
            const arr = internals.ownPropKeys(obj);
            const res = [];

            for (let i = 0; i < arr.length; i++) {
                if (typeof arr[i] !== 'symbol') continue;
                res[res.length] = arr[i];
            }

            return res as any;
        },
        hasOwn(obj, key) {
            const keys = internals.ownPropKeys(obj);

            for (let i = 0; i < keys.length; i++) {
                if (keys[i] === key) return true;
            }

            return false;
        },

        getPrototypeOf(obj) {
            return obj.__proto__;
        },
        setPrototypeOf(obj, proto) {
            (obj as any).__proto__ = proto;
            return obj;
        },

        fromEntries(iterable) {
            const res = {} as any;

            for (const el of iterable) {
                res[el[0]] = el[1];
            }

            return res;
        },

        preventExtensions(obj) {
            throwNotObject(obj, 'preventExtensions');
            internals.lock(obj, 'ext');
            return obj;
        },
        seal(obj) {
            throwNotObject(obj, 'seal');
            internals.lock(obj, 'seal');
            return obj;
        },
        freeze(obj) {
            throwNotObject(obj, 'freeze');
            internals.lock(obj, 'freeze');
            return obj;
        },

        isExtensible(obj) {
            if (!check(obj)) return false;
            return internals.extensible(obj);
        },
        isSealed(obj) {
            if (!check(obj)) return true;
            if (internals.extensible(obj)) return false;
            const keys = internals.ownPropKeys(obj);

            for (let i = 0; i < keys.length; i++) {
                if (internals.ownProp(obj, keys[i]).configurable) return false;
            }

            return true;
        },
        isFrozen(obj) {
            if (!check(obj)) return true;
            if (internals.extensible(obj)) return false;
            const keys = internals.ownPropKeys(obj);

            for (let i = 0; i < keys.length; i++) {
                const prop = internals.ownProp(obj, keys[i]);
                if (prop.configurable) return false;
                if ('writable' in prop && prop.writable) return false;
            }

            return true;
        }
    });

    setProps(Object.prototype, {
        valueOf() {
            return this;
        },
        toString() {
            return '[object ' + (this[Symbol.typeName] ?? 'Unknown') + ']';
        },
        hasOwnProperty(key) {
            return Object.hasOwn(this, key);
        },
    });
    internals.markSpecial(Object);
});