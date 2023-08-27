define("values/boolean", () => {
    var Boolean = env.global.Boolean = function (this: Boolean | undefined, arg) {
        var val;
        if (arguments.length === 0) val = false;
        else val = !!arg;
        if (this === undefined || this === null) return val;
        else (this as any).value = val;
    } as BooleanConstructor;
    
    Boolean.prototype = (false as any).__proto__ as Boolean;
    setConstr(Boolean.prototype, Boolean, env);
});
