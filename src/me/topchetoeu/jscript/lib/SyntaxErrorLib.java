package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.core.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("SyntaxError")
public class SyntaxErrorLib extends ErrorLib {
    @ExposeField public static final String __name = "SyntaxError";

    @ExposeConstructor public static ObjectValue __constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.SYNTAX_ERROR);
        return target;
    }
}