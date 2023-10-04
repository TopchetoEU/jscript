package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public interface Filesystem {
    File open(String path) throws IOException, InterruptedException;
    boolean mkdir(String path) throws IOException, InterruptedException;
    EntryType type(String path) throws IOException, InterruptedException;
    boolean rm(String path) throws IOException, InterruptedException;
}
