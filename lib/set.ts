define("set", () => {
    var Set = env.global.Set = env.internals.Set;
    Set.prototype[Symbol.iterator] = function() {
        return this.values();
    };

    var entries = Set.prototype.entries;
    var keys = Set.prototype.keys;
    var values = Set.prototype.values;

    Set.prototype.entries = function() {
        var it = entries.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Set.prototype.keys = function() {
        var it = keys.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Set.prototype.values = function() {
        var it = values.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
});
