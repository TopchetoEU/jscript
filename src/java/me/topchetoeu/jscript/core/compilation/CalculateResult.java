package me.topchetoeu.jscript.core.compilation;

import me.topchetoeu.jscript.core.engine.values.Values;

public final class CalculateResult {
    public final boolean exists;
    public final Object value;

    public final boolean isTruthy() {
        return exists && Values.toBoolean(value);
    }

    public CalculateResult(Object value) {
        this.exists = true;
        this.value = value;
    }
    public CalculateResult() {
        this.exists = false;
        this.value = null;
    }
}