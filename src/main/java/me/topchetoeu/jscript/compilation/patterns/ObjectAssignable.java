package me.topchetoeu.jscript.compilation.patterns;

import java.util.List;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;

public class ObjectAssignable extends ObjectDestructor<AssignTarget> implements AssignTarget {
    @Override public void afterAssign(CompileResult target, boolean pollute) {
        compile(target, t -> t.assign(target, false), pollute);
    }

    public ObjectAssignable(Location loc, List<Member<AssignTarget>> members) {
        super(loc, members);
    }
}
