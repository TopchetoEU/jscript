package me.topchetoeu.jscript.common.parsing;

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
    public final Location errorLocation;
    public final String error;
    public final T result;
    public final int n;

    private ParseRes(ParseRes.State state, Location errorLocation, String error, T result, int readN) {
        this.result = result;
        this.n = readN;
        this.state = state;
        this.error = error;
        this.errorLocation = errorLocation;
    }

    public ParseRes<T> setN(int i) {
        if (!state.isSuccess()) return this;
        return new ParseRes<>(state, null, null, result, i);
    }
    public ParseRes<T> addN(int n) {
        if (!state.isSuccess()) return this;
        return new ParseRes<>(state, null, null, result, this.n + n);
    }
    public <T2> ParseRes<T2> chainError() {
        if (isSuccess()) throw new RuntimeException("Can't transform a ParseRes that hasn't failed");
        return new ParseRes<>(state, errorLocation, error, null, 0);
    }
    @SuppressWarnings("unchecked")
    public <T2> ParseRes<T2> chainError(ParseRes<?> other) {
        if (!this.isError()) return other.chainError();
        return (ParseRes<T2>) this;
    }
    @SuppressWarnings("unchecked")
    public <T2> ParseRes<T2> chainError(Location loc, String error) {
        if (!this.isError()) return new ParseRes<>(State.ERROR, loc, error, null, 0);
        return (ParseRes<T2>) this;
    }

    public boolean isSuccess() { return state.isSuccess(); }
    public boolean isFailed() { return state.isFailed(); }
    public boolean isError() { return state.isError(); }

    public static <T> ParseRes<T> failed() {
        return new ParseRes<T>(State.FAILED, null, null, null, 0);
    }
    public static <T> ParseRes<T> error(Location loc, String error) {
        // TODO: differentiate definitive and probable errors
        return new ParseRes<>(State.ERROR, loc, error, null, 0);
    }
    public static <T> ParseRes<T> res(T val, int i) {
        return new ParseRes<>(State.SUCCESS, null, null, val, i);
    }

    @SafeVarargs
    @SuppressWarnings("all")
    public static <T> ParseRes<T> first(Source src, int i, Parser ...parsers) {
        int n = Parsing.skipEmpty(src, i);
        ParseRes<T> error = ParseRes.failed();

        for (var parser : parsers) {
            var res = parser.parse(src, i + n);
            if (res.isSuccess()) return res.addN(n);
            if (res.isError() && error.isFailed()) error = res.chainError();
        }

        return error;
    }
}