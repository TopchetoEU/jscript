/** @internal */
define("values/object", () => {
    var Object = env.global.Object = function(arg: any) {
        if (arg === undefined || arg === null) return {};
        else if (typeof arg === 'boolean') return new Boolean(arg);
        else if (typeof arg === 'number') return new Number(arg);
        else if (typeof arg === 'string') return new String(arg);
        return arg;
    } as ObjectConstructor;

    Object.prototype = ({} as any).__proto__ as Object;
    setConstr(Object.prototype, Object as any, env);

    function throwNotObject(obj: any, name: string) {
        if (obj === null || typeof obj !== 'object' && typeof obj !== 'function') {
            throw new TypeError(`Object.${name} may only be used for objects.`);
        }
    }
    function check(obj: any) {
        return typeof obj === 'object' && obj !== null || typeof obj === 'function';
    }

    setProps(Object, env, {
        assign: function(dst, ...src) {
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
                if (!env.internals.defineField(
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
        
                if (!env.internals.defineProp(
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
            onlyString = !!(onlyString ?? true);
            return env.internals.keys(obj, onlyString);
        },
        entries(obj, onlyString) {
            return Object.keys(obj, onlyString).map(v => [ v, (obj as any)[v] ]);
        },
        values(obj, onlyString) {
            return Object.keys(obj, onlyString).map(v => (obj as any)[v]);
        },

        getOwnPropertyDescriptor(obj, key) {
            return env.internals.ownProp(obj, key);
        },
        getOwnPropertyDescriptors(obj) {
            return Object.fromEntries([
                ...Object.getOwnPropertyNames(obj),
                ...Object.getOwnPropertySymbols(obj)
            ].map(v => [ v, Object.getOwnPropertyDescriptor(obj, v) ])) as any;
        },

        getOwnPropertyNames(obj) {
            return env.internals.ownPropKeys(obj, false);
        },
        getOwnPropertySymbols(obj) {
            return env.internals.ownPropKeys(obj, true);
        },
        hasOwn(obj, key) {
            if (Object.getOwnPropertyNames(obj).includes(key)) return true;
            if (Object.getOwnPropertySymbols(obj).includes(key)) return true;
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
            env.internals.preventExtensions(obj);
            return obj;
        },
        seal(obj) {
            throwNotObject(obj, 'seal');
            env.internals.seal(obj);
            return obj;
        },
        freeze(obj) {
            throwNotObject(obj, 'freeze');
            env.internals.freeze(obj);
            return obj;
        },

        isExtensible(obj) {
            if (!check(obj)) return false;
            return env.internals.extensible(obj);
        },
        isSealed(obj) {
            if (!check(obj)) return true;
            if (Object.isExtensible(obj)) return false;
            return Object.getOwnPropertyNames(obj).every(v => !Object.getOwnPropertyDescriptor(obj, v).configurable);
        },
        isFrozen(obj) {
            if (!check(obj)) return true;
            if (Object.isExtensible(obj)) return false;
            return Object.getOwnPropertyNames(obj).every(v => {
                var prop = Object.getOwnPropertyDescriptor(obj, v);
                if ('writable' in prop && prop.writable) return false;
                return !prop.configurable;
            });
        }
    });

    setProps(Object.prototype, env, {
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
});