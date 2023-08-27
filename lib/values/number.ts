define("values/number", () => {
    var Number = env.global.Number = function(this: Number | undefined, arg: any) {
        var val;
        if (arguments.length === 0) val = 0;
        else val = arg - 0;
        if (this === undefined || this === null) return val;
        else (this as any).value = val;
    } as NumberConstructor;

    Number.prototype = (0 as any).__proto__ as Number;
    setConstr(Number.prototype, Number, env);

    setProps(Number.prototype, env, {
        valueOf() {
            if (typeof this === 'number') return this;
            else return (this as any).value;
        },
        toString() {
            if (typeof this === 'number') return this + '';
            else return (this as any).value + '';
        }
    });

    setProps(Number, env, {
        parseInt(val) { return Math.trunc(Number.parseFloat(val)); },
        parseFloat(val) { return env.internals.parseFloat(val); },
    });

    env.global.Object.defineProperty(env.global, 'parseInt', { value: Number.parseInt, writable: false });
    env.global.Object.defineProperty(env.global, 'parseFloat', { value: Number.parseFloat, writable: false });
    env.global.Object.defineProperty(env.global, 'NaN', { value: 0 / 0, writable: false });
    env.global.Object.defineProperty(env.global, 'Infinity', { value: 1 / 0, writable: false });
});