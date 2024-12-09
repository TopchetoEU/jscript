(function main() {
	function extend(derived, base) {
		if (base == null) {
			object.setPrototype(derived.prototype, null);
		}
		else {
			object.setPrototype(derived, base);
			object.setPrototype(derived.prototype, base.prototype);
		}
	}
	
	var target = arguments[0];
	var primordials = arguments[1];
	var symbol = primordials.symbol || (function () {
		var repo = {};
		return {
			makeSymbol: function (name) { return { name: name }; },
			getSymbol: function (name) {
				if (name in repo) return repo[name];
				else return repo[name] = { name: name };
			},
			getSymbolKey: function (symbol) {
				if (symbol.name in repo && repo[symbol.name] === symbol) return symbol.name;
				else return undefined;
			},
			getSymbolDescription: function (symbol) {
				return symbol.name;
			}
		};
	});
	var number = primordials.number || {
		parseInt: function () { throw new Error("parseInt not supported"); },
		parseFloat: function () { throw new Error("parseFloat not supported"); },
		isNaN: function (val) { return val !== val; },
		NaN: 0 / 0,
		Infinity: 1 / 0,
	};
	var string = primordials.string;
	var object = primordials.object || {
		defineProperty: function () { throw new Error("Define property not polyfillable"); },
		defineField: function (obj, key, a, b, c, value) { obj[key] = value; },
		getOwnMember: function () { throw new Error("Get own member not polyfillable"); },
		getOwnSymbolMember: function () { throw new Error("Get own symbol member not polyfillable"); },
		getOwnMembers: function () { throw new Error("Get own members not polyfillable"); },
		getOwnSymbolMembers: function () { throw new Error("Get own symbol members not polyfillable"); },
		getPrototype: function () { throw new Error("Get prototype not polyfillable"); },
		setPrototype: function () { throw new Error("Set prototype not polyfillable"); },
	};
	var func = primordials.function || {
		invokeType: function (args, self) {
			if (typeof self === "object") return "new";
			else return "call";
		},
		setConstructable: function () { throw new Error("Set constructable not polyfillable"); },
		setCallable: function () { throw new Error("Set callable not polyfillable"); },
		invoke: function () { throw new Error("Invoke not polyfillable"); },
		construct: function () { throw new Error("Construct not polyfillable"); },
	};
	var json = primordials.json || {
		stringify: function (val) { throw new Error("JSON stringify not polyfillable"); },
		parse: function (val) { throw new Error("JSON parse not polyfillable"); },
	}
	
	var setGlobalPrototypes = primordials.setGlobalPrototypes;
	var compile = primordials.compile;
	var valueKey = symbol.makeSymbol("Primitive.value");
	var undefined = void 0;
	target.undefined = undefined;
	
	function unwrapThis(self, type, constr, name, arg, defaultVal) {
		if (arg === void 0) { arg = "this"; }
		if (typeof self === type)
			return self;
		if (self instanceof constr && valueKey in self)
			self = self[valueKey];
		if (typeof self === type)
			return self;
		if (defaultVal !== undefined)
			return defaultVal;
		throw new TypeError(name + " requires that '" + arg + "' be a " + constr.name);
	}
	function wrapIndex(i, len) { }
	function Symbol(name) {
		if (name === undefined) name = "";
		return symbol.makeSymbol(name);
	}
	Symbol.prototype.toString = function () {
		return "Symbol(" + unwrapThis(this, "symbol", Symbol, "Symbol.prototype.toString").description + ")";
	};
	Symbol.prototype.valueOf = function () {
		return unwrapThis(this, "symbol", Symbol, "Symbol.prototype.valueOf");
	};
	Symbol.for = function (name) {
		return symbol.getSymbol(name + "");
	};
	Symbol.keyFor = function (value) {
		return symbol.getSymbolKey(unwrapThis(value, "symbol", Symbol, "Symbol.keyFor"));
	};
	object.defineProperty(Symbol.prototype, "desc", false, true, function () {
		return symbol.getSymbolDescriptor(unwrapThis(this, "symbol", Symbol, "Symbol.prototype.description"));
	});
	object.defineField(Symbol, "asyncIterator", false, false, false, Symbol("Symbol.asyncIterator"));
	object.defineField(Symbol, "iterator", false, false, false, Symbol("Symbol.iterator"));
	object.defineField(Symbol, "match", false, false, false, Symbol("Symbol.match"));
	object.defineField(Symbol, "matchAll", false, false, false, Symbol("Symbol.matchAll"));
	object.defineField(Symbol, "replace", false, false, false, Symbol("Symbol.replace"));
	object.defineField(Symbol, "search", false, false, false, Symbol("Symbol.search"));
	object.defineField(Symbol, "split", false, false, false, Symbol("Symbol.split"));
	object.defineField(Symbol, "toStringTag", false, false, false, Symbol("Symbol.toStringTag"));
	func.setConstructable(Symbol, false);
	target.Symbol = Symbol;

	function Number(value) {
		if (func.invokeType(arguments, this) === "call") {
			if (arguments.length === 0) return 0;
			else return +value;
		}
		this[valueKey] = target.Number(value);
	}
	Number.prototype.toString = function () {
		return "" + unwrapThis(this, "number", Number, "Number.prototype.toString");
	};
	Number.prototype.valueOf = function () {
		return unwrapThis(this, "number", Number, "Number.prototype.toString");
	};
	Number.isFinite = function (value) {
		value = unwrapThis(value, "number", Number, "Number.isFinite", "value", undefined);
		if (value === undefined || value !== value)
			return false;
		if (value === Infinity || value === -Infinity)
			return false;
		return true;
	};
	Number.isInteger = function (value) {
		value = unwrapThis(value, "number", Number, "Number.isInteger", "value", undefined);
		if (value === undefined)
			return false;
		return number.parseInt(value) === value;
	};
	Number.isNaN = function (value) {
		return number.isNaN(value);
	};
	Number.isSafeInteger = function (value) {
		value = unwrapThis(value, "number", Number, "Number.isSafeInteger", "value", undefined);
		if (value === undefined || number.parseInt(value) !== value)
			return false;
		return value >= -9007199254740991 && value <= 9007199254740991;
	};
	Number.parseFloat = function (value) {
		value = 0 + value;
		return number.parseFloat(value);
	};
	Number.parseInt = function (value, radix) {
		value = 0 + value;
		radix = +radix;
		if (number.isNaN(radix))
			radix = 10;
		return number.parseInt(value, radix);
	};
	
	object.defineField(Number, "EPSILON", false, false, false, 2.220446049250313e-16);
	object.defineField(Number, "MIN_SAFE_INTEGER", false, false, false, -9007199254740991);
	object.defineField(Number, "MAX_SAFE_INTEGER", false, false, false, 9007199254740991);
	object.defineField(Number, "POSITIVE_INFINITY", false, false, false, +number.Infinity);
	object.defineField(Number, "NEGATIVE_INFINITY", false, false, false, -number.Infinity);
	object.defineField(Number, "NaN", false, false, false, number.NaN);
	object.defineField(Number, "MAX_VALUE", false, false, false, 1.7976931348623157e+308);
	object.defineField(Number, "MIN_VALUE", false, false, false, 5e-324);
	func.setCallable(Number, true);
	target.Number = Number;
	target.parseInt = Number.parseInt;
	target.parseFloat = Number.parseFloat;
	target.NaN = Number.NaN;
	target.Infinity = Number.POSITIVE_INFINITY;
	
	function String(value) {
		if (func.invokeType(arguments, this) === "call") {
			if (arguments.length === 0)
				return "";
			else
				return value + "";
		}
		this[valueKey] = String(value);
	}
	String.prototype.at = function (index) {
		throw "Not implemented :/";
		return unwrapThis(this, "string", String, "String.prototype.at")[index];
	};
	String.prototype.toString = function () {
		return unwrapThis(this, "string", String, "String.prototype.toString");
	};
	String.prototype.valueOf = function () {
		return unwrapThis(this, "string", String, "String.prototype.valueOf");
	};
	String.fromCharCode = function () {
		var res = [];
		res[arguments.length] = 0;
		for (var i = 0; i < arguments.length; i++) {
			res[i] = string.fromCharCode(+arguments[i]);
		}
		return string.stringBuild(res);
	};
	String.fromCodePoint = function () {
		var res = [];
		res[arguments.length] = 0;
	
		for (var i = 0; i < arguments.length; i++) {
			res[i] = string.fromCodePoint(+arguments[i]);
		}
		return string.stringBuild(res);
	};
	func.setCallable(String, true);
	target.String = String;
	
	function Boolean(value) {
		if (func.invokeType(arguments, this) === "call") {
			if (arguments.length === 0) return false;
			else return !!value;
		}
		this[valueKey] = Boolean(value);
	}
	Boolean.prototype.toString = function () {
		return "" + unwrapThis(this, "boolean", Boolean, "Boolean.prototype.toString");
	};
	Boolean.prototype.valueOf = function () {
		return unwrapThis(this, "boolean", Boolean, "Boolean.prototype.valueOf");
	};
	func.setCallable(Boolean, true);
	target.Boolean = Boolean;
	
	function Object(value) {
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
	
		return {};
		// // TODO: use new.target.prototype as proto
		// if (target == null || typeof target !== 'object') target = {};
		// return target;
	}
	Object.prototype.toString = function () {
		if (this !== null && this !== undefined && (Symbol.toStringTag in this)) return "[object " + this[Symbol.toStringTag] + "]";
		else if (typeof this === "number" || this instanceof Number) return "[object Number]";
		else if (typeof this === "symbol" || this instanceof Symbol) return "[object Symbol]";
		else if (typeof this === "string" || this instanceof String) return "[object String]";
		else if (typeof this === "boolean" || this instanceof Boolean) return "[object Boolean]";
		else if (typeof this === "function") return "[object Function]";
		else return "[object Object]";
	};
	Object.prototype.valueOf = function () {
		return this;
	};
	Object.defineProperty = function (obj, key, desc) {
		if (typeof obj !== "object" || obj === null) throw new TypeError("Object.defineProperty called on non-object");
		if (typeof desc !== "object" || desc === null) throw new TypeError("Property description must be an object: " + desc);
		if ("get" in desc || "set" in desc) {
			var get = desc.get, set = desc.set;
	
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
	};
	func.setCallable(Object, true);
	extend(Object, null);
	object.setPrototype(Object.prototype, null);
	target.Object = Object;
	
	function Function() {
		var parts = ["(function annonymous("];
		for (var i = 0; i < arguments.length - 1; i++) {
			if (i > 0)
				parts[parts.length] = ",";
			parts[parts.length] = arguments[i];
		}
		parts[parts.length] = "){\n";
		parts[parts.length] = String(arguments[arguments.length - 1]);
		parts[parts.length] = "\n})";
		var res = compile(string.stringBuild(parts))();
		return res;
	}
	Function.prototype.toString = function () {
		if (this.name !== "")
			return "function " + this.name + "(...) { ... }";
		else
			return "function (...) { ... }";
	};
	Function.compile = function (src, opts) {
		if (src === void 0) src = "";
		if (opts === void 0) opts = {};
		if (opts.globals === void 0) opts.globals = [];
		if (opts.wrap === void 0) opts.wrap = false;
	
		var globals = opts.globals;
		var wrap = opts.wrap;
		var parts = [];
	
		if (wrap) parts[parts.length] = "return (function() {\n";
		if (globals.length > 0) {
			parts[parts.length] = "let {";
			for (var i = 0; i < globals.length; i++) {
				if (i > 0) parts[parts.length] = ",";
				parts[parts.length] = globals[i];
			}
			parts[parts.length] = "} = arguments[0];";
		}
		parts[parts.length] = src;
		if (wrap) parts[parts.length] = "\n})(arguments[0])";
	
		var res = compile(string.stringBuild(parts));
		return res;
	};
	func.setCallable(Function, true);
	target.Function = Function;
	
	function Array(len) {
		if (arguments.length === 1 && typeof len === "number") {
			var res = [];
			res.length = len;
			return res;
		}
		// TODO: Implement spreading
		else throw new Error("Spreading not implemented");
	}
	func.setCallable(Array, true);
	target.Array = Array;

	function Error(msg) {
		if (msg === void 0) { msg = ""; }
		if (func.invokeType(arguments, this) === "call")
			return new Error(msg);
		this.message = msg + "";
	}
	Error.prototype.toString = function () {
		var res = this.name || "Error";
		var msg = this.message;
		if (msg)
			res += ": " + msg;
		return res;
	};
	object.defineField(Error.prototype, "name", true, false, true, "Error");
	object.defineField(Error.prototype, "message", true, false, true, "");
	func.setCallable(Error, true);
	target.Error = Error;

	extend(SyntaxError, Error);
	function SyntaxError(msg) {
		if (func.invokeType(arguments, this) === "call")
			return new SyntaxError(msg);
		return _super.call(this, msg) || this;
	}
	object.defineField(SyntaxError.prototype, "name", true, false, true, "SyntaxError");
	func.setCallable(SyntaxError, true);
	target.SyntaxError = SyntaxError;

	extend(TypeError, Error);
	function TypeError(msg) {
		if (func.invokeType(arguments, this) === "call")
			return new TypeError(msg);
		return _super.call(this, msg) || this;
	}
	object.defineField(TypeError.prototype, "name", true, false, true, "TypeError");
	func.setCallable(TypeError, true);
	target.TypeError = TypeError;

	extend(RangeError, Error);
	function RangeError(msg) {
		if (func.invokeType(arguments, this) === "call")
			return new RangeError(msg);
		return _super.call(this, msg) || this;
	}
	object.defineField(RangeError.prototype, "name", true, false, true, "RangeError");
	func.setCallable(RangeError, true);
	target.RangeError = RangeError;

	target.uint8 = primordials.uint8;

	setGlobalPrototypes({
		string: String.prototype,
		number: Number.prototype,
		boolean: Boolean.prototype,
		symbol: Symbol.prototype,
		object: Object.prototype,
		array: Array.prototype,
		function: Function.prototype,
		error: Error.prototype,
		syntax: SyntaxError.prototype,
		range: RangeError.prototype,
		type: TypeError.prototype,
	});
})(arguments[0], arguments[1]);
