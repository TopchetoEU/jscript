package me.topchetoeu.jscript.utils.debug;

import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONMap;

public class V8Result {
    public final int id;
    public final JSONMap result;

    public V8Result(int id, JSONMap result) {
        this.id = id;
        this.result = result;
    }

    @Override
    public String toString() {
        return JSON.stringify(new JSONMap()
            .set("id", id)
            .set("result", result)
        );
    }
}
