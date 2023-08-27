define("values/errors", () => {
    var Error = env.global.Error = function Error(msg: string) {
        if (msg === undefined) msg = '';
        else msg += '';
    
        return Object.setPrototypeOf({
            message: msg,
            stack: [] as string[],
        }, Error.prototype);
    } as ErrorConstructor;
    
    Error.prototype = env.internals.err ?? {};
    Error.prototype.name = 'Error';
    setConstr(Error.prototype, Error, env);

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
        setConstr(err.prototype, err as ErrorConstructor, env);
        (err.prototype as any).__proto__ = Error.prototype;
        (err as any).__proto__ = Error;
        env.internals.special(err);

        return err;
    }

    env.global.RangeError = makeError('RangeError', env.internals.range ?? {});
    env.global.TypeError = makeError('TypeError', env.internals.type ?? {});
    env.global.SyntaxError = makeError('SyntaxError', env.internals.syntax ?? {});
});