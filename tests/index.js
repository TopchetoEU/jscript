function assert(cond, msg, locDepth) {
    if (locDepth < 0 || locDepth === undefined) locDepth = 0;
    if (!cond) {
        log('Assert failed', (typeof locDepth === 'string' ? locDepth : Error().stack[locDepth + 1]) + ': ', msg);
        return false;
    }
    return true;
}
function assertMatch(expected, actual, depth, msg) {
    if (!match(expected, actual, depth)) {
        log('Assert failed', Error().stack[1] + ': ', msg);
        log('Expected:', expected);
        log('Actual:', actual);
        return false;
    }
    return true;
}
function match(expected, actual, depth) {
    if (!Array.isArray(expected) || !Array.isArray(actual)) return expected === actual;
    else if (expected.length !== actual.length) return false;
    else if (depth === undefined) depth = Infinity;
    else if (depth < 0) depth = 0;

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
    var res = true;

    if (typeof this.exec === 'function') {
        var res = true, err = 'exec() returned false.';
        try {
            if (this.exec() === false) res = false;
        }
        catch (e) { res = false; err = e; }
        res &= assert(res, path.join('/') + ': ' + err, this.exec.location());
    }
    for (var i = 0; i < this.subtests.length; i++) {
        res &= this.subtests[i].run(path);
    }

    path.pop();

    return res;
}
UnitTest.prototype.add = function(test, exec) {
    if (test instanceof UnitTest) this.subtests.push(test);
    else this.subtests.push(new UnitTest(test, exec));
    return this;
}

return function() {
    if (
        require('arithmetics/index.js').run() &&
        require('array/index.js').run()
    ) log('All tests passed!');
    exit();
}
