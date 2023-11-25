package me.topchetoeu.jscript.permissions;

import java.util.LinkedList;

public class Permission {
    private static class State {
        public final int predI, trgI, wildcardI;
        public final boolean wildcard;

        @Override
        public String toString() {
            return "State [pr=%s;trg=%s;wildN=%s;wild=%s]".formatted(predI, trgI, wildcardI, wildcard);
        }

        public State(int predicateI, int targetI, int wildcardI, boolean wildcard) {
            this.predI = predicateI;
            this.trgI = targetI;
            this.wildcardI = wildcardI;
            this.wildcard = wildcard;
        }
    }

    public final String namespace;
    public final String value;

    public boolean match(Permission perm) {
        if (!Permission.match(namespace, perm.namespace, '.')) return false;
        if (value == null || perm.value == null) return true;
        return Permission.match(value, perm.value);
    }
    public boolean match(Permission perm, char delim) {
        if (!Permission.match(namespace, perm.namespace, '.')) return false;
        if (value == null || perm.value == null) return true;
        return Permission.match(value, perm.value, delim);
    }

    public boolean match(String perm) {
        return match(new Permission(perm));
    }
    public boolean match(String perm, char delim) {
        return match(new Permission(perm), delim);
    }

    @Override
    public String toString() {
        if (value != null) return namespace + ":" + value;
        else return namespace;
    }

    public Permission(String raw) {
        var i = raw.indexOf(':');

        if (i > 0) {
            value = raw.substring(i + 1);
            namespace = raw.substring(0, i);
        }
        else {
            value = null;
            namespace = raw;
        }
    }

    public static boolean match(String predicate, String target, char delim) {
        if (predicate.equals("")) return target.equals("");

        var queue = new LinkedList<State>();
        queue.push(new State(0, 0, 0, false));

        while (!queue.isEmpty()) {
            var state = queue.poll();
            var predEnd = state.predI >= predicate.length();

            if (state.trgI >= target.length()) return predEnd;
            var predC = predEnd ? 0 : predicate.charAt(state.predI);
            var trgC = target.charAt(state.trgI);

            if (state.wildcard) {
                if (state.wildcardI == 2 || trgC != delim) {
                    queue.add(new State(state.predI, state.trgI + 1, state.wildcardI, true));
                }
                queue.add(new State(state.predI, state.trgI, 0, false));
            }
            else if (predC == '*') {
                queue.add(new State(state.predI + 1, state.trgI, state.wildcardI + 1, false));
            }
            else if (state.wildcardI > 0) {
                if (state.wildcardI > 2) throw new IllegalArgumentException("Too many sequential stars.");
                queue.add(new State(state.predI, state.trgI, state.wildcardI, true));
            }
            else if (!predEnd && (predC == '?' || predC == trgC)) {
                queue.add(new State(state.predI + 1, state.trgI + 1, 0, false));
            }
        }

        return false;
    }
    public static boolean match(String predicate, String target) {
        if (predicate.equals("")) return target.equals("");

        var queue = new LinkedList<State>();
        queue.push(new State(0, 0, 0, false));

        while (!queue.isEmpty()) {
            var state = queue.poll();

            if (state.predI >= predicate.length() || state.trgI >= target.length()) {
                return state.predI >= predicate.length() && state.trgI >= target.length();
            }

            var predC = predicate.charAt(state.predI);
            var trgC = target.charAt(state.trgI);

            if (predC == '*') {
                queue.add(new State(state.predI, state.trgI + 1, state.wildcardI, true));
                queue.add(new State(state.predI + 1, state.trgI, 0, false));
            }
            else if (predC == '?' || predC == trgC) {
                queue.add(new State(state.predI + 1, state.trgI + 1, 0, false));
            }
        }

        return false;
    }
}
