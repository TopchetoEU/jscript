package me.topchetoeu.jscript.common.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class JSONList extends ArrayList<JSONElement> {
	public JSONList() {}
	public JSONList(JSONElement ...els) {
		super(Arrays.asList(els));
	}
	public JSONList(Collection<JSONElement> els) {
		super(els);
	}

	public JSONList addNull() { this.add(JSONElement.NULL); return this; }
	public JSONList add(String val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(double val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(boolean val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(Map<String, JSONElement> val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(Collection<JSONElement> val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(JSONMap val) { this.add(JSONElement.of(val)); return this; }
	public JSONList add(JSONList val) { this.add(JSONElement.of(val)); return this; }

}
