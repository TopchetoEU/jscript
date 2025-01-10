import { func } from "../stdlib/primordials.ts";

export default function _classCallCheck() {
	if (func.invokeTypeInfer() !== "new") throw new TypeError("Cannot call a class as a function");
}