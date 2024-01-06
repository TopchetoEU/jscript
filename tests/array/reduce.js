function runIterator(arr, method, func, n) {
    var res = [];
    var j = 1;
    var args = [ function() {
        var pushed = [];
        for (var i = 0; i < n; i++) pushed[i] = arguments[i];
        res[j++] = pushed;
        return func.apply(this, arguments);
    } ];

    for (var i = 4; i < arguments.length; i++) args[i - 3] = arguments[i];

    res[0] = method.apply(arr, args);

    return res;
}

return function(name) {
    var func = Array.prototype[name];
    return new UnitTest(name, function () { return typeof func === 'function' })
        .add('empty function', function () {match(
            [ undefined, [4, 3, 2], [undefined, 2, 1], [undefined, 1, 0], ],
            runIterator([1, 2, 3, 4], func, function() { }, 3), 1
        )})
        .add('adder', function () {match(
            [ 10, [4, 3, 2], [7, 2, 1], [9, 1, 0], ],
            runIterator([1, 2, 3, 4], func, function(a, b) { return a + b; }, 3), 1
        )})
        .add('sparse array', function () {match(
            [ 10, [4, 3, 11], [7, 2, 7], [9, 1, 3], ],
            runIterator([,,,1,,,, 2,,,, 3,,,, 4,,,,], func, function(a, b) { return a + b }, 3), 1
        )})
        .add('sparse array with one element', function () {match(
            [ 1 ],
            runIterator([,,,1,,,,], func, function(v) { return v; }, 3), 1
        )})
        .add('sparse array with no elements', function () {match(
            [ undefined ],
            runIterator([,,,,,,,], func, function(v) { return v; }, 3), 1
        )})

        .add('initial value and empty function', function () {match(
            [ undefined, [0, 4, 3], [undefined, 3, 2], [undefined, 2, 1], [undefined, 1, 0] ],
            runIterator([1, 2, 3, 4], func, function() { }, 3, 0), 1
        )})
        .add('initial value and adder', function () {match(
            [ 15, [5, 4, 3], [9, 3, 2], [12, 2, 1], [14, 1, 0] ],
            runIterator([1, 2, 3, 4], func, function(a, b) { return a + b; }, 3, 5), 1
        )})
        .add('initial value, sparce array and adder', function () {match(
            [ 15, [5, 4, 15], [9, 3, 11], [12, 2, 7], [14, 1, 3] ],
            runIterator([,,,1,,,, 2,,,, 3,,,, 4,,,,], func, function(a, b) { return a + b; }, 3, 5), 1
        )})
        .add('initial value and sparse array with one element', function () {match(
            [ 6, [5, 1, 3] ],
            runIterator([,,,1,,,,], func, function(a, b) { return a + b; }, 3, 5), 1
        )})
        .add('initial value and sparse array with no elements', function () {match(
            [ 5 ],
            runIterator([,,,,,,,], func, function(v) { return v; }, 3, 5), 1
        )});
}