package me.topchetoeu.jscript.common.parsing;

public interface Parser<T> {
    public ParseRes<T> parse(Source src, int i);
}