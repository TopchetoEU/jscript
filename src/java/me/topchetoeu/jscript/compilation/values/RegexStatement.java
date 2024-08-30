package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    // Not really pure, since a function is called, but can be ignored.
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadRegex(pattern, flags));
        if (!pollute) target.add(Instruction.discard());
    }


    public static ParseRes<RegexStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, '/')) return ParseRes.failed();
        var loc = src.loc(i + n);
        n++;

        var source = new StringBuilder();
        var flags = new StringBuilder();

        var inBrackets = false;

        while (true) {
            if (src.is(i + n, '[')) {
                n++;
                inBrackets = true;
                source.append(src.at(i + n));
                continue;
            }
            else if (src.is(i + n, ']')) {
                n++;
                inBrackets = false;
                source.append(src.at(i + n));
                continue;
            }
            else if (src.is(i + n, '/') && !inBrackets) {
                n++;
                break;
            }

            var charRes = Parsing.parseChar(src, i + n);
            if (charRes.result == null) return ParseRes.error(src.loc(i + n), "Multiline regular expressions are not allowed");
            source.append(charRes.result);
            n++;
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

        return ParseRes.res(new RegexStatement(loc, source.toString(), flags.toString()), n);
    }

    public RegexStatement(Location loc, String pattern, String flags) {
        super(loc);
        this.pattern = pattern;
        this.flags = flags;
    }
}
