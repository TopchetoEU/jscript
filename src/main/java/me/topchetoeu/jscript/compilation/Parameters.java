package me.topchetoeu.jscript.compilation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Parameters {
    public final int length;
    public final List<Parameter> params;
    public final Set<String> names;

    public Parameters(List<Parameter> params) {
        this.names = new HashSet<>();
        var len = params.size();

        for (var i = params.size() - 1; i >= 0; i--) {
            if (params.get(i).node == null) break;
            len--;
        }

        for (var param : params) {
            this.names.add(param.name);
        }

        this.params = params;
        this.length = len;
    }
}
