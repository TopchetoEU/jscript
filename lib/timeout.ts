define("timeout", () => {
    const timeouts: Record<number, () => void> = { };
    const intervals: Record<number, () => void> = { };
    let timeoutI = 0, intervalI = 0;

    env.global.setTimeout = (func, delay, ...args) => {
        if (typeof func !== 'function') throw new env.global.TypeError("func must be a function.");
        delay = (delay ?? 0) - 0;
        const cancelFunc = internals.delay(delay, () => internals.apply(func, undefined, args));
        timeouts[++timeoutI] = cancelFunc;
        return timeoutI;
    };
    env.global.setInterval = (func, delay, ...args) => {
        if (typeof func !== 'function') throw new env.global.TypeError("func must be a function.");
        delay = (delay ?? 0) - 0;

        const i = ++intervalI;
        intervals[i] = internals.delay(delay, callback);

        return i;

        function callback() {
            internals.apply(func, undefined, args);
            intervals[i] = internals.delay(delay!, callback);
        }
    };
    
    env.global.clearTimeout = (id) => {
        const func = timeouts[id];
        if (func) func();
        timeouts[id] = undefined!;
    };
    env.global.clearInterval = (id) => {
        const func = intervals[id];
        if (func) func();
        intervals[id] = undefined!;
    };
});