interface Number {
    toString(): string;
    valueOf(): number;
    constructor: NumberConstructor;
}
interface NumberConstructor {
    (val: any): number;
    new (val: any): Number;
    prototype: Number;
    parseInt(val: unknown): number;
    parseFloat(val: unknown): number;
}

declare var Number: NumberConstructor;
declare const parseInt: typeof Number.parseInt;
declare const parseFloat: typeof Number.parseFloat;

gt.Number = function(this: Number | undefined, arg: any) {
    var val;
    if (arguments.length === 0) val = 0;
    else val = arg - 0;
    if (this === undefined || this === null) return val;
    else (this as any).value = val;
} as NumberConstructor;

Number.prototype = (0 as any).__proto__ as Number;
setConstr(Number.prototype, Number);

setProps(Number.prototype, {
    valueOf() {
        if (typeof this === 'number') return this;
        else return (this as any).value;
    },
    toString() {
        if (typeof this === 'number') return this + '';
        else return (this as any).value + '';
    }
});

setProps(Number, {
    parseInt(val) { return Math.trunc(Number.parseFloat(val)); },
    parseFloat(val) { return internals.parseFloat(val); },
});

(gt as any).parseInt = Number.parseInt;
(gt as any).parseFloat = Number.parseFloat;