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

return new UnitTest('Array', function() { []; })
    .add(include('length.js'))
    .add(include('reduce.js'))
    .add(include('sparse.js'))
    .add(include('concat.js'))
