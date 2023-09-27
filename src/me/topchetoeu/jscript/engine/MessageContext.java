package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.exceptions.EngineException;

public class MessageContext {
    public final Engine engine;

    private final ArrayList<CodeFrame> frames = new ArrayList<>();
    public int maxStackFrames = 1000;

    public List<CodeFrame> frames() { return Collections.unmodifiableList(frames); }

    public MessageContext pushFrame(Context ctx, CodeFrame frame) throws InterruptedException {
        this.frames.add(frame);
        if (this.frames.size() > maxStackFrames) throw EngineException.ofRange("Stack overflow!");
        return this;
    }
    public boolean popFrame(CodeFrame frame) {
        if (this.frames.size() == 0) return false;
        if (this.frames.get(this.frames.size() - 1) != frame) return false;
        this.frames.remove(this.frames.size() - 1);
        return true;
    }

    public List<String> stackTrace() {
        var res = new ArrayList<String>();

        for (var el : frames) {
            var name = el.function.name;
            var loc = el.function.loc();
            var trace = "";

            if (loc != null) trace += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) trace += "in " + name + " ";

            trace = trace.trim();

            if (!res.equals("")) res.add(trace);
        }

        return res;
    }

    public MessageContext(Engine engine) {
        this.engine = engine;
    }
}
