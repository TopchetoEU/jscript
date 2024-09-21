package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.ClassNode;
import me.topchetoeu.jscript.compilation.JavaScript;

public class ClassValueNode extends ClassNode {
    public ClassValueNode(Location loc, Location end, String name, ClassBody body) {
        super(loc, end, name, body);
    }

    public static ParseRes<ClassValueNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "class")) return ParseRes.failed();
        n += 5;

        var name = Parsing.parseIdentifier(src, i + n);
        if (name.isSuccess() && !JavaScript.checkVarName(name.result)) {
            name = ParseRes.error(src.loc(i + n), "Unexpected keyword '" + name.result + "'");
        }
        n += name.n;

        var body = parseBody(src, i + n);
        if (!body.isSuccess()) return body.chainError(name).chainError(src.loc(i + n), "Expected a class body");
        n += body.n;

        return ParseRes.res(new ClassValueNode(loc, src.loc(i + n), name.result, body.result), n);
    }
}
