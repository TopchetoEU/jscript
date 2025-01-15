package me.topchetoeu.j2s.compilation.parsing;

public interface Parser<T> {
	public ParseRes<T> parse(Source src, int i);
}