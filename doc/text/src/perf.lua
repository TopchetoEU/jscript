local ffi = require "ffi";

ffi.cdef [[
	typedef struct timeval {
		long tv_sec;
		long tv_usec;
	} timeval;

	int gettimeofday(struct timeval* t, void* tzp);
]];

local function now()
	local target = ffi.new "timeval";
	ffi.C.gettimeofday(target, nil);
	return tonumber(target.tv_sec) + tonumber(target.tv_usec) / 1000000;
end

local function measure(func, ...)
	local start = now();

	return (function(...)
		io.stderr:write(("Took %s seconds\n"):format(now() - start));
		return ...;
	end)(func(...));
end

return {
	now = now,
	measure = measure,
}