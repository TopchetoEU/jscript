package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeField;
import me.topchetoeu.jscript.interop.ExposeTarget;

@WrapperName("RangeError")
public class RangeErrorLib extends ErrorLib {
    @ExposeField(target = ExposeTarget.STATIC)
    public static final String __name = "RangeError";

    @ExposeConstructor
    public static ObjectValue constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.RANGE_ERROR);
        return target;
    }
}