package me.topchetoeu.jscript.compilation.scope;

import me.topchetoeu.jscript.common.Instruction;

public final class VariableIndex {
    public static enum IndexType {
        LOCALS,
        CAPTURABLES,
        CAPTURES,
    }

    public final VariableIndex.IndexType type;
    public final int index;

    public final int toCaptureIndex() {
        switch (type) {
            case CAPTURES: return ~index;
            case CAPTURABLES: return index;
            default: throw new UnsupportedOperationException("Index type " + type + " may not be captured");
        }
    }

    public final Instruction toGet() {
        switch (type) {
            case CAPTURES: return Instruction.loadVar(~index);
            case CAPTURABLES: return Instruction.loadVar(index);
            case LOCALS: return Instruction.loadVar(index);
            default: throw new UnsupportedOperationException("Unknown index type " + type);
        }
    }
    public final Instruction toSet(boolean keep) {
        switch (type) {
            case CAPTURES: return Instruction.storeVar(index, keep, false);
            case CAPTURABLES: return Instruction.storeVar(index, keep, false);
            case LOCALS: return Instruction.storeVar(index, keep, false);
            default: throw new UnsupportedOperationException("Unknown index type " + type);
        }
    }
    public final Instruction toInit() {
        switch (type) {
            case CAPTURES: throw new UnsupportedOperationException("Unknown index type " + type);
            case CAPTURABLES: return Instruction.storeVar(index, false, true);
            case LOCALS: return Instruction.storeVar(index, false, true);
            default: throw new UnsupportedOperationException("Unknown index type " + type);
        }
    }
    public final Instruction toUndefinedInit(boolean force) {
        switch (type) {
            case CAPTURES: throw new UnsupportedOperationException("Unknown index type " + type);
            case CAPTURABLES: return Instruction.varInit(index, force);
            case LOCALS: return Instruction.varInit(index, force);
            default: throw new UnsupportedOperationException("Unknown index type " + type);
        }
    }

    public VariableIndex(VariableIndex.IndexType type, int index) {
        this.type = type;
        this.index = index;
    }
}