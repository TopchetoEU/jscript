package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.core.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("RangeError")
public class RangeErrorLib extends ErrorLib {
    @ExposeField public static final String __name = "RangeError";

    @ExposeConstructor public static ObjectValue constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.RANGE_ERROR);
        return target;
    }
}