interface Environment {
    global: typeof globalThis & Record<string, any>;
    captureErr: boolean;
    internals: any;
}

function setProps<
    TargetT extends object,
    DescT extends {
        [x in Exclude<keyof TargetT, 'constructor'> ]?: TargetT[x] extends ((...args: infer ArgsT) => infer RetT) ?
            ((this: TargetT, ...args: ArgsT) => RetT) :
            TargetT[x]
    }
>(target: TargetT, env: Environment, desc: DescT) {
    var props = env.internals.keys(desc, false);
    for (var i = 0; i < props.length; i++) {
        var key = props[i];
        env.internals.defineField(
            target, key, (desc as any)[key],
            true, // writable
            false, // enumerable
            true // configurable
        );
    }
}
function setConstr<ConstrT, T extends { constructor: ConstrT }>(target: T, constr: ConstrT, env: Environment) {
    env.internals.defineField(
        target, 'constructor', constr,
        true, // writable
        false, // enumerable
        true // configurable
    );
}

function wrapI(max: number, i: number) {
    i |= 0;
    if (i < 0) i = max + i;
    return i;
}
function clampI(max: number, i: number) {
    if (i < 0) i = 0;
    if (i > max) i = max;
    return i;
}