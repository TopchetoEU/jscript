return new UnitTest('constructor', function() { return typeof Array === 'function'; })
    .add('no args', function () { return match(new Array(), []); })
    .add('length', function () {
        var res = new Array(3);
        return res.length === 3 &&
            !(0 in res) &&
            !(1 in res) &&
            !(2 in res);
    })
    .add('elements', function () {
        return match(new Array(1, 2, 3), [1, 2, 3]);
    })
