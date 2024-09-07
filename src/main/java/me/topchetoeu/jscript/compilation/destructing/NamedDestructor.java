package me.topchetoeu.jscript.compilation.destructing;

import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public interface NamedDestructor extends Destructor {
    String name();

    public static ParseRes<Destructor> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        ParseRes<Destructor> first = ParseRes.first(src, i + n,
            AssignDestructorNode::parse,
            VariableNode::parse
        );
        return first.addN(n);
    }
}
