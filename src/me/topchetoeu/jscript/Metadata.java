package me.topchetoeu.jscript;

public class Metadata {
    private static final String VERSION = "${VERSION}";
    private static final String AUTHOR = "${AUTHOR}";
    private static final String NAME = "${NAME}";

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
