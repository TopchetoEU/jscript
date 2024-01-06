return new UnitTest('push', function() { return typeof Array.prototype.push === 'function'; })
    .add('simple push', function() {
        var arr = [];
        arr.push(1, 2, 3);
        return match(arr, [1, 2, 3])
    })
    .add('push array', function() {
        var arr = [];
        arr.push([1, 2, 3]);
        return match(arr, [[1, 2, 3]])
    })
    .add('push as concat', function() {
        var arr = [1, 2, 3];
        arr.push(4, 5, 6);
        return match(arr, [1, 2, 3, 4, 5, 6])
    })