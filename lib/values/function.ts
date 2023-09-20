define("values/function", () => {
    var Function = env.global.Function = internals.function;
    env.setProto('function', Function.prototype);
    setConstr(Function.prototype, Function);
});