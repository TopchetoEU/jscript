var a = { id: 10, name: 'test1' };
var b = { id: 15, name: 'test2' };
var c = { id: 20, name: 'test3' };

return new UnitTest('find', function() { return typeof Array.prototype.find === 'function'; })
    .add('simple', function() {
        return [ a, b, c ].find(function (v) { return v.id === 15; }) === b;
    })
    .add('sparse', function() {
        var n = 0;
        [ a, b,,,, c ].find(function (v) { n++; return v === undefined; });
        return n === 3;
    })
    .add('no occurence', function() {
        return [ a, b, c ].find(function (v) { return v.id === 30 }) === undefined;
    })
    .add('pass this', function() {
        return [ a, b ].find(function (v) { return this === c; }, c) === a;
    })