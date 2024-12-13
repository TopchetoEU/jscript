function split(val, limit) {
	var self = unwrapThis(this, "string", String, "String.prototype.split");
	if (val === undefined) return [self];
	if (val !== null && typeof val === "object" && Symbol.split in val) {
		return val[Symbol.split](self, limit);
	}
	val = String(val);
	var offset = 0;
	var res = [];
	while (true) {
		var start = string.indexOf(self, val, offset, false);
		if (start < 0) {
			res[res.length] = string.substring(self, offset, self.length);
			break;
		}
		var end = start + val.length;
		res[res.length] = string.substring(self, offset, start);
		offset = end;
	}
	return res;
}

split(print(), print());