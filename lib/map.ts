define("map", () => {
    var Map = env.global.Map = env.internals.Map;

    Map.prototype[Symbol.iterator] = function() {
        return this.entries();
    };

    var entries = Map.prototype.entries;
    var keys = Map.prototype.keys;
    var values = Map.prototype.values;

    Map.prototype.entries = function() {
        var it = entries.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Map.prototype.keys = function() {
        var it = keys.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };
    Map.prototype.values = function() {
        var it = values.call(this);
        it[Symbol.iterator] = () => it;
        return it;
    };

    env.global.Map = Map;
});
