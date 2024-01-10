package me.topchetoeu.jscript.utils.filesystem;

public enum EntryType {
    NONE("none"),
    FILE("file"),
    FOLDER("folder");

    public final String name;

    private EntryType(String name) {
        this.name = name;
    }
}