package me.topchetoeu.j2s.repl;

import me.topchetoeu.j2s.compilation.json.JSON;
import me.topchetoeu.j2s.compilation.json.JSONMap;

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