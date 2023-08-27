var { define, run } = (() => {
    const modules: Record<string, Function> = {};

    function define(name: string, func: Function) {
        modules[name] = func;
    }
    function run(name: string) {
        return modules[name]();
    }

    return { define, run };
})();
