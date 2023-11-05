return new UnitTest('concat', function() { return typeof Array.prototype.concat === 'function'; })
    .add('two arrays', function() { return match([1, 2, 3], [1].concat([2], [3])) })
    .add('simple spread', function() { return match([1, 2, 3, 4, 5], [1].concat([2], 3, [4, 5])) })
    .add('sparse concat', function() { return match([1,, 2,,, 3,,, 4, 5], [1,,2].concat([,,3,,,4], 5)) })