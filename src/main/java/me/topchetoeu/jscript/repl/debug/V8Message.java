package me.topchetoeu.jscript.repl.debug;

import java.util.Map;

import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.json.JSONMap;

public class V8Message {
    public final String name;
    public final int id;
    public final JSONMap params;

    public V8Message(String name, int id, Map<String, JSONElement> params) {
        this.name = name;
        this.params = new JSONMap(params);
        this.id = id;
    }
    public V8Result respond(JSONMap result) {
        return new V8Result(id, result);
    }
    public V8Result respond() {
        return new V8Result(id, new JSONMap());
    }

    public V8Message(JSONMap raw) {
        if (!raw.isNumber("id")) throw new IllegalArgumentException("Expected number property 'id'.");
        if (!raw.isString("method")) throw new IllegalArgumentException("Expected string property 'method'.");

        this.name = raw.string("method");
        this.id = (int)raw.number("id");
        this.params = raw.contains("params") ? raw.map("params") : new JSONMap();
    }
    public V8Message(String raw) {
        this(JSON.parse(null, raw).map());
    }

    public JSONMap toMap() {
        var res = new JSONMap();
        return res;
    }
    @Override
    public String toString() {
        return JSON.stringify(new JSONMap()
            .set("method", name)
            .set("params", params)
            .set("id", id)
        );
    }
}
