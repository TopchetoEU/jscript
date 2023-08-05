interface Error {
    constructor: ErrorConstructor;
    name: string;
    message: string;
    stack: string[];
}
interface ErrorConstructor {
    (msg?: any): Error;
    new (msg?: any): Error;
    prototype: Error;
}

interface TypeErrorConstructor extends ErrorConstructor {
    (msg?: any): TypeError;
    new (msg?: any): TypeError;
    prototype: Error;
}
interface TypeError extends Error {
    constructor: TypeErrorConstructor;
    name: 'TypeError';
}

interface RangeErrorConstructor extends ErrorConstructor {
    (msg?: any): RangeError;
    new (msg?: any): RangeError;
    prototype: Error;
}
interface RangeError extends Error {
    constructor: RangeErrorConstructor;
    name: 'RangeError';
}

interface SyntaxErrorConstructor extends ErrorConstructor {
    (msg?: any): RangeError;
    new (msg?: any): RangeError;
    prototype: Error;
}
interface SyntaxError extends Error {
    constructor: SyntaxErrorConstructor;
    name: 'SyntaxError';
}


declare var Error: ErrorConstructor;
declare var RangeError: RangeErrorConstructor;
declare var TypeError: TypeErrorConstructor;
declare var SyntaxError: SyntaxErrorConstructor;

gt.Error = function Error(msg: string) {
    if (msg === undefined) msg = '';
    else msg += '';

    return Object.setPrototypeOf({
        message: msg,
        stack: [] as string[],
    }, Error.prototype);
} as ErrorConstructor;

Error.prototype = internals.err ?? {};
Error.prototype.name = 'Error';
setConstr(Error.prototype, Error);

Error.prototype.toString = function() {
    if (!(this instanceof Error)) return '';

    if (this.message === '') return this.name;
    else return this.name + ': ' + this.message;
};

function makeError<T extends ErrorConstructor>(name: string, proto: any): T {
    var err = function (msg: string) {
        var res = new Error(msg);
        (res as any).__proto__ = err.prototype;
        return res;
    } as T;

    err.prototype = proto;
    err.prototype.name = name;
    setConstr(err.prototype, err as ErrorConstructor);
    (err.prototype as any).__proto__ = Error.prototype;
    (err as any).__proto__ = Error;
    internals.special(err);

    return err;
}

gt.RangeError = makeError('RangeError', internals.range ?? {});
gt.TypeError = makeError('TypeError', internals.type ?? {});
gt.SyntaxError = makeError('SyntaxError', internals.syntax ?? {});