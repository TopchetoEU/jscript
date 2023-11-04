package me.topchetoeu.jscript;

public class Location implements Comparable<Location> {
    public static final Location INTERNAL = new Location(0, 0, new Filename("jscript", "native"));
    private int line;
    private int start;
    private Filename filename;

    public int line() { return line; }
    public int start() { return start; }
    public Filename filename() { return filename; }

    @Override
    public String toString() {
        return filename.toString() + ":" + line + ":" + start;
    }

    public Location add(int n, boolean clone) {
        if (clone) return new Location(line, start + n, filename);
        this.start += n;
        return this;
    }
    public Location add(int n) {
        return add(n, false);
    }
    public Location nextLine() {
        line++;
        start = 0;
        return this;
    }
    public Location clone() {
        return new Location(line, start, filename);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + line;
        result = prime * result + start;
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Location other = (Location) obj;
        if (line != other.line) return false;
        if (start != other.start) return false;
        if (filename == null && other.filename != null) return false;
        else if (!filename.equals(other.filename)) return false;
        return true;
    }

    @Override
    public int compareTo(Location other) {
        int a = filename.toString().compareTo(other.filename.toString());
        int b = Integer.compare(line, other.line);
        int c = Integer.compare(start, other.start);

        if (a != 0) return a;
        if (b != 0) return b;
        return c;
    }

    public Location(int line, int start, Filename filename) {
        this.line = line;
        this.start = start;
        this.filename = filename;
    }
}
