package me.topchetoeu.jscript.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import me.topchetoeu.jscript.core.exceptions.SyntaxException;

public class Instruction {
    public static enum Type {
        NOP(0),
        RETURN(1),
        THROW(2),
        THROW_SYNTAX(3),
        DELETE(4),
        TRY_START(5),
        TRY_END(6),

        CALL(7),
        CALL_NEW(8),
        JMP_IF(9),
        JMP_IFN(10),
        JMP(11),

        PUSH_UNDEFINED(12),
        PUSH_NULL(13),
        PUSH_BOOL(14),
        PUSH_NUMBER(15),
        PUSH_STRING(16),

        LOAD_VAR(17),
        LOAD_MEMBER(18),
        LOAD_GLOB(20),

        LOAD_FUNC(21),
        LOAD_ARR(22),
        LOAD_OBJ(23),
        STORE_SELF_FUNC(24),
        LOAD_REGEX(25),

        DUP(26),

        STORE_VAR(27),
        STORE_MEMBER(28),
        DISCARD(29),

        MAKE_VAR(30),
        DEF_PROP(31),
        KEYS(32),

        TYPEOF(33),
        OPERATION(34);

        private static final HashMap<Integer, Type> types = new HashMap<>();
        public final int numeric;

        static {
            for (var val : Type.values()) types.put(val.numeric, val);
        }

        private Type(int numeric) {
            this.numeric = numeric;
        }

        public static Type fromNumeric(int i) {
            return types.get(i);
        }
    }
    public static enum BreakpointType {
        NONE,
        STEP_OVER,
        STEP_IN;

        public boolean shouldStepIn() {
            return this != NONE;
        }
        public boolean shouldStepOver() {
            return this == STEP_OVER;
        }
    }

    public final Type type;
    public final Object[] params;

    @SuppressWarnings("unchecked")
    public <T> T get(int i) {
        if (i >= params.length || i < 0) return null;
        return (T)params[i];
    }
    @SuppressWarnings("unchecked")
    public <T> T get(int i, T defaultVal) {
        if (i >= params.length || i < 0) return defaultVal;
        return (T)params[i];
    }
    public boolean match(Object ...args) {
        if (args.length != params.length) return false;
        for (int i = 0; i < args.length; i++) {
            var a = params[i];
            var b = args[i];
            if (a == null || b == null) {
                if (!(a == null && b == null)) return false;
            }
            if (!a.equals(b)) return false;
        }
        return true;
    }
    public boolean is(int i, Object arg) {
        if (params.length <= i) return false;
        return params[i].equals(arg);
    }

    public void write(DataOutputStream writer) throws IOException {
        var rawType = type.numeric;

        switch (type) {
            case KEYS:
            case PUSH_BOOL:
            case STORE_MEMBER: rawType |= (boolean)get(0) ? 128 : 0; break;
            case STORE_VAR: rawType |= (boolean)get(1) ? 128 : 0; break;
            case TYPEOF: rawType |= params.length > 0 ? 128 : 0; break;
            default:
        }

        writer.writeByte(rawType);

        switch (type) {
            case CALL: writer.writeInt(get(0)); break;
            case CALL_NEW: writer.writeInt(get(0)); break;
            case DUP: writer.writeInt(get(0)); break;
            case JMP: writer.writeInt(get(0)); break;
            case JMP_IF: writer.writeInt(get(0)); break;
            case JMP_IFN: writer.writeInt(get(0)); break;
            case LOAD_ARR: writer.writeInt(get(0)); break;
            case LOAD_FUNC: {
                writer.writeInt(params.length - 1);

                for (var i = 0; i < params.length; i++) {
                    writer.writeInt(get(i + 1));
                }

                writer.writeInt(get(0));
                break;
            }
            case LOAD_REGEX: writer.writeUTF(get(0)); break;
            case LOAD_VAR: writer.writeInt(get(0)); break;
            case MAKE_VAR: writer.writeUTF(get(0)); break;
            case OPERATION: writer.writeByte(((Operation)get(0)).numeric); break;
            case PUSH_NUMBER: writer.writeDouble(get(0)); break;
            case PUSH_STRING: writer.writeUTF(get(0)); break;
            case STORE_SELF_FUNC: writer.writeInt(get(0)); break;
            case STORE_VAR: writer.writeInt(get(0)); break;
            case THROW_SYNTAX: writer.writeUTF(get(0));
            case TRY_START:
                writer.writeInt(get(0));
                writer.writeInt(get(1));
                writer.writeInt(get(2));
                break;
            case TYPEOF:
                if (params.length > 0) writer.writeUTF(get(0));
                break;
            default:
        }
    }

    private Instruction(Type type, Object ...params) {
        this.type = type;
        this.params = params;
    }

    public static Instruction read(DataInputStream stream) throws IOException {
        var rawType = stream.readUnsignedByte();
        var type = Type.fromNumeric(rawType & 127);
        var flag = (rawType & 128) != 0;

        switch (type) {
            case CALL: return call(stream.readInt());
            case CALL_NEW: return callNew(stream.readInt());
            case DEF_PROP: return defProp();
            case DELETE: return delete();
            case DISCARD: return discard();
            case DUP: return dup(stream.readInt());
            case JMP: return jmp(stream.readInt());
            case JMP_IF: return jmpIf(stream.readInt());
            case JMP_IFN: return jmpIfNot(stream.readInt());
            case KEYS: return keys(flag);
            case LOAD_ARR: return loadArr(stream.readInt());
            case LOAD_FUNC: {
                var captures = new int[stream.readInt()];

                for (var i = 0; i < captures.length; i++) {
                    captures[i] = stream.readInt();
                }

                return loadFunc(stream.readInt(), captures);
            }
            case LOAD_GLOB: return loadGlob();
            case LOAD_MEMBER: return loadMember();
            case LOAD_OBJ: return loadObj();
            case LOAD_REGEX: return loadRegex(stream.readUTF(), null);
            case LOAD_VAR: return loadVar(stream.readInt());
            case MAKE_VAR: return makeVar(stream.readUTF());
            case OPERATION: return operation(Operation.fromNumeric(stream.readUnsignedByte()));
            case PUSH_NULL: return pushNull();
            case PUSH_UNDEFINED: return pushUndefined();
            case PUSH_BOOL: return pushValue(flag);
            case PUSH_NUMBER: return pushValue(stream.readDouble());
            case PUSH_STRING: return pushValue(stream.readUTF());
            case RETURN: return ret();
            case STORE_MEMBER: return storeMember(flag);
            case STORE_SELF_FUNC: return storeSelfFunc(stream.readInt());
            case STORE_VAR: return storeVar(stream.readInt(), flag);
            case THROW: return throwInstr();
            case THROW_SYNTAX: return throwSyntax(stream.readUTF());
            case TRY_END: return tryEnd();
            case TRY_START: return tryStart(stream.readInt(), stream.readInt(), stream.readInt());
            case TYPEOF: return flag ? typeof(stream.readUTF()) : typeof();
            case NOP:
                if (flag) return null;
                else return nop();
            default: return null;
        }
    }

    public static Instruction tryStart(int catchStart, int finallyStart, int end) {
        return new Instruction(Type.TRY_START, catchStart, finallyStart, end);
    }
    public static Instruction tryEnd() {
        return new Instruction(Type.TRY_END);
    }
    public static Instruction throwInstr() {
        return new Instruction(Type.THROW);
    }
    public static Instruction throwSyntax(SyntaxException err) {
        return new Instruction(Type.THROW_SYNTAX, err.getMessage());
    }
    public static Instruction throwSyntax(String err) {
        return new Instruction(Type.THROW_SYNTAX, err);
    }
    public static Instruction delete() {
        return new Instruction(Type.DELETE);
    }
    public static Instruction ret() {
        return new Instruction(Type.RETURN);
    }
    public static Instruction debug() {
        return new Instruction(Type.NOP, "debug");
    }

    public static Instruction nop(Object ...params) {
        return new Instruction(Type.NOP, params);
    }

    public static Instruction call(int argn) {
        return new Instruction(Type.CALL, argn);
    }
    public static Instruction callNew(int argn) {
        return new Instruction(Type.CALL_NEW, argn);
    }
    public static Instruction jmp(int offset) {
        return new Instruction(Type.JMP, offset);
    }
    public static Instruction jmpIf(int offset) {
        return new Instruction(Type.JMP_IF, offset);
    }
    public static Instruction jmpIfNot(int offset) {
        return new Instruction(Type.JMP_IFN, offset);
    }

    public static Instruction pushUndefined() {
        return new Instruction(Type.PUSH_UNDEFINED);
    }
    public static Instruction pushNull() {
        return new Instruction(Type.PUSH_NULL);
    }
    public static Instruction pushValue(boolean val) {
        return new Instruction(Type.PUSH_BOOL, val);
    }
    public static Instruction pushValue(double val) {
        return new Instruction(Type.PUSH_NUMBER, val);
    }
    public static Instruction pushValue(String val) {
        return new Instruction(Type.PUSH_STRING, val);
    }

    public static Instruction makeVar(String name) {
        return new Instruction(Type.MAKE_VAR, name);
    }
    public static Instruction loadVar(Object i) {
        return new Instruction(Type.LOAD_VAR, i);
    }
    public static Instruction loadGlob() {
        return new Instruction(Type.LOAD_GLOB);
    }
    public static Instruction loadMember() {
        return new Instruction(Type.LOAD_MEMBER);
    }

    public static Instruction loadRegex(String pattern, String flags) {
        return new Instruction(Type.LOAD_REGEX, pattern, flags);
    }
    public static Instruction loadFunc(int id, int[] captures) {
        var args = new Object[1 + captures.length];
        args[0] = id;
        for (var i = 0; i < captures.length; i++) args[i + 1] = captures[i];
        return new Instruction(Type.LOAD_FUNC, args);
    }
    public static Instruction loadObj() {
        return new Instruction(Type.LOAD_OBJ);
    }
    public static Instruction loadArr(int count) {
        return new Instruction(Type.LOAD_ARR, count);
    }
    public static Instruction dup() {
        return new Instruction(Type.DUP, 1);
    }
    public static Instruction dup(int count) {
        return new Instruction(Type.DUP, count);
    }

    public static Instruction storeSelfFunc(int i) {
        return new Instruction(Type.STORE_SELF_FUNC, i);
    }
    public static Instruction storeVar(Object i) {
        return new Instruction(Type.STORE_VAR, i, false);
    }
    public static Instruction storeVar(Object i, boolean keep) {
        return new Instruction(Type.STORE_VAR, i, keep);
    }
    public static Instruction storeMember() {
        return new Instruction(Type.STORE_MEMBER, false);
    }
    public static Instruction storeMember(boolean keep) {
        return new Instruction(Type.STORE_MEMBER, keep);
    }
    public static Instruction discard() {
        return new Instruction(Type.DISCARD);
    }

    public static Instruction typeof() {
        return new Instruction(Type.TYPEOF);
    }
    public static Instruction typeof(String varName) {
        return new Instruction(Type.TYPEOF, varName);
    }

    public static Instruction keys(boolean forInFormat) {
        return new Instruction(Type.KEYS, forInFormat);
    }

    public static Instruction defProp() {
        return new Instruction(Type.DEF_PROP);
    }

    public static Instruction operation(Operation op) {
        return new Instruction(Type.OPERATION, op);
    }

    @Override
    public String toString() {
        var res = type.toString();

        for (int i = 0; i < params.length; i++) {
            res += " " + params[i];
        }

        return res;
    }
}
