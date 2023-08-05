interface RegExpResultIndices extends Array<[number, number]> {
    groups?: { [name: string]: [number, number]; };
}
interface RegExpResult extends Array<string> {
    groups?: { [name: string]: string; };
    index: number;
    input: string;
    indices?: RegExpResultIndices;
    escape(raw: string, flags: string): RegExp;
}
interface SymbolConstructor {
    readonly match: unique symbol;
    readonly matchAll: unique symbol;
    readonly split: unique symbol;
    readonly replace: unique symbol;
    readonly search: unique symbol;
}

type ReplaceFunc = (match: string, ...args: any[]) => string;

interface Matcher {
    [Symbol.match](target: string): RegExpResult | string[] | null;
    [Symbol.matchAll](target: string): IterableIterator<RegExpResult>;
}
interface Splitter {
    [Symbol.split](target: string, limit?: number, sensible?: boolean): string[];
}
interface Replacer {
    [Symbol.replace](target: string, replacement: string): string;
}
interface Searcher {
    [Symbol.search](target: string, reverse?: boolean, start?: number): number;
}

declare class RegExp implements Matcher, Splitter, Replacer, Searcher {
    static escape(raw: any, flags?: string): RegExp;

    prototype: RegExp;

    exec(val: string): RegExpResult | null;
    test(val: string): boolean;
    toString(): string;

    [Symbol.match](target: string): RegExpResult | string[] | null;
    [Symbol.matchAll](target: string): IterableIterator<RegExpResult>;
    [Symbol.split](target: string, limit?: number, sensible?: boolean): string[];
    [Symbol.replace](target: string, replacement: string | ReplaceFunc): string;
    [Symbol.search](target: string, reverse?: boolean, start?: number): number;

    readonly dotAll: boolean;
    readonly global: boolean;
    readonly hasIndices: boolean;
    readonly ignoreCase: boolean;
    readonly multiline: boolean;
    readonly sticky: boolean;
    readonly unicode: boolean;

    readonly source: string;
    readonly flags: string;

    lastIndex: number;

    constructor(pattern?: string, flags?: string);
    constructor(pattern?: RegExp, flags?: string);
}

(Symbol as any).replace = Symbol('Symbol.replace');
(Symbol as any).match = Symbol('Symbol.match');
(Symbol as any).matchAll = Symbol('Symbol.matchAll');
(Symbol as any).split = Symbol('Symbol.split');
(Symbol as any).search = Symbol('Symbol.search');

setProps(RegExp.prototype, {
    [Symbol.typeName]: 'RegExp',

    test(val) {
        return !!this.exec(val);
    },
    toString() {
        return '/' + this.source + '/' + this.flags;
    },

    [Symbol.match](target) {
        if (this.global) {
            const res: string[] = [];
            let val;
            while (val = this.exec(target)) {
                res.push(val[0]);
            }
            this.lastIndex = 0;
            return res;
        }
        else {
            const res = this.exec(target);
            if (!this.sticky) this.lastIndex = 0;
            return res;
        }
    },
    [Symbol.matchAll](target) {
        let pattern: RegExp | undefined = new this.constructor(this, this.flags + "g") as RegExp;

        return {
            next: (): IteratorResult<RegExpResult, undefined> => {
                const val = pattern?.exec(target);

                if (val === null || val === undefined) {
                    pattern = undefined;
                    return { done: true };
                }
                else return { value: val };
            },
            [Symbol.iterator]() { return this; }
        }
    },
    [Symbol.split](target, limit, sensible) {
        const pattern = new this.constructor(this, this.flags + "g") as RegExp;
        let match: RegExpResult | null;
        let lastEnd = 0;
        const res: string[] = [];

        while ((match = pattern.exec(target)) !== null) {
            let added: string[] = [];

            if (match.index >= target.length) break;

            if (match[0].length === 0) {
                added = [ target.substring(lastEnd, pattern.lastIndex), ];
                if (pattern.lastIndex < target.length) added.push(...match.slice(1));
            }
            else if (match.index - lastEnd > 0) {
                added = [ target.substring(lastEnd, match.index), ...match.slice(1) ];
            }
            else {
                for (let i = 1; i < match.length; i++) {
                    res[res.length - match.length + i] = match[i];
                }
            }

            if (sensible) {
                if (limit !== undefined && res.length + added.length >= limit) break;
                else res.push(...added);
            }
            else {
                for (let i = 0; i < added.length; i++) {
                    if (limit !== undefined && res.length >= limit) return res;
                    else res.push(added[i]);
                }
            }

            lastEnd = pattern.lastIndex;
        }

        if (lastEnd < target.length) {
            res.push(target.substring(lastEnd));
        }

        return res;
    },
    [Symbol.replace](target, replacement) {
        const pattern = new this.constructor(this, this.flags + "d") as RegExp;
        let match: RegExpResult | null;
        let lastEnd = 0;
        const res: string[] = [];

        // log(pattern.toString());

        while ((match = pattern.exec(target)) !== null) {
            const indices = match.indices![0];
            res.push(target.substring(lastEnd, indices[0]));
            if (replacement instanceof Function) {
                res.push(replacement(target.substring(indices[0], indices[1]), ...match.slice(1), indices[0], target));
            }
            else {
                res.push(replacement);
            }
            lastEnd = indices[1];
            if (!pattern.global) break;
        }

        if (lastEnd < target.length) {
            res.push(target.substring(lastEnd));
        }

        return res.join('');
    },
    [Symbol.search](target, reverse, start) {
        const pattern: RegExp | undefined = new this.constructor(this, this.flags + "g") as RegExp;

        
        if (!reverse) {
            pattern.lastIndex = (start as any) | 0;
            const res = pattern.exec(target);
            if (res) return res.index;
            else return -1;
        }
        else {
            start ??= target.length;
            start |= 0;
            let res: RegExpResult | null = null;

            while (true) {
                const tmp = pattern.exec(target);
                if (tmp === null || tmp.index > start) break;
                res = tmp;
            }

            if (res && res.index <= start) return res.index;
            else return -1;
        }
    },
});
