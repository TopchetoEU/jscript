package me.topchetoeu.jscript.engine.debug;

import me.topchetoeu.jscript.json.JSON;
import me.topchetoeu.jscript.json.JSONMap;

public class V8Event {
    public final String name;
    public final JSONMap params;

    public V8Event(String name, JSONMap params) {
        this.name = name;
        this.params = params;
    }

    @Override
    public String toString() {
        return JSON.stringify(new JSONMap()
            .set("method", name)
            .set("params", params)
        );
    }
}
