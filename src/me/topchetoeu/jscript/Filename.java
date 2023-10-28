package me.topchetoeu.jscript;

import java.io.File;

public class Filename {
    public final String protocol;
    public final String path;

    public String toString() {
        return protocol + "://" + path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + protocol.hashCode();
        result = prime * result + path.hashCode();
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        var other = (Filename) obj;

        if (protocol == null) {
            if (other.protocol != null) return false;
        }
        else if (!protocol.equals(other.protocol)) return false;

        if (path == null) {
            if (other.path != null) return false;
        }
        else if (!path.equals(other.path)) return false;
        return true;
    }

    public static Filename fromFile(File file) {
        return new Filename("file", file.getAbsolutePath());
    }


    public Filename(String protocol, String path) {
        this.protocol = protocol;
        this.path = path;
    }
}