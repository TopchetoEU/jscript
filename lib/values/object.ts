define("values/object", () => {
    var Object = env.global.Object = internals.object;
    (Object.prototype as any).__proto__ = null;
    env.setProto('object', Object.prototype);
});