package me.topchetoeu.jscript.engine.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.BreakpointData;
import me.topchetoeu.jscript.engine.DebugCommand;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.events.Event;

public class DebugState {
    private boolean paused = false;

    public final HashSet<Location> breakpoints = new HashSet<>();
    public final List<CodeFrame> frames = new ArrayList<>();
    public final Map<String, String> sources = new HashMap<>();

    public final Event<BreakpointData> breakpointNotifier = new Event<>();
    public final Event<DebugCommand> commandNotifier = new Event<>();
    public final Event<String> sourceAdded = new Event<>();

    public DebugState pushFrame(CodeFrame frame) {
        frames.add(frame);
        return this;
    }
    public DebugState popFrame() {
        if (frames.size() > 0) frames.remove(frames.size() - 1);
        return this;
    }

    public DebugCommand pause(BreakpointData data) throws InterruptedException {
        paused = true;
        breakpointNotifier.next(data);
        return commandNotifier.toAwaitable().await();
    }
    public void resume(DebugCommand command) {
        paused = false;
        commandNotifier.next(command);
    }

    // public void addSource()?

    public boolean paused() { return paused; }

    public boolean isBreakpoint(Location loc) {
        return breakpoints.contains(loc);
    }
}
