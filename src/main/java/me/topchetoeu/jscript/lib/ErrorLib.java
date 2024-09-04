package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.exceptions.ConvertException;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.runtime.values.ObjectValue.PlaceholderProto;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Error")
public class ErrorLib {
    private static String toString(Context ctx, Object name, Object message) {
        if (name == null) name = "";
        else name = Values.toString(ctx, name).trim();
        if (message == null) message = "";
        else message = Values.toString(ctx, message).trim();
        StringBuilder res = new StringBuilder();

        if (!name.equals("")) res.append(name);
        if (!message.equals("") && !name.equals("")) res.append(": ");
        if (!message.equals("")) res.append(message);

        return res.toString();
    }

    @ExposeField public static final String __name = "Error";

    @Expose public static String __toString(Arguments args) {
        if (args.self instanceof ObjectValue) return toString(args.ctx,
            Values.getMember(args.ctx, args.self, "name"),
            Values.getMember(args.ctx, args.self, "message")
        );
        else return "[Invalid error]";
    }

    @ExposeConstructor public static ObjectValue __constructor(Arguments args) {
        var target = new ObjectValue();
        var message = args.getString(0, "");

        try {
            target = args.self(ObjectValue.class);
        }
        catch (ConvertException e) {}

        target.setPrototype(PlaceholderProto.ERROR);
        target.defineProperty(args.ctx, "message", Values.toString(args.ctx, message));

        return target;
    }
}
