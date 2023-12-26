package me.topchetoeu.jscript.filesystem;

import java.util.ArrayList;

public class Paths {
    public static String normalize(String path) {
        var parts = path.split("[\\\\/]");
        var res = new ArrayList<String>();

        for (var part : parts) {
            if (part.equals("...")) res.clear();
            else if (part.equals("..")) {
                if (res.size() > 0) res.remove(res.size() - 1);
            }
            else if (!part.equals(".")) res.add(part);
        }

        var sb = new StringBuilder();

        for (var el : res) sb.append("/").append(el);

        return sb.toString();
    }

    public static String chroot(String root, String path) {
        return normalize(root) + normalize(path);
    }

    public static String cwd(String cwd, String path) {
        return normalize(cwd + "/" + path);
    }

    public static String filename(String path) {
        var i = path.lastIndexOf('/');
        if (i < 0) i = path.lastIndexOf('\\');

        if (i < 0) return path;
        else return path.substring(i + 1);
    }

    public static String extension(String path) {
        var i = path.lastIndexOf('.');

        if (i < 0) return "";
        else return path.substring(i + 1);
    }

    public static String dir(String path) {
        return normalize(path + "/..");
    }
}
