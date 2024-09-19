return;

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

const number = primordials.number || (() => {
    return {
        parseInt() { throw new Error("parseInt not supported"); },
        parseFloat() { throw new Error("parseFloat not supported"); },
        isNaN: (val) => val !== val,
        NaN: 0 / 0,
        Infinity: 1 / 0,
    };
});

const fromCharCode = primordials.string.fromCharCode;
const fromCodePoint = primordials.string.fromCodePoint;
const stringBuild = primordials.string.stringBuild;

const defineProperty = primordials.object.defineProperty;
const defineField = primordials.object.defineField;
const getOwnMember = primordials.object.getMember;
const getOwnSymbolMember = primordials.object.getOwnSymbolMember;
const getOwnMembers = primordials.object.getOwnMembers;
const getOwnSymbolMembers = primordials.object.getOwnSymbolMembers;
const getPrototype = primordials.object.getPrototype;
const setPrototype = primordials.object.setPrototype;

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

const unwrapThis = (self, type, constr, name, arg, defaultVal) => {
    if (arg == null) arg = "this";
    if (typeof self === type) return self;
    if (self instanceof constr && valueKey in self) self = self[valueKey];
    if (typeof self === type) return self;
    if (defaultVal !== undefined) return defaultVal;

    throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
}

const wrapIndex = (i, len) => {};

const Symbol = (name = "") => symbol.makeSymbol(name);

defineField(Symbol, "for", true, false, true, function(name) {
    return symbol.getSymbol(name + "");
});
defineField(Symbol, "keyFor", true, false, true, function(value) {
    return symbol.getSymbolKey(unwrapThis(value, "symbol", Symbol, "Symbol.keyFor"));
});

defineField(Symbol, "asyncIterator", false, false, false, Symbol("Symbol.asyncIterator"));
defineField(Symbol, "iterator", false, false, false, Symbol("Symbol.iterator"));
defineField(Symbol, "match", false, false, false, Symbol("Symbol.match"));
defineField(Symbol, "matchAll", false, false, false, Symbol("Symbol.matchAll"));
defineField(Symbol, "replace", false, false, false, Symbol("Symbol.replace"));
defineField(Symbol, "search", false, false, false, Symbol("Symbol.search"));
defineField(Symbol, "split", false, false, false, Symbol("Symbol.split"));
defineField(Symbol, "toStringTag", false, false, false, Symbol("Symbol.toStringTag"));
defineField(Symbol, "prototype", false, false, false, {});

defineProperty(Symbol.prototype, "description", false, true, function () {
    return symbol.getSymbolDescription(unwrapThis(this, "symbol", Symbol, "Symbol.prototype.description"));
}, undefined);
defineField(Symbol.prototype, "toString", true, false, true, function() {
    return "Symbol(" + unwrapThis(this, "symbol", Symbol, "Symbol.prototype.toString").description + ")";
});
defineField(Symbol.prototype, "valueOf", true, false, true, function() {
    return unwrapThis(this, "symbol", Symbol, "Symbol.prototype.valueOf");
});

target.Symbol = Symbol;

const Number = function(value) {
    if (invokeType(arguments) === "call") {
        if (arguments.length === 0) return 0;
        else return +value;
    }

    this[valueKey] = target.Number(value);
};

defineField(Number, "isFinite", true, false, true, function(value) {
    value = unwrapThis(value, "number", Number, "Number.isFinite", "value", undefined);

    if (value === undefined || value !== value) return false;
    if (value === Infinity || value === -Infinity) return false;

    return true;
});
defineField(Number, "isInteger", true, false, true, function(value) {
    value = unwrapThis(value, "number", Number, "Number.isInteger", "value", undefined);
    if (value === undefined) return false;
    return number.parseInt(value) === value;
});
defineField(Number, "isNaN", true, false, true, function(value) {
    return number.isNaN(value);
});
defineField(Number, "isSafeInteger", true, false, true, function(value) {
    value = unwrapThis(value, "number", Number, "Number.isSafeInteger", "value", undefined);
    if (value === undefined || number.parseInt(value) !== value) return false;
    return value >= -9007199254740991 && value <= 9007199254740991;
});
defineField(Number, "parseFloat", true, false, true, function(value) {
    value = 0 + value;
    return number.parseFloat(value);
});
defineField(Number, "parseInt", true, false, true, function(value, radix) {
    value = 0 + value;
    radix = +radix;
    if (number.isNaN(radix)) radix = 10;

    return number.parseInt(value, radix);
});

defineField(Number, "EPSILON", false, false, false, 2.220446049250313e-16);
defineField(Number, "MIN_SAFE_INTEGER", false, false, false, -9007199254740991);
defineField(Number, "MAX_SAFE_INTEGER", false, false, false, 9007199254740991);
defineField(Number, "POSITIVE_INFINITY", false, false, false, +number.Infinity);
defineField(Number, "NEGATIVE_INFINITY", false, false, false, -number.Infinity);
defineField(Number, "NaN", false, false, false, number.NaN);
defineField(Number, "MAX_VALUE", false, false, false, 1.7976931348623157e+308);
defineField(Number, "MIN_VALUE", false, false, false, 5e-324);
defineField(Number, "prototype", false, false, false, {});

defineField(Number.prototype, "toString", true, false, true);   
defineField(Number.prototype, "toString", true, false, true, function() {
    return "" + unwrapThis(this, "number", Number, "Number.prototype.toString");
});
defineField(Number.prototype, "valueOf", true, false, true, function() {
    return unwrapThis(this, "number", Number, "Number.prototype.toString");
});

target.Number = Number;
target.parseInt = Number.parseInt;
target.parseFloat = Number.parseFloat;
target.NaN = Number.NaN;
target.Infinity = Number.POSITIVE_INFINITY;

const String = function(value) {
    if (invokeType(arguments) === "call") {
        if (arguments.length === 0) return "";
        else return value + "";
    }

    this[valueKey] = String(value);
};

defineField(String, "fromCharCode", true, false, true, function() {
    const res = [];
    res[arguments.length] = 0;

    for (let i = 0; i < arguments.length; i++) {
        res[i] = fromCharCode(+arguments[i]);
    }

    return stringBuild(res);
});
defineField(String, "fromCodePoint", true, false, true, function() {
    const res = [];
    res[arguments.length] = 0;

    for (let i = 0; i < arguments.length; i++) {
        res[i] = fromCodePoint(+arguments[i]);
    }

    return stringBuild(res);
});

defineField(String, "prototype", false, false, false, {});

defineField(String.prototype, "at", true, false, true, function(index) {
    throw "Not implemented :/";
    return unwrapThis(this, "string", String, "String.prototype.at")[index];
});
defineField(String.prototype, "toString", true, false, true, function() {
    return unwrapThis(this, "string", String, "String.prototype.toString");
});
defineField(String.prototype, "valueOf", true, false, true, function() {
    return unwrapThis(this, "string", String, "String.prototype.valueOf");
});

target.String = String;

const Boolean = function(value) {
    if (invokeType(arguments) === "call") {
        if (arguments.length === 0) return false;
        else return !!value;
    }

    this[valueKey] = Boolean(value);
};

defineField(Boolean, "prototype", false, false, false, {});

defineField(Boolean.prototype, "toString", true, false, true, function() {
    return "" + unwrapThis(this, "boolean", Boolean, "Boolean.prototype.toString");
});
defineField(Boolean.prototype, "valueOf", true, false, true, function() {
    return unwrapThis(this, "boolean", Boolean, "Boolean.prototype.valueOf");
});

target.Boolean = Boolean;

const Object = function(value) {
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
    if (target == null || typeof target !== 'object') target = {};

    this[valueKey] = Object(value);
};

defineField(Object, "prototype", false, false, false, setPrototype({}, null));

defineField(Object, "defineProperty", true, false, true, (obj, key, desc) => {
    if (typeof obj !== "object" || obj === null) {
        print(obj);
        print(typeof obj);
        throw new TypeError("Object.defineProperty called on non-object");
    }
    if (typeof desc !== "object" || desc === null) throw new TypeError("Property description must be an object: " + desc);

    if ("get" in desc || "set" in desc) {
        let get = desc.get, set = desc.set;

        print(typeof get);

        if (get !== undefined && typeof get !== "function") throw new TypeError("Getter must be a function: " + get);
        if (set !== undefined && typeof set !== "function") throw new TypeError("Setter must be a function: " + set);

        if ("value" in desc || "writable" in desc) {
            throw new TypeError("Invalid property descriptor. Cannot both specify accessors and a value or writable attribute");
        }

        if (!defineProperty(obj, key, desc.enumerable, desc.configurable, get, set)) {
            throw new TypeError("Cannot redefine property: " + key);
        }
    }
    else if (!defineField(obj, key, desc.writable, desc.enumerable, desc.configurable, desc.value)) {
        throw new TypeError("Cannot redefine property: " + key);
    }

    return obj;
});

defineField(Object.prototype, "toString", true, false, true, function() {
    if (this !== null && this !== undefined && (Symbol.toStringTag in this)) return "[object " + this[Symbol.toStringTag] + "]";
    else if (typeof this === "number" || this instanceof Number) return "[object Number]";
    else if (typeof this === "symbol" || this instanceof Symbol) return "[object Symbol]";
    else if (typeof this === "string" || this instanceof String) return "[object String]";
    else if (typeof this === "boolean" || this instanceof Boolean) return "[object Boolean]";
    else if (typeof this === "function") return "[object Function]";
    else return "[object Object]";
});
defineField(Object.prototype, "valueOf", true, false, true, function() {
    return this;
});

target.Object = Object;

const Function = function() {
    const parts = ["return function annonymous("];

    for (let i = 0; i < arguments.length - 1; i++) {
        if (i > 0) parts[parts.length] = ",";
        parts[parts.length] = arguments[i];
    }
    parts[parts.length] = "){\n";
    parts[parts.length] = String(arguments[arguments.length - 1]);
    parts[parts.length] = "\n}";

    const res = compile(stringBuild(parts))();
    return res;
};

defineField(Function, "compile", true, false, true, (src = "", options = {}) => {
    if (options.globals == null) options.globals = [];
    if (options.wrap == null) options.wrap = true;

    const parts = [];

    if (options.wrap) parts[parts.length] = "return (function() {\n";
    if (options.globals.length > 0) {
        parts[parts.length] = "var ";

        for (let i = 0; i < options.globals.length; i++) {
            if (i > 0) parts[parts.length] = ",";
            parts[parts.length] = options.globals[i];
        }

        parts[parts.length] = ";((g=arguments[0])=>{";

        for (let i = 0; i < options.globals.length; i++) {
            const name = options.globals[i];
            parts[parts.length] = name + "=g[" + json.stringify(name) + "];";
        }

        parts[parts.length] = "})()\n";
    }

    parts[parts.length] = src;
    if (options.wrap) parts[parts.length] = "\n})(arguments[0])";

    const res = compile(stringBuild(parts));
    return res;
});
defineField(Function, "prototype", false, false, false, setPrototype({}, null));

defineField(Function.prototype, "toString", true, false, true, function() {
    if (this.name !== "") return "function " + this.name + "(...) { ... }";
    else return "function (...) { ... }";
});
defineField(Function.prototype, "valueOf", true, false, true, function() {
    return this;
});

target.Function = Function;

// setIntrinsic("spread_obj", target.spread_obj = (target, obj) => {
//     if (obj === null || obj === undefined) return;
//     const members = getOwnMembers(obj, true);
//     const symbols = getOwnSymbolMembers(obj, true);

//     for (let i = 0; i < members.length; i++) {
//         const member = members[i];
//         target[member] = obj[member];
//     }

//     for (let i = 0; i < symbols.length; i++) {
//         const member = symbols[i];
//         target[member] = obj[member];
//     }
// });
// setIntrinsic("apply", target.spread_call = (func, self, args) => {
//     return invoke(func, self, args);
// });
// setIntrinsic("apply", target.spread_new = (func, args) => {
//     return invoke(func, null, args);
// });

const Error = function(msg = "") {
    if (invokeType(arguments) === "call") return new Error(msg);
    this.message = msg + "";
};
defineField(Error.prototype, "name", true, false, true, "Error");
defineField(Error.prototype, "message", true, false, true, "");
defineField(Error.prototype, "toString", true, false, true, function toString() {
    let res = this.name || "Error";

    const msg = this.message;
    if (msg) res += ": " + msg;

    return res;
});

target.Error = Error;

const SyntaxError = function(msg = "") {
    if (invokeType(arguments) === "call") return new SyntaxError(msg);
    this.message = msg + "";
};
defineField(SyntaxError.prototype, "name", true, false, true, "SyntaxError");

setPrototype(SyntaxError, Error);
setPrototype(SyntaxError.prototype, Error.prototype);

target.SyntaxError = SyntaxError;

setGlobalPrototype("string", String.prototype);
setGlobalPrototype("number", Number.prototype);
setGlobalPrototype("boolean", Boolean.prototype);
setGlobalPrototype("symbol", Symbol.prototype);
setGlobalPrototype("object", Object.prototype);
setGlobalPrototype("function", Function.prototype);
setGlobalPrototype("error", Error.prototype);
setGlobalPrototype("syntax", SyntaxError.prototype);
