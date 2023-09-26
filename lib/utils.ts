function setProps<
    TargetT extends object,
    DescT extends {
        [x in Exclude<keyof TargetT, 'constructor'> ]?: TargetT[x] extends ((...args: infer ArgsT) => infer RetT) ?
            ((this: TargetT, ...args: ArgsT) => RetT) :
            TargetT[x]
    }
>(target: TargetT, desc: DescT) {
    var props = internals.keys(desc, false);
    for (var i = 0; i in props; i++) {
        var key = props[i];
        internals.defineField(
            target, key, (desc as any)[key],
            true, // writable
            false, // enumerable
            true // configurable
        );
    }
}
function setConstr(target: object, constr: Function) {
    internals.defineField(
        target, 'constructor', constr,
        true, // writable
        false, // enumerable
        true // configurable
    );
}
