package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeField;

@WrapperName("SyntaxError")
public class SyntaxErrorLib extends ErrorLib {
    @ExposeField public static final String __name = "SyntaxError";

    @ExposeConstructor public static ObjectValue __constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.SYNTAX_ERROR);
        return target;
    }
}