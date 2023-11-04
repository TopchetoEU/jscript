function assert(cond, msg, locDepth) {
    if (locDepth < 0 || locDepth === undefined) locDepth = 0;
    if (!cond) {
        log('Assert failed', (typeof locDepth === 'string' ? locDepth : Error().stack[locDepth + 1]) + ': ', msg);
    }
}
function assertMatch(expected, actual, depth, msg) {
    if (!match(expected, actual, depth)) {
        log('Assert failed', Error().stack[1] + ': ', msg);
        log('Expected:', expected);
        log('Actual:', actual);
    }
}
function match(expected, actual, depth) {
    if (!Array.isArray(expected) || !Array.isArray(actual)) return expected === actual;
    else if (expected.length !== actual.length) return false;
    else if (depth === undefined || depth < 0) depth = 0;

    for (var i = 0; i < expected.length; i++) {
        if (!(i in expected) || !(i in actual)) return !(i in expected) && !(i in actual);

        if (
            expected[i] === actual[i] ||
            depth > 0 &&
            Array.isArray(expected) &&
            Array.isArray(actual) &&
            match(expected[i], actual[i], depth - 1)
        ) continue;

        return false;
    }

    return true;
}

/** @class */ function UnitTest(msg, exec) {
    this.name = msg;
    this.exec = exec;
    this.subtests = [];
}

UnitTest.prototype.run = function(path) {
    if (path === undefined) path = [];

    path.push(this.name);

    if (typeof this.exec === 'function') {
        var res = true, err = 'exec() returned false.';
        try {
            if (this.exec() === false) res = false;
        }
        catch (e) { res = false; err = e; }
        assert(res, path.join('/') + ': ' + err, this.exec.location());
    }
    for (var i = 0; i < this.subtests.length; i++) {
        this.subtests[i].run(path);
    }

    path.pop();
}
UnitTest.prototype.add = function(test, exec) {
    if (test instanceof UnitTest) this.subtests.push(test);
    else this.subtests.push(new UnitTest(test, exec));
    return this;
}

include('arithmetics/index.js').run();
include('array/index.js').run();

log('Tests complete.');