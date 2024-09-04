package me.topchetoeu.jscript.runtime;

import java.util.Iterator;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.CodeFunction;
import me.topchetoeu.jscript.runtime.values.FunctionValue;

public class Context implements Extensions {
    public final Context parent;
    public final Extensions extensions;
    public final Frame frame;
    public final int stackSize;

    @Override public <T> void add(Key<T> key, T obj) {
        if (extensions != null) extensions.add(key, obj);
    }
    @Override public <T> T get(Key<T> key) {
        if (extensions != null && extensions.has(key)) return extensions.get(key);
        return null;
    }
    @Override public boolean has(Key<?> key) {
        return extensions != null && extensions.has(key);
    }
    @Override public boolean remove(Key<?> key) {
        var res = false;
        if (extensions != null) res |= extensions.remove(key);
        return res;
    }
    @Override public Iterable<Key<?>> keys() {
        if (extensions == null) return List.of();
        else return extensions.keys();
    }

    public FunctionValue compile(Filename filename, String raw) {
        DebugContext.get(this).onSource(filename, raw);
        var result = new CodeFunction(extensions, filename.toString(), Compiler.get(this).compile(filename, raw), new ValueVariable[0]);
        return result;
    }

    public Context pushFrame(Frame frame) {
        var res = new Context(this, frame.function.extensions, frame, stackSize + 1);
        return res;
    }

    public Iterable<Frame> frames() {
        var self = this;
        return () -> new Iterator<Frame>() {
            private Context curr = self;

            private void update() {
                while (curr != null && curr.frame == null) curr = curr.parent;
            }

            @Override public boolean hasNext() {
                update();
                return curr != null;
            }
            @Override public Frame next() {
                update();
                var res = curr.frame;
                curr = curr.parent;
                return res;
            }
        };
    }

    private Context(Context parent, Extensions ext, Frame frame, int stackSize) {
        this.parent = parent;
        this.extensions = ext;
        this.frame = frame;
        this.stackSize = stackSize;

        if (hasNotNull(Environment.MAX_STACK_COUNT) && stackSize > (int)get(Environment.MAX_STACK_COUNT)) {
            throw EngineException.ofRange("Stack overflow!");
        }
    }

    public Context() {
        this(null, null, null, 0);
    }
    public Context(Extensions ext) {
        this(null, clean(ext), null, 0);
    }

    public static Context of(Extensions ext) {
        if (ext instanceof Context) return (Context)ext;
        return new Context(ext);
    }
    public static Extensions clean(Extensions ext) {
        if (ext instanceof Context) return clean(((Context)ext).extensions);
        else return ext;
    }
}
