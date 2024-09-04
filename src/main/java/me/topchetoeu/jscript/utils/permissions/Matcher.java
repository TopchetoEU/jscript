package me.topchetoeu.jscript.utils.permissions;

import java.util.LinkedList;

public interface Matcher {
    static class State {
        public final int predI, trgI, wildcardI;
        public final boolean wildcard;

        @Override
        public String toString() {
            return String.format("State [pr=%s;trg=%s;wildN=%s;wild=%s]", predI, trgI, wildcardI, wildcard);
        }

        public State(int predicateI, int targetI, int wildcardI, boolean wildcard) {
            this.predI = predicateI;
            this.trgI = targetI;
            this.wildcardI = wildcardI;
            this.wildcard = wildcard;
        }
    }

    boolean match(String predicate, String value);

    public static Matcher fileWildcard() {
        return (predicate, value) -> execWildcard(predicate, value, '/');
    }
    public static Matcher namespaceWildcard() {
        return (predicate, value) -> execWildcard(predicate, value, '.');
    }
    public static Matcher wildcard() {
        return (predicate, value) -> execWildcard(predicate, value, '\0');
    }

    public static boolean execWildcard(String predicate, String target, char delim) {
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
}
