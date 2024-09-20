const target = arguments[0];
const primordials = arguments[1];

const symbol = primordials.symbol || (() => {
    const repo = {};

    return {
        makeSymbol: (name) => { name },
        getSymbol(name) {
            if (name in repo) return repo[name];
            else return repo[name] = { name };
        },
        getSymbolKey(symbol) {
            if (symbol.name in repo && repo[symbol.name] === symbol) return symbol.name;
            else return undefined;
        },
        getSymbolDescription: ({ name }) => name,
    };
});

const number = primordials.number || {
    parseInt() { throw new Error("parseInt not supported"); },
    parseFloat() { throw new Error("parseFloat not supported"); },
    isNaN: (val) => val !== val,
    NaN: 0 / 0,
    Infinity: 1 / 0,
};

const string = primordials.string;

const object = primordials.object || {
    defineProperty() { throw new Error("Define property not polyfillable"); },
    defineField(obj, key, a, b, c, value) { obj[key] = value; },
    getOwnMember() { throw new Error("Get own member not polyfillable"); },
    getOwnSymbolMember() { throw new Error("Get own symbol member not polyfillable"); },
    getOwnMembers() { throw new Error("Get own members not polyfillable"); },
    getOwnSymbolMembers() { throw new Error("Get own symbol members not polyfillable"); },
    getPrototype() { throw new Error("Get prototype not polyfillable"); },
    setPrototype() { throw new Error("Set prototype not polyfillable"); },
}

const invokeType = primordials.function.invokeType;
const setConstructable = primordials.function.setConstructable;
const setCallable = primordials.function.setCallable;
const invoke = primordials.function.invoke;
const construct = primordials.function.construct;

const json = primordials.json;

const setGlobalPrototype = primordials.setGlobalPrototype;
const compile = primordials.compile;
const setIntrinsic = primordials.setIntrinsic;

const valueKey = symbol.makeSymbol("Primitive.value");
const undefined = ({}).definitelyDefined;

target.undefined = undefined;

const unwrapThis = (self, type, constr, name, arg = "this", defaultVal) => {
    if (typeof self === type) return self;
    if (self instanceof constr && valueKey in self) self = self[valueKey];
    if (typeof self === type) return self;
    if (defaultVal !== undefined) return defaultVal;

    throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
}

const wrapIndex = (i, len) => {};

class Symbol {
    get description() {
        return symbol.getSymbolDescriptor(unwrapThis(this, "symbol", Symbol, "Symbol.prototype.description"));
    }
    toString() {
        return "Symbol(" + unwrapThis(this, "symbol", Symbol, "Symbol.prototype.toString").description + ")";
    }
    valueOf() {
        return unwrapThis(this, "symbol", Symbol, "Symbol.prototype.valueOf");
    }

    constructor(name = "") {
        return symbol.makeSymbol(name);
    }

    static for(name) {
        return symbol.getSymbol(name + "");
    }
    static keyFor(value) {
        return symbol.getSymbolKey(unwrapThis(value, "symbol", Symbol, "Symbol.keyFor"));
    }
}

setCallable(Symbol, true);
setConstructable(Symbol, false);

object.defineField(Symbol, "asyncIterator", false, false, false, Symbol("Symbol.asyncIterator"));
object.defineField(Symbol, "iterator", false, false, false, Symbol("Symbol.iterator"));
object.defineField(Symbol, "match", false, false, false, Symbol("Symbol.match"));
object.defineField(Symbol, "matchAll", false, false, false, Symbol("Symbol.matchAll"));
object.defineField(Symbol, "replace", false, false, false, Symbol("Symbol.replace"));
object.defineField(Symbol, "search", false, false, false, Symbol("Symbol.search"));
object.defineField(Symbol, "split", false, false, false, Symbol("Symbol.split"));
object.defineField(Symbol, "toStringTag", false, false, false, Symbol("Symbol.toStringTag"));

Symbol();
target.Symbol = Symbol;

class Number {
    toString() {
        return "" + unwrapThis(this, "number", Number, "Number.prototype.toString");
    }
    valueOf() {
        return unwrapThis(this, "number", Number, "Number.prototype.toString");
    }

    constructor(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return 0;
            else return +value;
        }

        this[valueKey] = target.Number(value);
    }

    static isFinite(value) {
        value = unwrapThis(value, "number", Number, "Number.isFinite", "value", undefined);

        if (value === undefined || value !== value) return false;
        if (value === Infinity || value === -Infinity) return false;

        return true;
    }
    static isInteger(value) {
        value = unwrapThis(value, "number", Number, "Number.isInteger", "value", undefined);
        if (value === undefined) return false;
        return number.parseInt(value) === value;
    }
    static isNaN(value) {
        return number.isNaN(value);
    }
    static isSafeInteger(value) {
        value = unwrapThis(value, "number", Number, "Number.isSafeInteger", "value", undefined);
        if (value === undefined || number.parseInt(value) !== value) return false;
        return value >= -9007199254740991 && value <= 9007199254740991;
    }
    static parseFloat(value) {
        value = 0 + value;
        return number.parseFloat(value);
    }
    static parseInt(value, radix) {
        value = 0 + value;
        radix = +radix;
        if (number.isNaN(radix)) radix = 10;

        return number.parseInt(value, radix);
    }
}

object.defineField(Number, "EPSILON", false, false, false, 2.220446049250313e-16);
object.defineField(Number, "MIN_SAFE_INTEGER", false, false, false, -9007199254740991);
object.defineField(Number, "MAX_SAFE_INTEGER", false, false, false, 9007199254740991);
object.defineField(Number, "POSITIVE_INFINITY", false, false, false, +number.Infinity);
object.defineField(Number, "NEGATIVE_INFINITY", false, false, false, -number.Infinity);
object.defineField(Number, "NaN", false, false, false, number.NaN);
object.defineField(Number, "MAX_VALUE", false, false, false, 1.7976931348623157e+308);
object.defineField(Number, "MIN_VALUE", false, false, false, 5e-324);

setCallable(Number, true);
target.Number = Number;
target.parseInt = Number.parseInt;
target.parseFloat = Number.parseFloat;
target.NaN = Number.NaN;
target.Infinity = Number.POSITIVE_INFINITY;

class String {
    at(index) {
        throw "Not implemented :/";
        return unwrapThis(this, "string", String, "String.prototype.at")[index];
    }
    toString() {
        return unwrapThis(this, "string", String, "String.prototype.toString");
    }
    valueOf() {
        return unwrapThis(this, "string", String, "String.prototype.valueOf");
    }

    constructor(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return "";
            else return value + "";
        }

        this[valueKey] = String(value);
    }

    static fromCharCode() {
        const res = [];
        res[arguments.length] = 0;

        for (let i = 0; i < arguments.length; i++) {
            res[i] = string.fromCharCode(+arguments[i]);
        }

        return string.stringBuild(res);
    }
    static fromCodePoint() {
        const res = [];
        res[arguments.length] = 0;

        for (let i = 0; i < arguments.length; i++) {
            res[i] = string.fromCodePoint(+arguments[i]);
        }

        return string.stringBuild(res);
    }
}

setCallable(String, true);
target.String = String;

class Boolean {
    toString() {
        return "" + unwrapThis(this, "boolean", Boolean, "Boolean.prototype.toString");
    }
    valueOf() {
        return unwrapThis(this, "boolean", Boolean, "Boolean.prototype.valueOf");
    }

    constructor(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return false;
            else return !!value;
        }

        this[valueKey] = Boolean(value);
    }
}

setCallable(Boolean, true);
target.Boolean = Boolean;

class Object {
    toString() {
        print("2");
        if (this !== null && this !== undefined && (Symbol.toStringTag in this)) return "[object " + this[Symbol.toStringTag] + "]";
        else if (typeof this === "number" || this instanceof Number) return "[object Number]";
        else if (typeof this === "symbol" || this instanceof Symbol) return "[object Symbol]";
        else if (typeof this === "string" || this instanceof String) return "[object String]";
        else if (typeof this === "boolean" || this instanceof Boolean) return "[object Boolean]";
        else if (typeof this === "function") return "[object Function]";
        else return "[object Object]";
    }
    valueOf() {
        print("1");
        return this;
    }

    constructor(value) {
        if (typeof value === 'object' && value !== null) return value;

        if (typeof value === 'string') return new String(value);
        if (typeof value === 'number') return new Number(value);
        if (typeof value === 'boolean') return new Boolean(value);
        if (typeof value === 'symbol') {
            const res = {};
            setPrototype(res, Symbol.prototype);
            res[valueKey] = value;
            return res;
        }

        const target = this;
        // TODO: use new.target.prototype as proto
        if (target == null || typeof target !== 'object') target = {};

        this[valueKey] = Object(value);
    }

    static defineProperty(obj, key, desc) {
        if (typeof obj !== "object" || obj === null) {
            print(obj);
            print(typeof obj);
            throw new TypeError("Object.defineProperty called on non-object");
        }
        if (typeof desc !== "object" || desc === null) throw new TypeError("Property description must be an object: " + desc);

        if ("get" in desc || "set" in desc) {
            let get = desc.get, set = desc.set;

            if (get !== undefined && typeof get !== "function") throw new TypeError("Getter must be a function: " + get);
            if (set !== undefined && typeof set !== "function") throw new TypeError("Setter must be a function: " + set);

            if ("value" in desc || "writable" in desc) {
                throw new TypeError("Invalid property descriptor. Cannot both specify accessors and a value or writable attribute");
            }

            if (!object.defineProperty(obj, key, desc.enumerable, desc.configurable, get, set)) {
                throw new TypeError("Cannot redefine property: " + key);
            }
        }
        else if (!object.defineField(obj, key, desc.writable, desc.enumerable, desc.configurable, desc.value)) {
            throw new TypeError("Cannot redefine property: " + key);
        }

        return obj;
    }
}

setCallable(Object, true);
object.setPrototype(Object.prototype, null);
target.Object = Object;

class Function {
    toString() {
        if (this.name !== "") return "function " + this.name + "(...) { ... }";
        else return "function (...) { ... }";
    }

    constructor() {
        const parts = ["(function annonymous("];

        for (let i = 0; i < arguments.length - 1; i++) {
            if (i > 0) parts[parts.length] = ",";
            parts[parts.length] = arguments[i];
        }
        parts[parts.length] = "){\n";
        parts[parts.length] = String(arguments[arguments.length - 1]);
        parts[parts.length] = "\n})";

        const res = compile(string.stringBuild(parts))();
        return res;
    }

    static compile(src = "", { globals = [], wrap = false } = {}) {
        const parts = [];

        if (wrap) parts[parts.length] = "return (function() {\n";
        if (globals.length > 0) {
            parts[parts.length] = "let {";

            for (let i = 0; i < globals.length; i++) {
                if (i > 0) parts[parts.length] = ",";
                parts[parts.length] = globals[i];
            }

            parts[parts.length] = "} = arguments[0];";
        }

        parts[parts.length] = src;
        if (wrap) parts[parts.length] = "\n})(arguments[0])";

        const res = compile(string.stringBuild(parts));
        return res;
    }
}

setCallable(Function, true);
target.Function = Function;

class Array {
    constructor(len) {
        if (arguments.length === 1 && typeof len === "number") {
            const res = [];
            res.length = len;
            return res;
        }
        // TODO: Implement spreading
        else throw new Error("Spreading not implemented");
    }
}

setCallable(Array, true);
target.Array = Array;

class Error {
    toString() {
        let res = this.name || "Error";

        const msg = this.message;
        if (msg) res += ": " + msg;

        return res;
    }

    constructor (msg = "") {
        if (invokeType(arguments) === "call") return new Error(msg);
        this.message = msg + "";
    }
}

object.defineField(Error.prototype, "name", true, false, true, "Error");
object.defineField(Error.prototype, "message", true, false, true, "");
setCallable(Error, true);
target.Error = Error;

class SyntaxError {
    constructor (msg = "") {
        if (invokeType(arguments) === "call") return new SyntaxError(msg);
        this.message = msg + "";
    }
}

object.defineField(SyntaxError.prototype, "name", true, false, true, "SyntaxError");
object.setPrototype(SyntaxError, Error);
object.setPrototype(SyntaxError.prototype, Error.prototype);
setCallable(SyntaxError, true);
target.SyntaxError = SyntaxError;

class TypeError {
    constructor (msg = "") {
        if (invokeType(arguments) === "call") return new TypeError(msg);
        this.message = msg + "";
    }
}

object.defineField(TypeError.prototype, "name", true, false, true, "TypeError");
object.setPrototype(TypeError, Error);
object.setPrototype(TypeError.prototype, Error.prototype);
setCallable(TypeError, true);
target.TypeError = TypeError;

class RangeError {
    constructor (msg = "") {
        if (invokeType(arguments) === "call") return new RangeError(msg);
        this.message = msg + "";
    }
}

object.defineField(RangeError.prototype, "name", true, false, true, "RangeError");
object.setPrototype(RangeError, Error);
object.setPrototype(RangeError.prototype, Error.prototype);
setCallable(RangeError, true);
target.RangeError = RangeError;

setGlobalPrototype("string", String.prototype);
setGlobalPrototype("number", Number.prototype);
setGlobalPrototype("boolean", Boolean.prototype);
setGlobalPrototype("symbol", Symbol.prototype);
setGlobalPrototype("object", Object.prototype);
setGlobalPrototype("array", Array.prototype);
setGlobalPrototype("function", Function.prototype);
setGlobalPrototype("error", Error.prototype);
setGlobalPrototype("syntax", SyntaxError.prototype);
