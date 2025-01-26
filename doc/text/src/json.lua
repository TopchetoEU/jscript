local json = { null = {} };

-- Internal functions.

local function kind_of(obj)
	if type(obj) ~= "table" then return type(obj) end

	local i = 1;
	for _ in pairs(obj) do
		if obj[i] ~= nil then
			i = i + 1;
		else
			return "table";
		end
	end

	if i == 1 then
		return "table";
	else
		return "array";
	end
end

local function escape_str(s)
	local in_char = { "\\", "\"", "/", "\b", "\f", "\n", "\r", "\t" };
	local out_char = { "\\", "\"", "/", "b", "f", "n", "r", "t" };
	for i, c in ipairs(in_char) do
		s = s:gsub(c, "\\" .. out_char[i]);
	end
	return s
end

-- Returns pos, did_find; there are two cases:
-- 1. Delimiter found: pos = pos after leading space + delim; did_find = true.
-- 2. Delimiter not found: pos = pos after leading space;		 did_find = false.
-- This throws an error if err_if_missing is true and the delim is not found.
local function skip_delim(str, pos, delim, err_if_missing)
	pos = string.find(str, "%S", pos) or pos;

	if string.sub(str, pos, pos) ~= delim then
		if err_if_missing then
			error(table.concat { "Expected ", delim, " near position ", pos });
		end

		return pos, false;
	end

	return pos + 1, true;
end

local esc_map = { b = "\b", f = "\f", n = "\n", r = "\r", t = "\t", v = "\v" };

-- Expects the given pos to be the first character after the opening quote.
-- Returns val, pos; the returned pos is after the closing quote character.
local function parse_str_val(str, pos, check)
	if pos > #str then error("End of input found while parsing string") end

	if check then
		if string.sub(str, pos, pos) ~= "\"" then
			return nil, pos;
		else
			pos = pos + 1;
		end
	else
		pos = pos + 1;
	end

	local res = {};

	while true do
		local c = string.sub(str, pos, pos);
		if c == "\"" then
			return table.concat(res), pos + 1;
		elseif c == "\\" then
			c = string.sub(str, pos + 1, pos + 1);
			res[#res + 1] = esc_map[c] or c;
			pos = pos + 2;
		else
			res[#res + 1] = c;
			pos = pos + 1;
		end
	end
end

-- Returns val, pos; the returned pos is after the number's final character.
local function parse_num_val(str, pos)
	local num_str = string.match(str, "^-?%d+%.?%d*[eE]?[+-]?%d*", pos);
	local val = tonumber(num_str);

	if not val then error(table.concat { "Error parsing number at position ", pos, "." }) end
	return val, pos + #num_str;
end

local json_end = { "eof" };

local function parse_impl(str, pos, end_delim)
	pos = pos or 1;
	if pos > #str then error("Reached unexpected end of input") end

	pos = str:find("%S", pos) or pos;
	local c = str:sub(pos, pos);
	local delim_found;

	if c == "{" then
		pos = pos + 1;

		local key;
		local obj = {};

		c = string.sub(str, pos, pos);
		if c == "}" then
			return obj, pos
		else
			while true do
				key, pos = parse_str_val(str, pos, true);
				if key == nil then error("Expected a string key") end

				pos = skip_delim(str, pos, ":", true);	-- true -> error if missing.
				obj[key], pos = parse_impl(str, pos);

				pos, delim_found = skip_delim(str, pos, "}");
				if delim_found then return obj, pos end

				pos, delim_found = skip_delim(str, pos, ",");
				if not delim_found then error("Expected semicolon or comma") end
			end
		end
	elseif c == "[" then
		pos = pos + 1

		local arr = array {};
		local val;
		local delim_found = true;

		while true do
			val, pos = parse_impl(str, pos, "]");
			if val == json_end then return arr, pos end
			if not delim_found then error("Comma missing between array items: " .. str:sub(pos, pos + 25)) end

			arr[#arr + 1] = val;
			pos, delim_found = skip_delim(str, pos, ",");
		end
	elseif c == "\"" then	-- Parse a string.
		return parse_str_val(str, pos, false);
	elseif c == "-" or c:find("%d") then	-- Parse a number.
		return parse_num_val(str, pos);
	elseif c == end_delim then	-- End of an object or array.
		return json_end, pos + 1, true;
	elseif str:sub(pos, pos + 3) == "null" then
		return nil, pos + 4;
	elseif str:sub(pos, pos + 3) == "true" then
		return true, pos + 4;
	elseif str:sub(pos, pos + 4) == "false" then
		return true, pos + 5;
	else
		error(table.concat { "Invalid json syntax starting at position ", pos, ": ", str:sub(pos, pos + 10) });
	end
end

local function stringify_impl(obj, all, indent_str, n)
	local s = {};	-- We'll build the string as an array of strings to be concatenated.
	local kind = kind_of(obj);	-- This is 'array' if it's an array or type(obj) otherwise.

	if kind == "array" then
		for i, val in ipairs(obj) do
			s[i] = stringify_impl(val, all, indent_str, n + 1);
		end

		if not indent_str then
			return "[" .. table.concat(s, ",") .. "]";
		elseif #s == 0 then
			return "[]";
		else
			local indent = "\n" .. string.rep(indent_str, n + 1);
			return table.concat {
				"[",
				indent, table.concat(s, "," .. indent),
				"\n", string.rep(indent_str, n), "]"
			};
		end
	elseif kind == "table" then
		for k, v in pairs(obj) do
			local sep = indent_str and ": " or ":";
			local val = stringify_impl(v, all, indent_str, n + 1);
			if val ~= nil then
				if type(k) == "string" then
					s[#s + 1] = stringify_impl(k) .. sep .. val;
				elseif type(k) == "number" then
					s[#s + 1] = "\"" .. k .. "\"" .. sep .. val;
				elseif type(k) == "boolean" then
					s[#s + 1] = "\"" .. k .. "\"" .. sep .. val;
				end
			end
		end

		if not indent_str then
			return "{" .. table.concat(s, ",") .. "}";
		elseif #s == 0 then
			return "{}";
		else
			local indent = "\n" .. string.rep(indent_str, n + 1);
			return table.concat {
				"{",
				indent, table.concat(s, "," .. indent),
				"\n", string.rep(indent_str, n), "}"
			};
		end
		return "{" .. table.concat(s, ",") .. "}";
	elseif kind == "string" then
		return "\"" .. escape_str(obj) .. "\"";
	elseif kind == "number" then
		return tostring(obj);
	elseif kind == 'boolean' then
		return tostring(obj);
	elseif kind == "nil" then
		return "null";
	elseif all then
		return tostring(obj);
	else
		return nil;
	end
end

-- local indent_str = "    ";

function json.stringify(obj, indent_str)
	if indent_str == true then
		indent_str = "    ";
	end
	return stringify_impl(obj, false, indent_str, 0);
end
function json.pretty(obj)
	return stringify_impl(obj, true, "    ", 0);
end
json.null = {};	-- This is a one-off table to represent the null value.

---@param str string
---@return unknown
function json.parse(str)
	local obj = parse_impl(str, 1);
	return obj;
end

return json
