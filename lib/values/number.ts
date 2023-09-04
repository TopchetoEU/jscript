define("values/number", () => {
    var Number = env.global.Number = function(this: Number | undefined, arg: any) {
        var val;
        if (arguments.length === 0) val = 0;
        else val = arg - 0;
        if (this === undefined || this === null) return val;
        else (this as any).value = val;
    } as NumberConstructor;

    env.setProto('number', Number.prototype);
    setConstr(Number.prototype, Number);

    setProps(Number.prototype, {
        valueOf() {
            if (typeof this === 'number') return this;
            else return (this as any).value;
        },
        toString() {
            if (typeof this === 'number') return this + '';
            else return (this as any).value + '';
        }
    });

    setProps(Number, {
        parseInt(val) { return Math.trunc(val as any - 0); },
        parseFloat(val) { return val as any - 0; },
    });

    env.global.parseInt = Number.parseInt;
    env.global.parseFloat = Number.parseFloat;
    env.global.Object.defineProperty(env.global, 'NaN', { value: 0 / 0, writable: false });
    env.global.Object.defineProperty(env.global, 'Infinity', { value: 1 / 0, writable: false });
});