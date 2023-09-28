package me.topchetoeu.jscript.polyfills;

import java.util.HashMap;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.interop.Native;

public class TimerPolyfills {
    private HashMap<Integer, Thread> threads = new HashMap<>();

    private int i = 0;

    @Native public int setTimeout(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.message.engine.pushMsg(false, ctx.message, func, null, args);
        });
        thread.start();

        threads.put(++i, thread);

        return i;
    }
    @Native public int setInterval(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }
    
                ctx.message.engine.pushMsg(false, ctx.message, func, null, args);
            }
        });
        thread.start();

        threads.put(++i, thread);

        return i;
    }

    @Native public void clearTimeout(Context ctx, int i) {
        var thread = threads.remove(i);
        if (thread != null) thread.interrupt();
    }
    @Native public void clearInterval(Context ctx, int i) {
        clearTimeout(ctx, i);
    }
}
