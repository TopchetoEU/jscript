package me.topchetoeu.j2s.compilation.values;

import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.common.parsing.ParseRes;
import me.topchetoeu.j2s.common.parsing.Parsing;
import me.topchetoeu.j2s.common.parsing.Source;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.members.FieldMemberNode;
import me.topchetoeu.j2s.compilation.members.Member;
import me.topchetoeu.j2s.compilation.members.PropertyMemberNode;
import me.topchetoeu.j2s.compilation.values.constants.NumberNode;
import me.topchetoeu.j2s.compilation.values.constants.StringNode;

public class ObjectNode extends Node {
	public final List<Member> members;

	@Override public void compileFunctions(CompileResult target) {
		for (var member : members) member.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		target.add(Instruction.loadObj());
		for (var el : members) el.compile(target, true);
	}

	public ObjectNode(Location loc, List<Member> map) {
		super(loc);
		this.members = map;
	}

	private static ParseRes<Node> parseComputePropName(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		if (!src.is(i + n, "[")) return ParseRes.failed();
		n++;

		var val = JavaScript.parseExpression(src, i, 0);
		if (!val.isSuccess()) return val.chainError(src.loc(i + n), "Expected an expression in compute property");
		n += val.n;
		n += Parsing.skipEmpty(src, i + n);

		if (!src.is(i + n, "]")) return ParseRes.error(src.loc(i + n), "Expected a closing bracket after compute property");
		n++;

		return ParseRes.res(val.result, n);
	}
	public static ParseRes<Node> parsePropName(Source src, int i) {
		return ParseRes.first(src, i,
			(s, j) -> {
				var m = Parsing.skipEmpty(s, j);
				var l = s.loc(j + m);

				var r = Parsing.parseIdentifier(s, j + m);
				if (r.isSuccess()) return ParseRes.res(new StringNode(l, r.result), r.n);
				else return r.chainError();
			},
			StringNode::parse,
			NumberNode::parse,
			ObjectNode::parseComputePropName
		);
	}

	public static ParseRes<ObjectNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "{")) return ParseRes.failed();
		n++;
		n += Parsing.skipEmpty(src, i + n);

		var members = new LinkedList<Member>();

		if (src.is(i + n, "}")) {
			n++;
			return ParseRes.res(new ObjectNode(loc, members), n);
		}

		while (true) {
			ParseRes<Member> prop = ParseRes.first(src, i + n,
				PropertyMemberNode::parse,
				FieldMemberNode::parse
			);
			if (!prop.isSuccess()) return prop.chainError(src.loc(i + n), "Expected a member in object literal");
			n += prop.n;

			members.add(prop.result);

			n += Parsing.skipEmpty(src, i + n);
			if (src.is(i + n, ",")) {
				n++;
				n += Parsing.skipEmpty(src, i + n);

				if (src.is(i + n, "}")) {
					n++;
					break;
				}

				continue;
			}
			else if (src.is(i + n, "}")) {
				n++;
				break;
			}
			else ParseRes.error(src.loc(i + n), "Expected a comma or a closing brace.");
		}
	
		return ParseRes.res(new ObjectNode(loc, members), n);
	}
}
