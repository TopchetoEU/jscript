package me.topchetoeu.jscript.compilation.patterns;

public interface NamedDestructor extends Pattern {
    String name();

    // public static ParseRes<Destructor> parse(Source src, int i) {
    //     var n = Parsing.skipEmpty(src, i);

    //     ParseRes<Destructor> first = ParseRes.first(src, i + n,
    //         AssignDestructorNode::parse,
    //         VariableNode::parse
    //     );
    //     return first.addN(n);
    // }
}
