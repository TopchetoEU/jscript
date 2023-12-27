package me.topchetoeu.jscript.lib;

import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.filesystem.EntryType;
import me.topchetoeu.jscript.filesystem.File;
import me.topchetoeu.jscript.filesystem.FileStat;
import me.topchetoeu.jscript.filesystem.Filesystem;
import me.topchetoeu.jscript.filesystem.FilesystemException;
import me.topchetoeu.jscript.filesystem.Mode;
import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;
import me.topchetoeu.jscript.interop.Native;

@Native("Filesystem")
public class FilesystemLib {
    @Native public static final int SEEK_SET = 0;
    @Native public static final int SEEK_CUR = 1;
    @Native public static final int SEEK_END = 2;

    private static Filesystem fs(Context ctx) {
        var fs = Filesystem.get(ctx);
        if (fs != null) return fs;
        throw EngineException.ofError("Current environment doesn't have a file system.");
    }

    @Native public static String normalize(Context ctx, String... paths) {
        return fs(ctx).normalize(paths);
    }

    @Native public static PromiseLib open(Context ctx, String _path, String mode) {
        var path = fs(ctx).normalize(_path);
        var _mode = Mode.parse(mode);

        return PromiseLib.await(ctx, () -> {
            try {
                if (fs(ctx).stat(path).type != EntryType.FILE) {
                    throw new FilesystemException(path, FSCode.NOT_FILE);
                }

                var file = fs(ctx).open(path, _mode);
                return new FileLib(file);
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public static ObjectValue ls(Context ctx, String _path) throws IOException {
        var path = fs(ctx).normalize(_path);

        return Values.toJSAsyncIterator(ctx, new Iterator<>() {
            private boolean failed, done;
            private File file;
            private String nextLine;

            private void update() {
                if (done) return;
                if (!failed) {
                    if (file == null) {
                        if (fs(ctx).stat(path).type != EntryType.FOLDER) {
                            throw new FilesystemException(path, FSCode.NOT_FOLDER);
                        }

                        file = fs(ctx).open(path, Mode.READ);
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
    @Native public static PromiseLib mkdir(Context ctx, String _path) throws IOException {
        return PromiseLib.await(ctx, () -> {
            try {
                fs(ctx).create(Filename.parse(_path).toString(), EntryType.FOLDER);
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });

    }
    @Native public static PromiseLib mkfile(Context ctx, String path) throws IOException {
        return PromiseLib.await(ctx, () -> {
            try {
                fs(ctx).create(path, EntryType.FILE);
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public static PromiseLib rm(Context ctx, String path, boolean recursive) throws IOException {
        return PromiseLib.await(ctx, () -> {
            try {
                if (!recursive) fs(ctx).create(path, EntryType.NONE);
                else {
                    var stack = new Stack<String>();
                    stack.push(path);

                    while (!stack.empty()) {
                        var currPath = stack.pop();
                        FileStat stat;

                        try { stat = fs(ctx).stat(currPath); }
                        catch (FilesystemException e) { continue; }

                        if (stat.type == EntryType.FOLDER) {
                            for (var el : fs(ctx).open(currPath, Mode.READ).readToString().split("\n")) stack.push(el);
                        }
                        else fs(ctx).create(currPath, EntryType.NONE);
                    }
                }
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public static PromiseLib stat(Context ctx, String path) throws IOException {
        return PromiseLib.await(ctx, () -> {
            try {
                var stat = fs(ctx).stat(path);
                var res = new ObjectValue();

                res.defineProperty(ctx, "type", stat.type.name);
                res.defineProperty(ctx, "mode", stat.mode.name);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public static PromiseLib exists(Context ctx, String _path) throws IOException {
        return PromiseLib.await(ctx, () -> {
            try { fs(ctx).stat(_path); return true; }
            catch (FilesystemException e) { return false; }
        });
    }
}
