define("values/string", () => {
    var String = env.global.String = function(this: String | undefined, arg: any) {
        var val;
        if (arguments.length === 0) val = '';
        else val = arg + '';
        if (this === undefined || this === null) return val;
        else (this as any).value = val;
    } as StringConstructor;

    env.setProto('string', String.prototype);
    setConstr(String.prototype, String);

    setProps(String.prototype, {
        toString() {
            if (typeof this === 'string') return this;
            else return (this as any).value;
        },
        valueOf() {
            if (typeof this === 'string') return this;
            else return (this as any).value;
        },

        substring(start, end) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.substring(start, end);
                else throw new Error('This function may be used only with primitive or object strings.');
            }
            start = start ?? 0 | 0;
            end = (end ?? this.length) | 0;

            const res = [];

            for (let i = start; i < end; i++) {
                if (i >= 0 && i < this.length) res[res.length] = this[i];
            }

            return internals.stringFromStrings(res);
        },
        substr(start, length) {
            start = start ?? 0 | 0;

            if (start >= this.length) start = this.length - 1;
            if (start < 0) start = 0;

            length = (length ?? this.length - start) | 0;
            const end = length + start;
            const res = [];

            for (let i = start; i < end; i++) {
                if (i >= 0 && i < this.length) res[res.length] = this[i];
            }

            return internals.stringFromStrings(res);
        },

        toLowerCase() {
            // TODO: Implement localization
            const res = [];

            for (let i = 0; i < this.length; i++) {
                const c = internals.char(this[i]);

                if (c >= 65 && c <= 90) res[i] = c - 65 + 97;
                else res[i] = c;
            }

            return internals.stringFromChars(res);
        },
        toUpperCase() {
            // TODO: Implement localization
            const res = [];

            for (let i = 0; i < this.length; i++) {
                const c = internals.char(this[i]);

                if (c >= 97 && c <= 122) res[i] = c - 97 + 65;
                else res[i] = c;
            }

            return internals.stringFromChars(res);
        },

        charAt(pos) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.charAt(pos);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            pos = pos | 0;
            if (pos < 0 || pos >= this.length) return '';
            return this[pos];
        },
        charCodeAt(pos) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.charAt(pos);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            pos = pos | 0;
            if (pos < 0 || pos >= this.length) return 0 / 0;
            return internals.char(this[pos]);
        },

        startsWith(term, pos) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.startsWith(term, pos);
                else throw new Error('This function may be used only with primitive or object strings.');
            }
            pos = pos! | 0;
            term = term + "";

            if (pos < 0 || this.length < term.length + pos) return false;

            for (let i = 0; i < term.length; i++) {
                if (this[i + pos] !== term[i]) return false;
            }

            return true;
        },
        endsWith(term, pos) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.endsWith(term, pos);
                else throw new Error('This function may be used only with primitive or object strings.');
            }
            pos = (pos ?? this.length) | 0;
            term = term + "";

            const start = pos - term.length;

            if (start < 0 || this.length < term.length + start) return false;

            for (let i = 0; i < term.length; i++) {
                if (this[i + start] !== term[i]) return false;
            }

            return true;
        },

        indexOf(term: any, start) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.indexOf(term, start);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof term[Symbol.search] !== 'function') term = RegExp.escape(term);

            return term[Symbol.search](this, false, start);
        },
        lastIndexOf(term: any, start) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.indexOf(term, start);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof term[Symbol.search] !== 'function') term = RegExp.escape(term);

            return term[Symbol.search](this, true, start);
        },
        includes(term, start) {
            return this.indexOf(term, start) >= 0;
        },

        replace(pattern: any, val) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.replace(pattern, val);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof pattern[Symbol.replace] !== 'function') pattern = RegExp.escape(pattern);

            return pattern[Symbol.replace](this, val);
        },
        replaceAll(pattern: any, val) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.replace(pattern, val);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof pattern[Symbol.replace] !== 'function') pattern = RegExp.escape(pattern, "g");
            if (pattern instanceof RegExp && !pattern.global) pattern = new pattern.constructor(pattern.source, pattern.flags + "g");

            return pattern[Symbol.replace](this, val);
        },

        match(pattern: any) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.match(pattern);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof pattern[Symbol.match] !== 'function') pattern = RegExp.escape(pattern);

            return pattern[Symbol.match](this);
        },
        matchAll(pattern: any) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.matchAll(pattern);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof pattern[Symbol.match] !== 'function') pattern = RegExp.escape(pattern, "g");
            if (pattern instanceof RegExp && !pattern.global) pattern = new pattern.constructor(pattern.source, pattern.flags + "g");

            return pattern[Symbol.match](this);
        },

        split(pattern: any, lim, sensible) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.split(pattern, lim, sensible);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            if (typeof pattern[Symbol.split] !== 'function') pattern = RegExp.escape(pattern, "g");

            return pattern[Symbol.split](this, lim, sensible);
        },
        slice(start, end) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.slice(start, end);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            start = wrapI(this.length, start ?? 0 | 0);
            end = wrapI(this.length, end ?? this.length | 0);

            if (start > end) return '';

            return this.substring(start, end);
        },

        concat(...args) {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.concat(...args);
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            var res = this;
            for (var arg of args) res += arg;
            return res;
        },

        trim() {
            return this
                .replace(/^\s+/g, '')
                .replace(/\s+$/g, '');
        }
    });

    setProps(String, {
        fromCharCode(val) {
            return internals.stringFromChars([val | 0]);
        },
    })

    env.global.Object.defineProperty(String.prototype, 'length', {
        get() {
            if (typeof this !== 'string') {
                if (this instanceof String) return (this as any).value.length;
                else throw new Error('This function may be used only with primitive or object strings.');
            }

            return internals.strlen(this);
        },
        configurable: true,
        enumerable: false,
    });
});