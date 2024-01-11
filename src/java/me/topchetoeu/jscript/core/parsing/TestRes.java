package me.topchetoeu.jscript.core.parsing;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.parsing.ParseRes.State;

public class TestRes {
    public final State state;
    public final String error;
    public final int i;

    private TestRes(ParseRes.State state, String error, int i) {
        this.i = i;
        this.state = state;
        this.error = error;
    }

    public TestRes add(int n) {
        return new TestRes(state, null, this.i + n);
    }
    public <T> ParseRes<T> transform() {
        if (isSuccess()) throw new RuntimeException("Can't transform a TestRes that hasn't failed.");
        else if (isError()) return ParseRes.error(null, error);
        else return ParseRes.failed();
    }

    public boolean isSuccess() { return state.isSuccess(); }
    public boolean isFailed() { return state.isFailed(); }
    public boolean isError() { return state.isError(); }

    public static TestRes failed() {
        return new TestRes(State.FAILED, null, 0);
    }
    public static TestRes error(Location loc, String error) {
        if (loc != null) error = loc + ": " + error;
        return new TestRes(State.ERROR, error, 0);
    }
    public static TestRes error(Location loc, String error, TestRes other) {
        if (loc != null) error = loc + ": " + error;
        if (!other.isError()) return new TestRes(State.ERROR, error, 0);
        return new TestRes(State.ERROR, other.error, 0);
    }
    public static TestRes res(int i) {
        return new TestRes(State.SUCCESS, null, i);
    }
}