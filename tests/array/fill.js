return new UnitTest('fill', function() { return typeof Array.prototype.push === 'function'; })
    .add('simple fill', function() {
        return match([5, 5, 5, 5, 5], [1, 2, 3, 4, 5].fill(5))
    })
    .add('fill empty', function() {
        return match([], [].fill(5))
    })
    .add('fill from', function() {
        return match([1, 'a', 'a', 'a', 'a'], [1, 2, 3, 4, 5].fill('a', 1))
    })
    .add('fill range', function() {
        return match([1, 'a', 'a', 'a', 5], [1, 2, 3, 4, 5].fill('a', 1, 4))
    })
    .add('fill wrap', function() {
        return match([1, 'a', 'a', 4, 5], [1, 2, 3, 4, 5].fill('a', 1, -2))
    })
    .add('fill out of range', function() {
        return match([1, 2, 'a', 'a', 'a'], [1, 2, 3, 4, 5].fill('a', 2, 8))
    })