var env: Environment;

// @ts-ignore
return (_env: Environment) => {
    env = _env;
    env.global.assert = (cond, msg) => {
        try {
            if (!cond()) throw 'condition not satisfied';
            log('Passed ' + msg);
            return true;
        }
        catch (e) {
            log('Failed ' + msg + ' because of: ' + e);
            return false;
        }
    }
    try {
        run('values/object');
        run('values/symbol');
        run('values/function');
        run('values/errors');
        run('values/string');
        run('values/number');
        run('values/boolean');
        run('values/array');

        env.internals.special(Object, Function, Error, Array);

        env.global.setTimeout = (func, delay, ...args) => {
            if (typeof func !== 'function') throw new TypeError("func must be a function.");
            delay = (delay ?? 0) - 0;
            return env.internals.setTimeout(() => func(...args), delay)
        };
        env.global.setInterval = (func, delay, ...args) => {
            if (typeof func !== 'function') throw new TypeError("func must be a function.");
            delay = (delay ?? 0) - 0;
            return env.internals.setInterval(() => func(...args), delay)
        };
        
        env.global.clearTimeout = (id) => {
            id = id | 0;
            env.internals.clearTimeout(id);
        };
        env.global.clearInterval = (id) => {
            id = id | 0;
            env.internals.clearInterval(id);
        };
    
        run('promise');
        run('map');
        run('set');
        run('regex');
        run('require');
        
        log('Loaded polyfills!');
    }
    catch (e: any) {
        if (!_env.captureErr) throw e;
        var err = 'Uncaught error while loading polyfills: ';
        if (typeof Error !== 'undefined' && e instanceof Error && e.toString !== {}.toString) err += e;
        else if ('message' in e) {
            if ('name' in e) err += e.name + ": " + e.message;
            else err += 'Error: ' + e.message;
        }
        else err += e;
        log(e);
    }
};