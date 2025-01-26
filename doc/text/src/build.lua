---@diagnostic disable: undefined-field
require "src.utils";
local json = require "src.json";

local function reader(fd, ...)
	if type(fd) == "string" then
		fd = assert(io.open(fd, ... or "r"));
	elseif type(fd) == "function" then
		return fd;
	else
		assert(fd, ...);
	end

	return function ()
		if fd == nil then error "File is closed" end
		local res = fd:read(1024);

		if res == nil then
			fd:close();
			fd = nil;
			return nil;
		else
			return res;
		end
	end;
end

local function read_to_str(fd, ...)
	local res = {};

	for val in reader(fd, ...) do
		res[#res + 1] = val;
	end

	return table.concat(res);
end

local flag = {};
function flag.has(name, ...)
	if name == ... then
		return true;
	elseif ... == nil then
		return false;
	else
		return flag.has(name, select(2, ...));
	end
end
function flag.add(name, ...)
	if flag.has(name, ...) then
		return ...;
	else
		return name, ...;
	end
end

local converters = {};

local function sanitize(str)
	return (str
		:gsub("<", "&lt")
		:gsub(">", "&gt")
	);
end

local function get_indent(ctx, n)
	return ("\t"):rep((ctx.indent or 0) + (n or 0));
end
local function indent(ctx)
	ctx.indent = (ctx.indent or 0) + 1;
end
local function undent(ctx)
	ctx.indent = (ctx.indent or 1) - 1;
end

local function convert(data, ctx, ...)
	local func = converters[data.t];
	if func == nil then
		error(table.concat { "Unknown node '", data.t, "': ", to_readable(data) });
	else
		return func(data.c, ctx, ...);
	end
end
local function convert_all(arr, ctx, ...)
	local res = array {};
	local plain = array {};
	local inline = true;

	for i = 1, #arr do
		local a, b, c = convert(arr[i], ctx, ...);
		res:append(a);
		plain:append(b);

		if not c then
			inline = false;
			res:append("\n", get_indent(ctx));
		end
	end

	return res, plain, inline;
end

local function make_id(id, readable, ctx)
	ctx.ids = ctx.ids or {};
	local res;

	if ctx.ids[id] ~= nil then
		ctx.ids[id] = ctx.ids[id] + 1;
		res = id .. "-" .. ctx.ids[id];
	else
		ctx.ids[id] = 0;
		res = id;
	end

	if readable then
		ctx.citables = ctx.citables or {};
		ctx.citables[id] = readable;
	end

	return res;
end
local function last_id(id, ctx)
	ctx.ids = ctx.ids or {};
	if ctx.ids[id] ~= nil and ctx.ids[id] > 0 then
		return id .. "-" .. ctx.ids[id];
	else
		return id;
	end
end

local function count_headers(array)
	local res = 0;

	for i = 1, #array do
		if not array[i].ignored then
			res = res + 1;
		end
	end

	return res;
end

local function read_attributes(data, readable, ctx, no_id)
	local res = {};

	if data[1] ~= "" then
		res.id = data[1];
	end
	for i = 1, #data[2] do
		res[data[2][i]] = true;
	end
	for i = 1, #data[3] do
		local curr = data[3][i];
		res[curr[1]] = curr[2];
	end

	if not no_id and res.id then
		res.id = make_id(res.id, readable, ctx);
	end

	return res;
end

function converters.Table(data, ctx)
	local options = data[3];
	local header = data[4];
	local body = data[5][1];

	local text = array {};

	local function convert_cell(cell, tag, options)
		local align = options[1].t;
		indent(ctx);
		text:push("<", tag);
		if align == "AlignRight" then
			text:push [[ class="right"]];
		elseif align == "AlignLeft" then
			text:push [[ class="left"]];
		elseif align == "AlignCenter" then
			text:push [[ class="center"]];
		end
		text:push(">\n", get_indent(ctx));
		text:append((convert_all(cell[5], ctx)));
		undent(ctx);
		text:push("\n", get_indent(ctx), "</" .. tag .. ">");
	end

	local function convert_row(row, tag)
		text:push("<tr>");

		for i = 1, #row[2] do
			local cell = row[2][i];

			indent(ctx);
			text:push("\n", get_indent(ctx));
			convert_cell(cell, tag, options[i]);
			undent(ctx);
		end

		text:push("\n", get_indent(ctx), "</tr>");
	end

	text:push [[<table class="graphic-table">]];

	indent(ctx);
	indent(ctx);

	text:push("\n", get_indent(ctx, -1), "<thead>");
	text:push("\n", get_indent(ctx));
	convert_row(header[2][1], "th");
	text:push("\n", get_indent(ctx, -1), "</thead>");
	text:push("\n", get_indent(ctx, -1), "<tbody>");

	for i = 1, #body[4] do
		text:push("\n", get_indent(ctx));
		convert_row(body[4][i], "td");
	end

	text:push("\n", get_indent(ctx, -1), "</tbody>");

	undent(ctx);
	undent(ctx);
	text:push("\n", get_indent(ctx), "</table>");

	return text, array { "[table]" };
end
function converters.BulletList(data, ctx)
	local text = array { "<ul class=\"bullet\">" };
	local plain = array {};

	indent(ctx);
	for i = 1, #data do
		local el_text, el_plain = convert_all(data[i], ctx);

		text
			:push("\n", get_indent(ctx), "<li>")
			:append(el_text)
			:push("</li>");
		plain:push("* "):append(el_plain):push("\n");
	end
	undent(ctx);

	text:push("\n", get_indent(ctx), "</ul>");

	return text, plain;
end
function converters.OrderedList(data, ctx)
	local text = array { "<ol>" };
	local plain = array {};

	indent(ctx);
	for i = 1, #data[2] do
		local el_text, el_plain = convert_all(data[2][i], ctx);

		text
			:push("\n", get_indent(ctx), "<li value=\"", i, "\">")
			:append(el_text)
			:push("</li>");
		plain:push(i .. ") "):append(el_plain):push("\n");
	end
	undent(ctx);

	text:push("\n", get_indent(ctx), "</ol>");

	return text, plain;
end

function converters.Code(data, ctx)
	local content = data[2];

	return array { "<code>", sanitize(content), "</code>" }, array { content };
end
function converters.CodeBlock(data, ctx)
	local attribs = read_attributes(data[1], nil, ctx);
	local content = data[2];

	local text = array { "<pre><code" };

	local classes = array {};

	for k, v in next, attribs do
		if v == true then
			classes:push("language-" .. k);
		end
	end

	if #classes > 0 then
		text:push(" class=\"", classes:join " ", "\"");
	end

	text:push(">", sanitize(content), "</code></pre>");

	return text, array { content };
end
function converters.Image(data, ctx)
	local alt, alt_plain = convert_all(data[2], ctx);
	local url = data[3][1];
	local title = data[3][2];
	local attribs = read_attributes(data[1], title or alt_plain:join " ", ctx);

	local classes = array {};
	if attribs.small then classes:push("small") end
	if attribs.big then classes:push("big") end

	local text = array {}
		:push("<img title=\"", title, "\" src=\"", url, "\" alt=\"")
		:append(alt)
		:push("\"");

	if #classes > 0 then
		text:push(" class=\"", classes:join " ", "\"");
	end

	text:push("/>");

	return text, title and { title } or alt_plain or url or "[picture]";
end
function converters.Figure(data, ctx)
	local chapter_i = #(ctx.headers or {});
	ctx.figures_indices = ctx.figures_indices or {};
	ctx.figures_indices[chapter_i] = (ctx.figures_indices[chapter_i] or 0) + 1;
	local figure_i = ctx.figures_indices[chapter_i];

	local prefix = ("%s.%s."):format(chapter_i, figure_i);

	local attribs = read_attributes(data[1], prefix, ctx);
	local id = attribs.id;

	indent(ctx);
	indent(ctx);

	local name, name_plain = convert_all(data[2][1], ctx);
	local content, plain = convert_all(data[3], ctx);

	undent(ctx);
	undent(ctx);
	local text = array { "<figure" };
	if id then text:push(" id=\"", id, "\"") end
	text:push(">");

	text
		:push("\n", get_indent(ctx, 1), "<div class=\"fig-content\">", "\n", get_indent(ctx, 2))
		:append(content)
		:push("\n", get_indent(ctx, 1), "</div>", "\n", get_indent(ctx, 1), "<figcaption>", "\n", get_indent(ctx, 2))
		:push(prefix, " ")
		:append(name)
		:push("\n", get_indent(ctx, 1), "</figcaption>")
		:push("\n", get_indent(ctx), "</figure>");
	plain
		:push(" (", prefix, " ")
		:append(name_plain)
		:push(")");

	ctx.citables = ctx.citables or {};
	if id then ctx.citables[id] = prefix end

	return text, plain;
end
function converters.Div(data, ctx)
	local attribs = read_attributes(data[1], nil, ctx, true);

	if attribs.figure then
		local separator_i = data[2]:find_i(function (v) return v.t == "HorizontalRule" end);

		local content_data, title_data;

		if separator_i == nil then
			content_data = array { data[2][1] };
			title_data = data[2]:slice(2);
		else
			content_data = data[2]:slice(1, separator_i - 1);
			title_data = data[2]:slice(separator_i + 1);
		end

		if #title_data == 1 and title_data[1].t == "Para" then
			title_data = title_data[1].c
		end

		return converters.Figure(array {
			data[1],
			array { title_data },
			content_data,
		}, ctx);
	else
		error "Divs are not supported";
	end
end

function converters.Cite(data, ctx)
	local citation = data[1][1];
	local id = last_id(citation.citationId, ctx);

	local function target()
		local res = (ctx.citables or {})[id];
		if res == nil then
			io.stderr:write("WARNING! Citation '" .. id .. "' doesn't exist!\n");
			res = id;
		end

		return res;
	end

	return array { "<a href=\"#", id, "\">", target, "</a>" }, array { target }, true;
end
function converters.Header(data, ctx)
	local level = data[1];
	local attribs = read_attributes(data[2], nil, ctx);

	ctx.headers = ctx.headers or array {};
	ctx.header_stack = ctx.header_stack or array {};

	local parent = ctx.header_stack[level - 1];
	if level > 1 and parent == nil then error "Heading hierarchy inconsistent" end

	local text, plain = convert_all(data[3], ctx);

	local header = {
		id = attribs.id,
		level = level,
		children = array {},
		ignored = attribs.nonum or false,
	};

	local text_prefix, plain_prefix = "", "";

	if level == 1 then
		ctx.headers:push(header);
	else
		parent.children:push(header);
	end

	if not header.ignored then
		local prefix = array { count_headers(ctx.headers) };
		local text_format, plain_format;

		for i = 1, level - 1 do
			--- @diagnostic disable-next-line: undefined-field
			prefix:push(count_headers(ctx.header_stack[i].children));
		end

		header.prefix = prefix:join ".";

		if level == 1 then
			text_format = ctx.chapter_format or "Chapter %s<br>";
			plain_format = ctx.chapter_format_plain or "Chapter %s: ";
		else
			text_format = "%s. ";
			plain_format = "%s. ";
		end

		text_prefix = text_format:format(header.prefix);
		plain_prefix = plain_format:format(header.prefix);
	end

	ctx.header_stack = ctx.header_stack:fill(nil, level + 1, #ctx.header_stack);
	ctx.header_stack[level] = header;

	header.text = text_prefix .. text:join "";
	header.plain = plain_prefix .. plain:join "";

	if attribs.id then
		ctx.citables = ctx.citables or {};
		ctx.citables[attribs.id] = header.plain;
	end

	return
		arrays.concat({ ("<h%d id=%q>"):format(level, attribs.id), text_prefix }, text, { ("</h%d>"):format(level) }),
		array { plain_prefix }:append(plain);
end
function converters.Link(data, ctx)
	local content, content_plain = convert_all(data[2], ctx);

	local url = data[3][1];
	local attribs = read_attributes(data[1], nil, ctx);
	local id = attribs.id or data[3][2];

	if id == "" then id = nil end


	if id then
		ctx.link_i = (ctx.link_i or 0) + 1;
		local i = ctx.link_i;
		ctx.citables = ctx.citables or {};
		ctx.citables[id] = "[" .. i .. "]";
	end

	local plain = array {};
	plain:push("<a href=\"", url, "\"");

	if id ~= nil then
		plain:push(" id=\"", id, "\"");
	end

	plain:push(">");
	plain:append(content);
	plain:push("</a>");


	return plain, content_plain:push(" (", url, ")"), true;
end

function converters.Para(data, ctx)
	indent(ctx);
	local text, plain = convert_all(data, ctx);
	undent(ctx);
	return
		array {}
			:push "<p>"
			:append(text)
			:append "</p>",
		plain;
end
function converters.Emph(data, ctx)
	local text, plain = convert_all(data, ctx);
	return arrays.concat({ "<i>" }, text, { "</i>" }), plain, true;
end
function converters.Strong(data, ctx)
	local text, plain = convert_all(data, ctx);
	return arrays.concat({ "<strong>" }, text, { "</strong>" }), plain, true;
end
function converters.Str(data)
	return array { sanitize(data) }, array { data }, true;
end
function converters.Plain(data, ctx)
	return convert_all(data, ctx);
end

function converters.RawBlock(data)
	return array {}, array {}, true;
end
function converters.MetaInlines(data, ctx)
	return convert_all(data, ctx);
end

function converters.LineBreak()
	return array { "<br>" }, array { " " }, true;
end
function converters.SoftBreak()
	return array { "&shy;" }, array { " " }, true;
end
function converters.HorizontalRule()
	return array { "<br class=\"pg-break\"/>" }, array { " " };
end
function converters.Space()
	return array { " " }, array { " " }, true;
end

local function parse_meta(meta)
	local res = {};

	for k, v in next, meta do
		local text, plain = convert(v, {});
		res[k] = text:concat "";
	end

	return res;
end

local function convert_headers(headers, ctx)
	ctx = ctx or { indent = 0 };
	indent(ctx);
	local res = arrays.concat(
		{ "<ul>" },
		headers:flat_map(function (child)
			return arrays.concat(
				{
					"\n", get_indent(ctx),
					"<li><a href=\"#",
					child.id,
					"\"><span class=\"name\">",
					child.plain,
					"</span><span class=\"page\">Page</span></a>"
				},
				#child.children > 0 and convert_headers(child.children, ctx) or { "" },
				{ "</li>" }
			);
		end),
		{
			"\n", get_indent(ctx, -1),
			"</ul>"
		}
	);
	undent(ctx);

	return res;
end

local function parse(...)
	local stream, err = io.popen("pandoc --highlight-style kate --from markdown-raw_html --to json -o - " .. array { ... }:join " ");
	local str = read_to_str(stream, err);
	local raw = json.parse(str);

	return {
		metadata = parse_meta(raw.meta),
		content = raw.blocks or array {},
	};
end

local function convert_page(data, ctx)
	ctx = ctx or {};
	ctx.headers = ctx.headers or array {};

	local curr_page = array {};
	local pages = array {};

	for i = 1, #data.content do
		local curr = data.content[i];
		if curr.t == "Header" then
			if curr.c[1] == 1 then
				if #pages > 0 or #curr_page > 0 then
					pages:append(curr_page);
				end
				curr_page = array {};
			end
		end

		local text, _, inline = convert(curr, ctx);
		curr_page:append(text);
		if not inline then
			curr_page:push("\n");
		end
	end

	pages:append(curr_page);

	local function mapper(v)
		if type(v) == "function" then
			return v();
		else
			return v;
		end
	end

	return {
		content = pages:map(mapper, true):join "",
		toc = convert_headers(ctx.headers):map(mapper, true):join "",
	};
end

local function subst(template, variables)
	local replaces = array {};

	for k, v in next, variables do
		local i = 1;
		local search = "{{" .. k .. "}}";

		while true do
			local b, e = string.find(template, search, i, true);
			if b == nil then break end

			replaces:push { b = b, e = e, v = v };
			i = e + 1;
		end
	end

	table.sort(replaces, function (a, b) return a.b < b.b end);

	local parts = array {};
	local prev = 1;

	for i = 1, #replaces do
		local curr = replaces[i];
		parts:push(template:sub(prev, curr.b - 1), curr.v);
		prev = curr.e + 1;
	end

	parts:push(template:sub(prev));

	return parts:join "";
end

function build(options)
	local toc = options.toc;
	local content = options.content;
	local ctx = options.ctx or {};

	print(table.concat { "Building ", array(options):join ", ", "..." });

	local res = convert_page(parse(unpack(options)), ctx);

	if toc == nil and content == nil then
		return res.content;
	end

	local obj = {};
	if toc ~= nil then obj[toc] = res.toc end
	if content ~= nil then obj[content] = res.content end

	return obj;
end

function combine(objects)
	local res = {};

	for i = 1, #objects do
		for k, v in next, objects[i] do
			res[k] = v;
		end
	end

	return res;
end

function template(options)
	print("Templating into " .. options.template .. "...");
	options = combine(array { options }:append(options));
	return subst(read_to_str(io.open(options.template, "r")), options);
end

function emit(values)
	local f = io.stdout;

	if values.target then
		f = assert(io.open(values.target, "w"));
		print("Emitting to " .. values.target .. "...");
	else
		print("Emitting to stdout...");
	end

	for i = 1, #values do
		assert(f:write(values[i]));
	end

	if f ~= io.stdout then
		f:close();
	end
end

-- return main;
