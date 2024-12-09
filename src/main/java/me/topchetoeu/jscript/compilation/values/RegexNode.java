package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class RegexNode extends Node {
	public final String pattern, flags;

	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		target.add(Instruction.loadRegex(pattern, flags));
		if (!pollute) target.add(Instruction.discard());
	}

	public static ParseRes<RegexNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);

		if (!src.is(i + n, '/')) return ParseRes.failed();
		var loc = src.loc(i + n);
		n++;

		var source = new StringBuilder();
		var flags = new StringBuilder();

		var inBrackets = false;

		loop: while (true) {
			switch (src.at(i + n)) {
				case '[':
					inBrackets = true;
					source.append('[');
					n++;
					continue;
				case ']':
					inBrackets = false;
					source.append(']');
					n++;
					continue;
				case '/':
					n++;
					if (inBrackets) {
						source.append('/');
						continue;
					}
					else break loop;
				case '\\':
					source.append('\\');
					source.append(src.at(i + n + 1));
					n += 2;
					break;
				default:
					source.append(src.at(i + n));
					n++;
					break;
			}
		}

		while (true) {
			char c = src.at(i + n, '\0');

			if (src.is(i + n, v -> Parsing.isAny(c, "dgimsuy"))) {
				if (flags.indexOf(c + "") >= 0) return ParseRes.error(src.loc(i + n), "The flags of a regular expression may not be repeated");
				flags.append(c);
			}
			else break;

			n++;
		}

		return ParseRes.res(new RegexNode(loc, source.toString(), flags.toString()), n);
	}

	public RegexNode(Location loc, String pattern, String flags) {
		super(loc);
		this.pattern = pattern;
		this.flags = flags;
	}
}
