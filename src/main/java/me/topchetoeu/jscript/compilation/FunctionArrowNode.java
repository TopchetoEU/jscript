package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.control.ReturnNode;

public class FunctionArrowNode extends FunctionNode {
    @Override public String name() { return null; }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        var id = target.addChild(compileBody(target, false, name, null));
        target.add(_i -> Instruction.loadFunc(id, true, false, true, null, captures(id, target)));
    }

    public FunctionArrowNode(Location loc, Location end, Parameters params, Node body) {
        super(loc, end, params, expToBody(body));
    }

    private static final CompoundNode expToBody(Node node) {
        if (node instanceof CompoundNode res) return res;
        else return new CompoundNode(node.loc(), new ReturnNode(node.loc(), node));
    }

    public static ParseRes<FunctionArrowNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        Parameters params;

        if (src.is(i + n, "(")) {
            var paramsRes = JavaScript.parseParameters(src, i + n);
            if (!paramsRes.isSuccess()) return paramsRes.chainError();
            n += paramsRes.n;
            n += Parsing.skipEmpty(src, i + n);

            params = paramsRes.result;
        }
        else {
            var singleParam = Parsing.parseIdentifier(src, i + n);
            if (!singleParam.isSuccess()) return ParseRes.failed();

            var paramLoc = src.loc(i + n);
            n += singleParam.n;
            n += Parsing.skipEmpty(src, i + n);

            params = new Parameters(List.of(new Parameter(paramLoc, singleParam.result, null)));
        }

        if (!src.is(i + n, "=>")) return ParseRes.failed();
        n += 2;

        ParseRes<Node> body = ParseRes.first(src, i + n,
            (s, j) -> JavaScript.parseExpression(s, j, 2),
            CompoundNode::parse
        );
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected an expression or a compount statement after '=>'");
        n += body.n;

        return ParseRes.res(new FunctionArrowNode(
            loc, src.loc(i + n - 1),
            params, body.result
        ), n);
    }
}
