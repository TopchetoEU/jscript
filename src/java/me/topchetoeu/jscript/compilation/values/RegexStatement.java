package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class RegexStatement extends Statement {
    public final String pattern, flags;

    // Not really pure, since a function is called, but can be ignored.
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadRegex(pattern, flags));
        if (!pollute) target.add(Instruction.discard());
    }

    public static ParseRes<RegexStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        if (Parsing.inBounds(tokens, i)) {
            if (tokens.get(i).isRegex()) {
                var val = tokens.get(i).regex();
                var index = val.lastIndexOf('/');
                var first = val.substring(1, index);
                var second = val.substring(index + 1);
                return ParseRes.res(new RegexStatement(loc, first, second), 1);
            }
            else return ParseRes.failed();
        }
        return ParseRes.failed();
    }

    public RegexStatement(Location loc, String pattern, String flags) {
        super(loc);
        this.pattern = pattern;
        this.flags = flags;
    }
}
