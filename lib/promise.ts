define("promise", () => {
    (env.global.Promise = env.internals.Promise).prototype[Symbol.typeName] = 'Promise';
});
