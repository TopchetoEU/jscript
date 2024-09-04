package me.topchetoeu.jscript.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class RefTracker {
    public static void onDestroy(Object obj, Runnable runnable) {
        var queue = new ReferenceQueue<>();
        var ref = new WeakReference<>(obj, queue);
        obj = null;

        var th = new Thread(() -> {
            try {
                queue.remove();
                ref.get();
                runnable.run();
            }
            catch (InterruptedException e) { return; }
        });
        th.setDaemon(true);
        th.start();
    }
}
