package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.core.engine.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("TypeError")
public class TypeErrorLib extends ErrorLib {
    @ExposeField public static final String __name = "TypeError";

    @ExposeConstructor public static ObjectValue __constructor(Arguments args) {
        var target = ErrorLib.__constructor(args);
        target.setPrototype(PlaceholderProto.TYPE_ERROR);
        return target;
    }
}