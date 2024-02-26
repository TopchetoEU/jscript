package me.topchetoeu.jscript.compilation.parsing;

import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.parsing.Parsing.Parser;

public class ParseRes<T> {
    public static enum State {
        SUCCESS,
        FAILED,
        ERROR;

        public boolean isSuccess() { return this == SUCCESS; }
        public boolean isFailed() { return this == FAILED; }
        public boolean isError() { return this == ERROR; }
    }

    public final ParseRes.State state;
    public final String error;
    public final T result;
    public final int n;

    private ParseRes(ParseRes.State state, String error, T result, int readN) {
        this.result = result;
        this.n = readN;
        this.state = state;
        this.error = error;
    }

    public ParseRes<T> setN(int i) {
        if (!state.isSuccess()) return this;
        return new ParseRes<>(state, null, result, i);
    }
    public ParseRes<T> addN(int i) {
        if (!state.isSuccess()) return this;
        return new ParseRes<>(state, null, result, this.n + i);
    }
    public <T2> ParseRes<T2> transform() {
        if (isSuccess()) throw new RuntimeException("Can't transform a ParseRes that hasn't failed.");
        return new ParseRes<>(state, error, null, 0);
    }
    public TestRes toTest() {
        if (isSuccess()) return TestRes.res(n);
        else if (isError()) return TestRes.error(null, error);
        else return TestRes.failed();
    }

    public boolean isSuccess() { return state.isSuccess(); }
    public boolean isFailed() { return state.isFailed(); }
    public boolean isError() { return state.isError(); }

    public static <T> ParseRes<T> failed() {
        return new ParseRes<T>(State.FAILED, null, null, 0);
    }
    public static <T> ParseRes<T> error(Location loc, String error) {
        if (loc != null) error = loc + ": " + error;
        return new ParseRes<>(State.ERROR, error, null, 0);
    }
    public static <T> ParseRes<T> error(Location loc, String error, ParseRes<?> other) {
        if (loc != null) error = loc + ": " + error;
        if (!other.isError()) return new ParseRes<>(State.ERROR, error, null, 0);
        return new ParseRes<>(State.ERROR, other.error, null, 0);
    }
    public static <T> ParseRes<T> res(T val, int i) {
        return new ParseRes<>(State.SUCCESS, null, val, i);
    }

    @SafeVarargs
    public static <T> ParseRes<? extends T> any(ParseRes<? extends T> ...parsers) {
        return any(List.of(parsers));
    }
    public static <T> ParseRes<? extends T> any(List<ParseRes<? extends T>> parsers) {
        ParseRes<? extends T> best = null;
        ParseRes<? extends T> error = ParseRes.failed();

        for (var parser : parsers) {
            if (parser.isSuccess()) {
                if (best == null || best.n < parser.n) best = parser;
            }
            else if (parser.isError() && error.isFailed()) error = parser.transform();
        }

        if (best != null) return best;
        else return error;
    }
    @SafeVarargs
    public static <T> ParseRes<? extends T> first(String filename, List<Token> tokens, Map<String, Parser<T>> named, Parser<? extends T> ...parsers) {
        ParseRes<? extends T> error = ParseRes.failed();

        for (var parser : parsers) {
            var res = parser.parse(null, tokens, 0);
            if (res.isSuccess()) return res;
            else if (res.isError() && error.isFailed()) error = res.transform();
        }

        return error;
    }
}