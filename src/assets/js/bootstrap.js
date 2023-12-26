(function (ts, env, libs) {
    var src = '', version = 0;
    var lib = libs.concat([
        'declare function exit(): never;',
        'declare function go(): any;',
        'declare function getTsDeclarations(): string[];'
    ]).join('');
    var libSnapshot = ts.ScriptSnapshot.fromString(lib);
    var environments = {};
    var declSnapshots = [];

    var settings = {
        outDir: "/out",
        declarationDir: "/out",
        target: ts.ScriptTarget.ES5,
        lib: [ ],
        module: ts.ModuleKind.None,
        declaration: true,
        stripInternal: true,
        downlevelIteration: true,
        forceConsistentCasingInFileNames: true,
        experimentalDecorators: true,
        strict: true,
        sourceMap: true,
    };

    var reg = ts.createDocumentRegistry();
    var service = ts.createLanguageService({
        getCurrentDirectory: function() { return "/"; },
        getDefaultLibFileName: function() { return "/lib.d.ts"; },
        getScriptFileNames: function() {
            var res = [ "/src.ts", "/lib.d.ts" ];
            for (var i = 0; i < declSnapshots.length; i++) res.push("/glob." + (i + 1) + ".d.ts");
            return res;
        },
        getCompilationSettings: function () { return settings; },
        fileExists: function(filename) { return filename === "/lib.d.ts" || filename === "/src.ts" || filename === "/glob.d.ts"; },

        getScriptSnapshot: function(filename) {
            if (filename === "/lib.d.ts") return libSnapshot;
            if (filename === "/src.ts") return ts.ScriptSnapshot.fromString(src);

            var index = /\/glob\.(\d+)\.d\.ts/g.exec(filename);
            if (index && index[1] && (index = Number(index[1])) && index > 0 && index <= declSnapshots.length) {
                return declSnapshots[index - 1];
            }

            throw new Error("File '" + filename + "' doesn't exist.");
        },
        getScriptVersion: function (filename) {
            if (filename === "/lib.d.ts" || filename.startsWith("/glob.")) return 0;
            else return version;
        },
    }, reg);

    service.getEmitOutput("/lib.d.ts");
    log("Loaded libraries!");

    var oldCompile = env.compile;

    function compile(code, filename, env) {
        src = code;
        version++;

        if (!environments[env.id]) environments[env.id] = []
        var decls = declSnapshots = environments[env.id];
        var emit = service.getEmitOutput("/src.ts");

        var diagnostics = []
            .concat(service.getCompilerOptionsDiagnostics())
            .concat(service.getSyntacticDiagnostics("/src.ts"))
            .concat(service.getSemanticDiagnostics("/src.ts"))
            .map(function (diagnostic) {
                var message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n");
                if (diagnostic.file) {
                    var pos = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start);
                    var file = diagnostic.file.fileName.substring(1);
                    if (file === "src.ts") file = filename;
                    return file + ":" + (pos.line + 1) + ":" + (pos.character + 1) + ": " + message;
                }
                else return message;
            });

        if (diagnostics.length > 0) {
            throw new SyntaxError(diagnostics.join("\n"));
        }

        var map = JSON.parse(emit.outputFiles[0].text);
        var result = emit.outputFiles[1].text;
        var declaration = emit.outputFiles[2].text;

        var compiled = oldCompile(result, filename, env);

        return {
            function: function () {
                var val = compiled.function.apply(this, arguments);
                if (declaration !== '') decls.push(ts.ScriptSnapshot.fromString(declaration));
                return val;
            },
            breakpoints: compiled.breakpoints,
            mapChain: compiled.mapChain.concat(map.mappings),
        };
    }

    function apply(env) {
        env.compile = compile;
        env.global.getTsDeclarations = function() {
            return environments[env.id];
        }
    }

    apply(env);
})(arguments[0], arguments[1], arguments[2]);
