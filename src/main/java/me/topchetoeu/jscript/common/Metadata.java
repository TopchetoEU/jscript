package me.topchetoeu.jscript.common;

import me.topchetoeu.jscript.common.json.JSON;

public class Metadata {
    private static final String VERSION;
    private static final String AUTHOR;
    private static final String NAME;

    static {
        var data = JSON.parse(null, Reading.resourceToString("metadata.json")).map();
        VERSION = data.string("version");
        AUTHOR = data.string("author");
        NAME = data.string("name");
    }

    public static String version() {
        if (VERSION.equals("$" + "{VERSION}")) return "1337-devel";
        else return VERSION;
    }
    public static String author() {
        if (AUTHOR.equals("$" + "{AUTHOR}")) return "anonymous";
        else return AUTHOR;
    }
    public static String name() {
        if (NAME.equals("$" + "{NAME}")) return "some-product";
        else return NAME;
    }
}
