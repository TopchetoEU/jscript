-- TILL - TopchetoEU's "immaculate" lua libs
-- Some useful utilities every lua-er should use.
-- Might break shit, don't use in production for crying out loud

-- Reimplement this for lua <5.1
if table.move == nil then
	--- @diagnostic disable: duplicate-set-field
	function table.move(src, src_b, src_e, dst_b, dst)
		if dst == nil then dst = src end
		local offset = dst_b - src_b;

		if dst_b < src_b then
			for i = src_e, src_b, -1 do
				dst[i + offset] = src[i];
			end
		else
			for i = src_b, src_e do
				dst[i + offset] = src[i];
			end
		end

		return dst;
	end
end

-- Why did we *remove* this from the spec again? Classical PUC
unpack = table.unpack or unpack;
table.unpack = unpack;

--- Prepares the object to be a class - puts an __index member in it pointing to the object itself
--- @generic T
--- @param obj T
--- @return T | { __index: T }
function class(obj)
	--- @diagnostic disable-next-line: inject-field
	obj.__index = obj;
	return obj;
end

-- arrays

--- @alias array<T> T[] | arraylib

--- Converts the object to an array by putting "arrays" as its metatable
--- @generic T: unknown
--- @param obj T[]
--- @return array<T>
--- @overload fun(val: string): array<string>
function array(obj)
	if type(obj) == "string" then
		return obj:split "";
	else
		return arrays.mk(obj);
	end
end

--- @class arraylib
arrays = class {};

--- Creates an array
function arrays.mk(obj)
	return setmetatable(obj, arrays);
end

--- Adds every element of every passed array to the end of this array
function arrays:append(...)
	local res = self;
	local n = #res + 1;

	for i = 1, select("#", ...) do
		local curr = select(i, ...);
		for j = 1, #curr do
			res[n] = curr[j];
			n = n + 1;
		end
	end

	return res;
end

--- Returns all the given arrays, concatenated to one
function arrays.concat(...)
	--- @diagnostic disable-next-line: missing-fields
	return arrays.append(array {}, ...);
end

--- Adds all the given elements to the end of this array
function arrays:push(...)
	return self:append({ ... });
end
--- Removes the last element of the array and returns it
--- @generic T
--- @param self array<T>
--- @return T val? The removed element, or nil if none
function arrays:pop()
	local res = self[#self];
	self[#self] = nil;
	return res;
end
--- @generic T
--- @param self array<T>
--- @return T val? The last element of this array, or nil of none
function arrays:peek()
	return self[#self];
end

--- Returns the result of mapping the values in table t through the function f
--- @param mutate boolean? If true, will operate directly on the given array
function arrays:map(f, mutate)
	local out;

	if mutate then
		out = self;
	else
		out = array {};
	end

	for i = 1, #self do
		out[i] = f(self[i], i, self);
	end

	return out;
end

--- Finds the index of the given element, or nil if it doesn't exist
function arrays:find_i(f)
	for i = 1, #self do
		if f(self[i], i, self) then
			return i;
		end
	end

	return nil;
end

--- Like arrays:map, but will expect the function to return arrays, and will :append them to the result array instead
function arrays:flat_map(f)
	local out = array {};

	for i = 1, #self do
		out:append(f(self[i], i, self));
	end

	return out;
end

--- Sets each value from b to e to val in the given array
function arrays:fill(val, b, e)
	if b == nil then b = 1 end
	if e == nil then e = self end
	if b < 0 then b = #self + 1 - b end
	if e < 0 then e = #self + 1 - e end

	for i = b, e do
		self[i] = val;
	end

	return self;
end

--- Every element from start to stop is removed from this array and are replaced with the given elements
function arrays:splice(start, stop, ...)
	-- TODO: optimize
	if select("#") > 0 then
		local n = stop - start + 1;

		while n > 0 do
			table.remove(self, start);
			n = n - 1;
		end

		for i = 1, select("#") do
			table.insert(self, start, (select(i, ...)));
		end

		return self;
	else
		local res = {};

		for i = start, stop do
			table.insert(res, self[i]);
		end

		return res;
	end
end

--- Every element from start to stop is removed from this array and are replaced with the given elements
function arrays:slice(start, stop)
	start = start or 1;
	stop = stop or #self;
	local res = array {};

	for i = start, stop do
		res:push(self[i]);
	end

	return res;
end

--- Equivalent of table.concat { table.unpack(self) }
--- @param sep string? Separator (defaults to empty)
--- @param b number? First element to take (defaults to beginning)
--- @param e number? Last element to take (defaults to end)
function arrays:join(sep, b, e)
	return table.concat(self, sep, b, e);
end

function arrays:__concat(other)
	return arrays.concat(self, other);
end

-- strings

--- Splits the text into an array of separate lines.
--- @param self string
function string:split(sep)
	sep = sep or "";
	local lines = arrays {};
	local pos = 1;

	if sep == "" then
		for i = 1, #self do
			lines:push(self:sub(1, 1));
		end
	else
		while true do
			local b, e = self:find(sep, pos);

			if not b then
				table.insert(lines, self:sub(pos));
				break;
			else
				table.insert(lines, self:sub(pos, b - 1));
				pos = e + 1;
			end
		end
	end

	return lines;
end
--- Gets the nth character
--- @param self string
--- @param i number
function string:at(i)
	return self:sub(i, i);
end
--- Performs a plain string replace
--- @param self string
--- @param old string
--- @param new string
function string:replace(old, new)
	local b, e = self:find(old, 1, true);

	if b == nil then
		return self;
	else
		return self:sub(1, b - 1) .. new .. self:sub(e + 1);
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

local function stringify_impl(obj, indent_str, n, passed)
	local s = {};	-- We'll build the string as an array of strings to be concatenated.
	local kind = type(obj);	-- This is 'array' if it's an array or type(obj) otherwise.

	if kind == "table" then
		if passed[obj] then return "<circular>" end
		passed[obj] = true;

		local len = #obj;

		for i = 1, len do
			s[i] = stringify_impl(obj[i], indent_str, n + 1, passed) .. ",";
		end

		local keys = {};

		for k, v in pairs(obj) do
			if type(k) ~= "number" or k > len then
				keys[#keys + 1] = { k, v };
			end
		end

		table.sort(keys, function (a, b)
			if type(a[1]) == "number" and type(b[1]) == "number" then
				return a[1] < b[1];
			elseif type(a[1]) == "string" and type(b[1]) == "string" then
				return a[1] < b[1];
			else
				return type(a[1]) < type(b[1]) or tostring(a[1]) < tostring(b[1]);
			end
		end);

		for i = 1, #keys do
			local k = keys[i][1];
			local v = keys[i][2];

			local val = stringify_impl(v, indent_str, n + 1, passed);
			if val ~= nil then
				if type(k) == "string" then
					s[#s + 1] = table.concat { k, ": ", val, "," };
				else
					s[#s + 1] = table.concat { "[", stringify_impl(k, indent_str, n + 1, passed), "]: ", val, "," };
				end
			end
		end

		local meta = getmetatable(obj);
		if meta ~= nil and meta ~= arrays then
			s[#s + 1] = "<meta> = " .. stringify_impl(meta, indent_str, n + 1, passed) .. ",";
		end

		passed[obj] = false;

		if #s == 0 then
			if meta == arrays then
				return "[]";
			else
				return "{}";
			end
		end

		local contents = table.concat(s, " "):sub(1, -2);
		if #contents > 80 then
			local indent = "\n" .. string.rep(indent_str, n + 1);

			contents = table.concat {
				indent,
				table.concat(s, indent),
				"\n", string.rep(indent_str, n)
			};
		else
			contents = " " .. contents .. " ";
		end

		if meta == arrays then
			return table.concat { "[", contents, "]" };
		else
			return table.concat { "{", contents, "}" };
		end
	elseif kind == "string" then
		return "\"" .. escape_str(obj) .. "\"";
	elseif kind == "function" then
		local data = debug.getinfo(obj, "S");
		return table.concat { tostring(obj), " @ ", data.short_src, ":", data.linedefined };
	elseif kind == "nil" then
		return "null";
	else
		return tostring(obj);
	end
end

--- Turns the given value to a human-readable string.
--- Should be used only for debugging and display purposes
function to_readable(obj, indent_str)
	return stringify_impl(obj, indent_str or "    ", 0, {});
end

function print(...)
	for i = 1, select("#", ...) do
		if i > 1 then
			io.stderr:write("\t");
		end

		io.stderr:write(tostring((select(i, ...))));
	end

	io.stderr:write("\n");
end

--- Prints the given values in a human-readable manner to stderr
--- Should be used only for debugging
function pprint(...)
	for i = 1, select("#", ...) do
		if i > 1 then
			io.stderr:write("\t");
		end

		io.stderr:write(to_readable((select(i, ...))));
	end

	io.stderr:write("\n");
end

-- functions

--- @class functions
functions = class {};

--- Constructs a function, such that it calls the first function with the passed arguments,
--- the second function with the return of the first, and so on. The return value of the last function is returned
--- 
--- In short, does the following, if the passed functions are a, b and c: return c(b(a(...)))
--- 
--- Sometimes less cumbersome to write (a | b | c | d)(args...) than d(c(b(a(args...))))
--- @param self function
function functions:pipe(...)
	if ... == nil then
		return self;
	else
		local next = ...;

		return functions.pipe(function (...)
			return next(self(...));
		end, select(2, ...));
	end
end
--- Calls pipe with a and b; Alternative syntax for older Lua installation
function functions.__sub(a, b)
	return functions.pipe(a, b);
end
--- Calls pipe with a and b
function functions.__bor(a, b)
	return functions.pipe(a, b);
end

-- It's not vital to have this metatable, so we will just try our very best
if debug then
	debug.setmetatable(load, functions);
end
