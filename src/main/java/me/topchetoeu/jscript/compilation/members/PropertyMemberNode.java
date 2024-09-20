package me.topchetoeu.jscript.compilation.members;

import java.util.Arrays;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.FunctionNode;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.Parameters;
import me.topchetoeu.jscript.compilation.patterns.Pattern;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class PropertyMemberNode extends FunctionNode implements Member{
    public final Node key;
    public final Pattern argument;

    @Override public String name() {
        if (key instanceof StringNode str) {
            if (isGetter()) return "get " + str.value;
            else return "set " + str.value;
        }
        else return null;
    }

    public boolean isGetter() { return argument == null; }
    public boolean isSetter() { return argument != null; }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (pollute) target.add(Instruction.dup());
        key.compile(target, true);

        var id = target.addChild(compileBody(target, name, null));
        target.add(_i -> Instruction.loadFunc(id, true, false, false, name, captures(id, target)));
    }


    @Override public void compile(CompileResult target, boolean pollute, boolean enumerable) {
        compile(target, pollute);
        target.add(Instruction.defProp(isSetter(), enumerable));
    }

    public PropertyMemberNode(Location loc, Location end, Node key, Pattern argument, CompoundNode body) {
        super(loc, end, argument == null ? new Parameters(Arrays.asList()) : new Parameters(Arrays.asList(argument)), body);
        this.key = key;
        this.argument = argument;
    }

    public static ParseRes<PropertyMemberNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var access = Parsing.parseIdentifier(src, i + n);
        if (!access.isSuccess()) return ParseRes.failed();
        if (!access.result.equals("get") && !access.result.equals("set")) return ParseRes.failed();
        n += access.n;

        var name = ObjectNode.parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a property name after '" + access + "'");
        n += name.n;

        var params = Parameters.parseParameters(src, i + n);
        if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
        if (access.result.equals("get") && params.result.params.size() != 0) return ParseRes.error(src.loc(i + n), "Getter must not have any parameters");
        if (access.result.equals("set") && params.result.params.size() != 1) return ParseRes.error(src.loc(i + n), "Setter must have exactly one parameter");
        if (params.result.rest != null) return ParseRes.error(params.result.rest.loc(), "Property members may not have rest arguments");
        n += params.n;

        var body = CompoundNode.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
        n += body.n;

        var end = src.loc(i + n - 1);

        return ParseRes.res(new PropertyMemberNode(
            loc, end, name.result, access.result.equals("get") ? null : params.result.params.get(0), body.result
        ), n);
    }
}