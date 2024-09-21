package me.topchetoeu.jscript.compilation.members;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.FunctionNode;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.Parameters;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class MethodMemberNode extends FunctionNode implements Member {
    public final Node key;

    @Override public String name() {
        if (key instanceof StringNode str) return str.value;
        else return null;
    }

    @Override protected Environment rootEnv(Environment env) {
        return env;
    }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        if (pollute) target.add(Instruction.dup());
        key.compile(target, true);

        var id = target.addChild(compileBody(target, name, null));
        target.add(_i -> Instruction.loadFunc(id, true, false, false, false, null, captures(id, target)));
    }

    @Override public void compile(CompileResult target, boolean pollute, boolean enumerable) {
        compile(target, pollute);
        target.add(Instruction.defField(enumerable));
    }

    public MethodMemberNode(Location loc, Location end, Node key, Parameters params, CompoundNode body) {
        super(loc, end, params, body);
        this.key = key;
    }

    public static ParseRes<MethodMemberNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var name = ObjectNode.parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError();
        n += name.n;

        var params = Parameters.parseParameters(src, i + n);
        if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
        n += params.n;

        var body = CompoundNode.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
        n += body.n;

        var end = src.loc(i + n - 1);

        return ParseRes.res(new MethodMemberNode(
            loc, end, name.result, params.result, body.result
        ), n);
    }
}