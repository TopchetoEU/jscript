(function(target, primordials) {
    var makeSymbol = primordials.symbol.makeSymbol;
    var getSymbol = primordials.symbol.getSymbol;
    var getSymbolKey = primordials.symbol.getSymbolKey;
    var getSymbolDescription = primordials.symbol.getSymbolDescription;

    var parseInt = primordials.number.parseInt;
    var parseFloat = primordials.number.parseFloat;
    var isNaN = primordials.number.isNaN;
    var NaN = primordials.number.NaN;
    var Infinity = primordials.number.Infinity;

    var fromCharCode = primordials.string.fromCharCode;
    var fromCodePoint = primordials.string.fromCodePoint;
    var stringBuild = primordials.string.stringBuild;

    var defineProperty = primordials.object.defineProperty;
    var defineField = primordials.object.defineField;
    var getOwnMember = primordials.object.getMember;
    var getOwnSymbolMember = primordials.object.getOwnSymbolMember;
    var getOwnMembers = primordials.object.getOwnMembers;
    var getOwnSymbolMembers = primordials.object.getOwnSymbolMembers;
    var getPrototype = primordials.object.getPrototype;
    var setPrototype = primordials.object.setPrototype;

    var invokeType = primordials.function.invokeType;
    var setConstructable = primordials.function.setConstructable;
    var setCallable = primordials.function.setCallable;
    var invoke = primordials.function.invoke;

    var setGlobalPrototype = primordials.setGlobalPrototype;
    var compile = primordials.compile;

    var json = primordials.json;

    var valueKey = makeSymbol("Primitive.value");

    function unwrapThis(self, type, constr, name, arg, defaultVal) {
        if (arg == null) arg = "this";
        if (typeof self === type) return self;
        if (self instanceof constr && valueKey in self) self = self[valueKey];
        if (typeof self === type) return self;
        if (arguments.length > 5) return defaultVal;

        throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
    }

    function wrapIndex(i, len) {
    }

    var Symbol = function(name) {
        if (arguments.length === 0) return makeSymbol("");
        else return makeSymbol(name + "");
    };
    setConstructable(Symbol, false);

    defineField(Symbol, "for", true, false, true, function(name) {
        return getSymbol(name + "");
    });
    defineField(Symbol, "keyFor", true, false, true, function(symbol) {
        return getSymbolKey(unwrapThis(symbol, "symbol", Symbol, "Symbol.keyFor"));
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
        return getSymbolDescription(unwrapThis(this, "symbol", Symbol, "Symbol.prototype.description"));
    }, undefined);
    defineField(Symbol.prototype, "toString", true, false, true, function() {
        return "Symbol(" + unwrapThis(this, "symbol", Symbol, "Symbol.prototype.toString").description + ")";
    });
    defineField(Symbol.prototype, "valueOf", true, false, true, function() {
        return unwrapThis(this, "symbol", Symbol, "Symbol.prototype.valueOf");
    });

    target.Symbol = Symbol;

    var Number = function(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return 0;
            else return +value;
        }

        this[valueKey] = target.Number(value);
    };

    defineField(Number, "isFinite", true, false, true, function(value) {
        value = unwrapThis(value, "number", Number, "Number.isFinite", "value", undefined);

        if (value === undefined || isNaN(value)) return false;
        if (value === Infinity || value === -Infinity) return false;

        return true;
    });
    defineField(Number, "isInteger", true, false, true, function(value) {
        value = unwrapThis(value, "number", Number, "Number.isInteger", "value", undefined);
        if (value === undefined) return false;
        return parseInt(value) === value;
    });
    defineField(Number, "isNaN", true, false, true, function(value) {
        return isNaN(value);
    });
    defineField(Number, "isSafeInteger", true, false, true, function(value) {
        value = unwrapThis(value, "number", Number, "Number.isSafeInteger", "value", undefined);
        if (value === undefined || parseInt(value) !== value) return false;
        return value >= -9007199254740991 && value <= 9007199254740991;
    });
    defineField(Number, "parseFloat", true, false, true, function(value) {
        value = 0 + value;
        return parseFloat(value);
    });
    defineField(Number, "parseInt", true, false, true, function(value, radix) {
        value = 0 + value;
        radix = +radix;
        if (isNaN(radix)) radix = 10;

        return parseInt(value, radix);
    });

    defineField(Number, "EPSILON", false, false, false, 2.220446049250313e-16);
    defineField(Number, "MIN_SAFE_INTEGER", false, false, false, -9007199254740991);
    defineField(Number, "MAX_SAFE_INTEGER", false, false, false, 9007199254740991);
    defineField(Number, "POSITIVE_INFINITY", false, false, false, +Infinity);
    defineField(Number, "NEGATIVE_INFINITY", false, false, false, -Infinity);
    defineField(Number, "NaN", false, false, false, NaN);
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

    var String = function(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return "";
            else return value + "";
        }

        this[valueKey] = target.String(value);
    };

    defineField(String, "fromCharCode", true, false, true, function() {
        var res = [];
        res[arguments.length] = 0;

        for (var i = 0; i < arguments.length; i++) {
            res[res.length] = fromCharCode(+arguments[i]);
        }

        return stringBuild(res);
    });
    defineField(String, "fromCodePoint", true, false, true, function(value) {
        var res = [];
        res[arguments.length] = 0;

        for (var i = 0; i < arguments.length; i++) {
            res[res.length] = fromCodePoint(+arguments[i]);
        }

        return stringBuild(res);
    });

    defineField(String, "prototype", false, false, false, {});

    defineField(String.prototype, "at", true, false, true, function(index) {
        return "" + unwrapThis(this, "string", String, "String.prototype.at");
    });
    defineField(String.prototype, "toString", true, false, true, function() {
        return unwrapThis(this, "string", String, "String.prototype.toString");
    });
    defineField(String.prototype, "valueOf", true, false, true, function() {
        return unwrapThis(this, "string", String, "String.prototype.valueOf");
    });

    target.String = String;

    var Boolean = function(value) {
        if (invokeType(arguments) === "call") {
            if (arguments.length === 0) return false;
            else return !!value;
        }

        this[valueKey] = target.Boolean(value);
    };

    defineField(Boolean, "prototype", false, false, false, {});

    defineField(Boolean.prototype, "toString", true, false, true, function() {
        return "" + unwrapThis(this, "boolean", Boolean, "Boolean.prototype.toString");
    });
    defineField(Boolean.prototype, "valueOf", true, false, true, function() {
        return unwrapThis(this, "boolean", Boolean, "Boolean.prototype.valueOf");
    });

    target.Boolean = Boolean;

    var Object = function(value) {
        if (typeof value === 'object' && value !== null) return value;

        if (typeof value === 'string') return new String(value);
        if (typeof value === 'number') return new Number(value);
        if (typeof value === 'boolean') return new Boolean(value);
        if (typeof value === 'symbol') {
            var res = {};
            setPrototype(res, Symbol.prototype);
            res[valueKey] = value;
            return res;
        }

        var target = this;
        if (target === undefined || target === null || typeof target !== 'object') target = {};

        this[valueKey] = target.Object(value);
    };

    defineField(Object, "prototype", false, false, false, setPrototype({}, null));

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

    target.Boolean = Boolean;

    var Function = function() {
        if (invokeType(arguments) === "new") return Function(value);

        var res = ["return function ("];

        for (var i = 0; i < arguments.length - 1; i++) {
            if (i > 0) res[res.length] = ",";
            res[res.length] = arguments[i];
        }
        res[res.length] = "){";
        res[res.length] = String(arguments[arguments.length - 1]);
        res[res.length] = "}";

        log(res);

        return compile(stringBuild(res))();
    };

    defineField(Function, "compile", true, false, true, function(src, options) {
        if (options == null) options = {};
        if (src == null) src = "";

        if (options.globals == null) options.globals = [];
        if (options.wrap == null) options.wrap = true;

        var res = [];

        if (options.wrap) res[res.length] = "return (function() {\n";
        if (options.globals.length > 0) {
            res[res.length] = "var ";

            for (var i = 0; i < options.globals.length; i++) {
                if (i > 0) res[res.length] = ",";
                res[res.length] = options.globals[i];
            }

            res[res.length] = ";(function(g){";

            for (var i = 0; i < options.globals.length; i++) {
                var name = options.globals[i];
                res[res.length] = name;
                res[res.length] = "=g[";
                res[res.length] = json.stringify(name);
                res[res.length] = "];";
            }

            res[res.length] = "})(arguments[0] || {});\n";
        }

        res[res.length] = src;
        if (options.wrap) res[res.length] = "\n})(arguments[0])";

        return compile(stringBuild(res));
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

    setGlobalPrototype("string", String.prototype);
    setGlobalPrototype("number", Number.prototype);
    setGlobalPrototype("boolean", Boolean.prototype);
    setGlobalPrototype("symbol", Symbol.prototype);
    setGlobalPrototype("object", Object.prototype);
})(arguments[0], arguments[1]);