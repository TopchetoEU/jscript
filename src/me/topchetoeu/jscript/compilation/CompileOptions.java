package me.topchetoeu.jscript.compilation;

public class CompileOptions {
    public final boolean emitBpMap;
    public final boolean emitVarNames;

    public CompileOptions(boolean emitBpMap, boolean emitVarNames) {
        this.emitBpMap = emitBpMap;
        this.emitVarNames = emitVarNames;
    }
}
