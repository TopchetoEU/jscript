package me.topchetoeu.jscript.lib;

import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.filesystem.ActionType;
import me.topchetoeu.jscript.utils.filesystem.EntryType;
import me.topchetoeu.jscript.utils.filesystem.ErrorReason;
import me.topchetoeu.jscript.utils.filesystem.File;
import me.topchetoeu.jscript.utils.filesystem.FileStat;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException;
import me.topchetoeu.jscript.utils.filesystem.Mode;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Filesystem")
public class FilesystemLib {
    @ExposeField(target = ExposeTarget.STATIC)
    public static final int __SEEK_SET = 0;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final int __SEEK_CUR = 1;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final int __SEEK_END = 2;

    private static Filesystem fs(Context ctx) {
        var fs = Filesystem.get(ctx);
        if (fs != null) return fs;
        throw EngineException.ofError("Current environment doesn't have a file system.");
    }

    @Expose(target = ExposeTarget.STATIC)
    public static String __normalize(Arguments args) {
        return fs(args.ctx).normalize(args.convert(String.class));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __open(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            var fs = fs(args.ctx);
            var path = fs.normalize(args.getString(0));
            var _mode = Mode.parse(args.getString(1));

            try {
                if (fs.stat(path).type != EntryType.FILE) {
                    throw new FilesystemException(ErrorReason.DOESNT_EXIST, "Not a file").setAction(ActionType.OPEN);
                }

                return new FileLib(fs.open(path, _mode));
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ObjectValue __ls(Arguments args) {

        return Values.toJSAsyncIterator(args.ctx, new Iterator<>() {
            private boolean failed, done;
            private File file;
            private String nextLine;

            private void update() {
                if (done) return;
                if (!failed) {
                    if (file == null) {
                        var fs = fs(args.ctx);
                        var path = fs.normalize(args.getString(0));

                        if (fs.stat(path).type != EntryType.FOLDER) {
                            throw new FilesystemException(ErrorReason.DOESNT_EXIST, "Not a directory").setAction(ActionType.OPEN);
                        }

                        file = fs.open(path, Mode.READ);
                    }

                    if (nextLine == null) {
                        while (true) {
                            nextLine = file.readLine();
                            if (nextLine == null) {
                                done = true;
                                return;
                            }
                            nextLine = nextLine.trim();
                            if (!nextLine.equals("")) break;
                        }
                    }
                }
            }

            @Override
            public boolean hasNext() {
                try {
                    update();
                    return !done && !failed;
                }
                catch (FilesystemException e) { throw e.toEngineException(); }
            }
            @Override
            public String next() {
                try {
                    update();
                    var res = nextLine;
                    nextLine = null;
                    return res;
                }
                catch (FilesystemException e) { throw e.toEngineException(); }
            }
        });
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __mkdir(Arguments args) throws IOException {
        return PromiseLib.await(args.ctx, () -> {
            try {
                fs(args.ctx).create(args.getString(0), EntryType.FOLDER);
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });

    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __mkfile(Arguments args) throws IOException {
        return PromiseLib.await(args.ctx, () -> {
            try {
                fs(args.ctx).create(args.getString(0), EntryType.FILE);
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __rm(Arguments args) throws IOException {
        return PromiseLib.await(args.ctx, () -> {
            try {
                var fs = fs(args.ctx);
                var path = fs.normalize(args.getString(0));
                var recursive = args.getBoolean(1);

                if (!recursive) fs.create(path, EntryType.NONE);
                else {
                    var stack = new Stack<String>();
                    stack.push(path);

                    while (!stack.empty()) {
                        var currPath = stack.pop();
                        FileStat stat;

                        try { stat = fs.stat(currPath); }
                        catch (FilesystemException e) { continue; }

                        if (stat.type == EntryType.FOLDER) {
                            for (var el : fs.open(currPath, Mode.READ).readToString().split("\n")) stack.push(el);
                        }
                        else fs.create(currPath, EntryType.NONE);
                    }
                }
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __stat(Arguments args) throws IOException {
        return PromiseLib.await(args.ctx, () -> {
            try {
                var fs = fs(args.ctx);
                var path = fs.normalize(args.getString(0));
                var stat = fs.stat(path);
                var res = new ObjectValue();

                res.defineProperty(args.ctx, "type", stat.type.name);
                res.defineProperty(args.ctx, "mode", stat.mode.name);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __exists(Arguments args) throws IOException {
        return PromiseLib.await(args.ctx, () -> {
            try { fs(args.ctx).stat(args.getString(0)); return true; }
            catch (FilesystemException e) { return false; }
        });
    }
}
