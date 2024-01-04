package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeField;

@WrapperName("TypeError")
public class TypeErrorLib extends ErrorLib {
    @ExposeField public static final String __name = "TypeError";

    @ExposeConstructor public static ObjectValue __constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.TYPE_ERROR);
        return target;
    }
}