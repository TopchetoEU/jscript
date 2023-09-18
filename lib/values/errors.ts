define("values/errors", () => {
    var Error = env.global.Error = function Error(msg: string) {
        if (msg === undefined) msg = '';
        else msg += '';

        return {
            message: msg,
            stack: [] as string[],
            __proto__: Error.prototype,
        } as any;
    } as ErrorConstructor;

    setConstr(Error.prototype, Error);
    setProps(Error.prototype, {
        name: 'Error',
        toString: internals.setEnv(function(this: Error) {
            if (!(this instanceof Error)) return '';
        
            if (this.message === '') return this.name;
            else return this.name + ': ' + this.message;
        }, env)
    });
    env.setProto('error', Error.prototype);
    internals.markSpecial(Error);

    function makeError<T1 extends ErrorConstructor>(name: string, proto: string): T1 {
        function constr (msg: string) {
            var res = new Error(msg);
            (res as any).__proto__ = constr.prototype;
            return res;
        }

        (constr as any).__proto__ = Error;
        (constr.prototype as any).__proto__ = env.proto('error');
        setConstr(constr.prototype, constr as ErrorConstructor);
        setProps(constr.prototype, { name: name });

        internals.markSpecial(constr);
        env.setProto(proto, constr.prototype);
        
        return constr as T1;
    }
    
    env.global.RangeError = makeError('RangeError', 'rangeErr');
    env.global.TypeError = makeError('TypeError', 'typeErr');
    env.global.SyntaxError = makeError('SyntaxError', 'syntaxErr');
});