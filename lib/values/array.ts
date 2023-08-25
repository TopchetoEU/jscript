// god this is awful
type FlatArray<Arr, Depth extends number> = {
    "done": Arr,
    "recur": Arr extends Array<infer InnerArr>
        ? FlatArray<InnerArr, [-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20][Depth]>
        : Arr
}[Depth extends -1 ? "done" : "recur"];

interface Array<T> {
    [i: number]: T;

    constructor: ArrayConstructor;
    length: number;

    toString(): string;
    // toLocaleString(): string;
    join(separator?: string): string;
    fill(val: T, start?: number, end?: number): T[];
    pop(): T | undefined;
    push(...items: T[]): number;
    concat(...items: (T | T[])[]): T[];
    concat(...items: (T | T[])[]): T[];
    join(separator?: string): string;
    reverse(): T[];
    shift(): T | undefined;
    slice(start?: number, end?: number): T[];
    sort(compareFn?: (a: T, b: T) => number): this;
    splice(start: number, deleteCount?: number | undefined, ...items: T[]): T[];
    unshift(...items: T[]): number;
    indexOf(searchElement: T, fromIndex?: number): number;
    lastIndexOf(searchElement: T, fromIndex?: number): number;
    every(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): boolean;
    some(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): boolean;
    forEach(callbackfn: (value: T, index: number, array: T[]) => void, thisArg?: any): void;
    includes(el: any, start?: number): boolean;

    map<U>(callbackfn: (value: T, index: number, array: T[]) => U, thisArg?: any): U[];
    filter(predicate: (value: T, index: number, array: T[]) => unknown, thisArg?: any): T[];
    find(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): T[];
    findIndex(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): number;
    findLast(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): T[];
    findLastIndex(predicate: (value: T, index: number, array: T[]) => boolean, thisArg?: any): number;

    flat<D extends number = 1>(depth?: D): FlatArray<T, D>;
    flatMap(func: (val: T, i: number, arr: T[]) => T | T[], thisAarg?: any): FlatArray<T[], 1>;
    sort(func?: (a: T, b: T) => number): this;

    reduce(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T): T;
    reduce(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T, initialValue: T): T;
    reduce<U>(callbackfn: (previousValue: U, currentValue: T, currentIndex: number, array: T[]) => U, initialValue: U): U;
    reduceRight(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T): T;
    reduceRight(callbackfn: (previousValue: T, currentValue: T, currentIndex: number, array: T[]) => T, initialValue: T): T;
    reduceRight<U>(callbackfn: (previousValue: U, currentValue: T, currentIndex: number, array: T[]) => U, initialValue: U): U;
}
interface ArrayConstructor {
    new <T>(arrayLength?: number): T[];
    new <T>(...items: T[]): T[];
    <T>(arrayLength?: number): T[];
    <T>(...items: T[]): T[];
    isArray(arg: any): arg is any[];
    prototype: Array<any>;
}

declare var Array: ArrayConstructor;

gt.Array = function(len?: number) {
    var res = [];

    if (typeof len === 'number' && arguments.length === 1) {
        if (len < 0) throw 'Invalid array length.';
        res.length = len;
    }
    else {
        for (var i = 0; i < arguments.length; i++) {
            res[i] = arguments[i];
        }
    }

    return res;
} as ArrayConstructor;

Array.prototype = ([] as any).__proto__ as Array<any>;
setConstr(Array.prototype, Array);

function wrapI(max: number, i: number) {
    i |= 0;
    if (i < 0) i = max + i;
    return i;
}
function clampI(max: number, i: number) {
    if (i < 0) i = 0;
    if (i > max) i = max;
    return i;
}

lgt.wrapI = wrapI;
lgt.clampI = clampI;

(Array.prototype as any)[Symbol.typeName] = "Array";

setProps(Array.prototype, {
    concat() {
        var res = [] as any[];
        res.push.apply(res, this);

        for (var i = 0; i < arguments.length; i++) {
            var arg = arguments[i];
            if (arg instanceof Array) {
                res.push.apply(res, arg);
            }
            else {
                res.push(arg);
            }
        }

        return res;
    },
    every(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument not a function.");
        func = func.bind(thisArg);

        for (var i = 0; i < this.length; i++) {
            if (!func(this[i], i, this)) return false;
        }

        return true;
    },
    some(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument not a function.");
        func = func.bind(thisArg);

        for (var i = 0; i < this.length; i++) {
            if (func(this[i], i, this)) return true;
        }

        return false;
    },
    fill(val, start, end) {
        if (arguments.length < 3) end = this.length;
        if (arguments.length < 2) start = 0;

        start = clampI(this.length, wrapI(this.length + 1, start ?? 0));
        end = clampI(this.length, wrapI(this.length + 1, end ?? this.length));

        for (; start < end; start++) {
            this[start] = val;
        }

        return this;
    },
    filter(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");

        var res = [];
        for (var i = 0; i < this.length; i++) {
            if (i in this && func.call(thisArg, this[i], i, this)) res.push(this[i]);
        }
        return res;
    },
    find(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");

        for (var i = 0; i < this.length; i++) {
            if (i in this && func.call(thisArg, this[i], i, this)) return this[i];
        }

        return undefined;
    },
    findIndex(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");

        for (var i = 0; i < this.length; i++) {
            if (i in this && func.call(thisArg, this[i], i, this)) return i;
        }

        return -1;
    },
    findLast(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");
    
        for (var i = this.length - 1; i >= 0; i--) {
            if (i in this && func.call(thisArg, this[i], i, this)) return this[i];
        }
    
        return undefined;
    },
    findLastIndex(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");
    
        for (var i = this.length - 1; i >= 0; i--) {
            if (i in this && func.call(thisArg, this[i], i, this)) return i;
        }
    
        return -1;
    },
    flat(depth) {
        var res = [] as any[];
        var buff = [];
        res.push(...this);
    
        for (var i = 0; i < (depth ?? 1); i++) {
            var anyArrays = false;
            for (var el of res) {
                if (el instanceof Array) {
                    buff.push(...el);
                    anyArrays = true;
                }
                else buff.push(el);
            }
    
            res = buff;
            buff = [];
            if (!anyArrays) break;
        }
    
        return res;
    },
    flatMap(func, th) {
        return this.map(func, th).flat();
    },
    forEach(func, thisArg) {
        for (var i = 0; i < this.length; i++) {
            if (i in this) func.call(thisArg, this[i], i, this);
        }
    },
    map(func, thisArg) {
        if (typeof func !== 'function') throw new TypeError("Given argument is not a function.");
    
        var res = [];
        for (var i = 0; i < this.length; i++) {
            if (i in this) res[i] = func.call(thisArg, this[i], i, this);
        }
        return res;
    },
    pop() {
        if (this.length === 0) return undefined;
        var val = this[this.length - 1];
        this.length--;
        return val;
    },
    push() {
        for (var i = 0; i < arguments.length; i++) {
            this[this.length] = arguments[i];
        }
        return arguments.length;
    },
    shift() {
        if (this.length === 0) return undefined;
        var res = this[0];

        for (var i = 0; i < this.length - 1; i++) {
            this[i] = this[i + 1];
        }

        this.length--;

        return res;
    },
    unshift() {
        for (var i = this.length - 1; i >= 0; i--) {
            this[i + arguments.length] = this[i];
        }
        for (var i = 0; i < arguments.length; i++) {
            this[i] = arguments[i];
        }

        return arguments.length;
    },
    slice(start, end) {
        start = clampI(this.length, wrapI(this.length + 1, start ?? 0));
        end = clampI(this.length, wrapI(this.length + 1, end ?? this.length));

        var res: any[] = [];
        var n = end - start;
        if (n <= 0) return res;
    
        for (var i = 0; i < n; i++) {
            res[i] = this[start + i];
        }
    
        return res;
    },
    toString() {
        let res = '';
        for (let i = 0; i < this.length; i++) {
            if (i > 0) res += ',';
            if (i in this && this[i] !== undefined && this[i] !== null) res += this[i];
        }
    
        return res;
    },
    indexOf(el, start) {
        start = start! | 0;
        for (var i = Math.max(0, start); i < this.length; i++) {
            if (i in this && this[i] == el) return i;
        }
    
        return -1;
    },
    lastIndexOf(el, start) {
        start = start! | 0;
        for (var i = this.length; i >= start; i--) {
            if (i in this && this[i] == el) return i;
        }

        return -1;
    },
    includes(el, start) {
        return this.indexOf(el, start) >= 0;
    },
    join(val = ',') {
        let res = '', first = true;

        for (let i = 0; i < this.length; i++) {
            if (!(i in this)) continue;
            if (!first) res += val;
            first = false;
            res += this[i];
        }
        return res;
    },
    sort(func) {
        func ??= (a, b) => {
            const _a = a + '';
            const _b = b + '';

            if (_a > _b) return 1;
            if (_a < _b) return -1;
            return 0;
        };

        if (typeof func !== 'function') throw new TypeError('Expected func to be undefined or a function.');

        internals.sort(this, func);
        return this;
    },
    splice(start, deleteCount, ...items) {
        start = clampI(this.length, wrapI(this.length, start ?? 0));
        deleteCount = (deleteCount ?? Infinity | 0);
        if (start + deleteCount >= this.length) deleteCount = this.length - start;

        const res = this.slice(start, start + deleteCount);
        const moveN = items.length - deleteCount;
        const len = this.length;

        if (moveN < 0) {
            for (let i = start - moveN; i < len; i++) {
                this[i + moveN] = this[i];
            }
        }
        else if (moveN > 0) {
            for (let i = len - 1; i >= start; i--) {
                this[i + moveN] = this[i];
            }
        }

        for (let i = 0; i < items.length; i++) {
            this[i + start] = items[i];
        }

        this.length = len + moveN;

        return res;
    }
});

setProps(Array, {
    isArray(val: any) { return (val instanceof Array); }
});
