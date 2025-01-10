import coffeescript from "./coffeescript.ts";
import babel from "./babel.ts";

register(v => coffeescript(babel(v)));
