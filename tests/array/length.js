return new UnitTest('length & capacity', function() { return 'length' in Array.prototype; })
    .add('empty literal', function() { return [].length === 0 })
    .add('filled literal', function() { return [1, 2, 3].length === 3 })
    .add('set length', function() {
        var a = [];
        a.length = 10;
        return a.length === 10;
    })
    .add('length after set', function() {
        var a = [];
        a[5] = 5;
        return a.length === 6;
    })
    .add('length after set (big)', function() {
        var a = [1, 2];
        a[5000] = 5;
        return a.length === 5001;
    })
    .add('expand test', function() {
        var a = [];
        for (var i = 0; i < 1000; i++) {
            a[i] = i * 50;
            if (a[i] !== i * 50) return false;
        }
        return a.length === 1000;
    })