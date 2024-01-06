return new UnitTest('pop', function() { return typeof Array.prototype.pop === 'function'; })
    .add('simple pop', function() {
        var arr = [1, 2, 3];
        return match(3, arr.pop())
    })
    .add('pop from empty', function() {
        return match(undefined, [].pop())
    })