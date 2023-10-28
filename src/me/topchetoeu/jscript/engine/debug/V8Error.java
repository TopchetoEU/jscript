package me.topchetoeu.jscript.engine.debug;

import me.topchetoeu.jscript.json.JSON;
import me.topchetoeu.jscript.json.JSONMap;

public class V8Error {
    public final String message;

    public V8Error(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return JSON.stringify(new JSONMap().set("error", new JSONMap()
            .set("message", message)
        ));
    }
}
