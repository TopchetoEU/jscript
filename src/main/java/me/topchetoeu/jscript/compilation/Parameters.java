package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.parsing.Location;

public final class Parameters {
    public final int length;
    public final List<Parameter> params;
    public final String restName;
    public final Location restLocation;

    public Parameters(List<Parameter> params, String restName, Location restLocation) {
        var len = params.size();

        for (var i = params.size() - 1; i >= 0; i--) {
            if (params.get(i).node == null) break;
            len--;
        }

        this.params = params;
        this.length = len;
        this.restName = restName;
        this.restLocation = restLocation;
    }
    public Parameters(List<Parameter> params) {
        this(params, null, null);
    }
}
