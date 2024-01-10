const { spawn } = require('child_process');
const fs = require('fs/promises');
const pt = require('path');
const { argv, exit } = require('process');
const { Readable } = require('stream');


async function* find(src, dst, wildcard) {
    const stat = await fs.stat(src);

    if (stat.isDirectory()) {
        for (const el of await fs.readdir(src)) {
            for await (const res of find(pt.join(src, el), dst ? pt.join(dst, el) : undefined, wildcard)) yield res;
        }
    }
    else if (stat.isFile() && wildcard(src)) yield dst ? { src, dst } : src;
}
async function copy(src, dst, wildcard) {
    const promises = [];

    for await (const el of find(src, dst, wildcard)) {
        promises.push((async () => {
            await fs.mkdir(pt.dirname(el.dst), { recursive: true });
            await fs.copyFile(el.src, el.dst);
        })());
    }

    await Promise.all(promises);
}

function run(suppressOutput, cmd, ...args) {
    return new Promise((res, rej) => {
        const proc = spawn(cmd, args, { stdio: suppressOutput ? 'ignore' : 'inherit' });
        proc.once('exit', code => {
            if (code === 0) res(code);
            else rej(new Error(`Process ${cmd} exited with code ${code}.`));
        });
    })
}

async function downloadTypescript(outFile) {
    try {
        // Import the required libraries, without the need of a package.json
        console.log('Importing modules...');
        await run(true, 'npm', 'i', 'tar', 'zlib', 'uglify-js');
        await fs.mkdir(pt.dirname(outFile), { recursive: true });
        await fs.mkdir('tmp', { recursive: true });

        const tar = require('tar');
        const zlib = require('zlib');
        const { minify } = await import('uglify-js');

        // Download the package.json file of typescript
        const packageDesc = await (await fetch('https://registry.npmjs.org/typescript/latest')).json();
        const url = packageDesc.dist.tarball;

        console.log('Extracting typescript...');
        await new Promise(async (res, rej) => Readable.fromWeb((await fetch(url)).body)
            .pipe(zlib.createGunzip())
            .pipe(tar.x({ cwd: 'tmp', filter: v => v === 'package/lib/typescript.js' }))
            .on('end', res)
            .on('error', rej)
        );

        console.log('Compiling typescript to ES5...');

        const ts = require('./tmp/package/lib/typescript');
        const program = ts.createProgram([ 'tmp/package/lib/typescript.js' ], {
            outFile: "tmp/typescript-es5.js",
            target: ts.ScriptTarget.ES5,
            module: ts.ModuleKind.None,
            downlevelIteration: true,
            allowJs: true,
        });
        program.emit();

        console.log('Minifying typescript...');

        const minified = minify((await fs.readFile('tmp/typescript-es5.js')).toString());
        // const minified = { code: (await fs.readFile('tmp/typescript-es5.js')).toString() };
        if (minified.error) throw minified.error;

        // Patch unsupported regex syntax
        minified.code = minified.code.replaceAll('[-/\\\\^$*+?.()|[\\]{}]', '[-/\\\\^$*+?.()|\\[\\]{}]');

        const stream = await fs.open(outFile, 'w');

        // Write typescript's license
        await stream.write(new TextEncoder().encode(`
/*! *****************************************************************************
Copyright (c) Microsoft Corporation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache Version 2.0 License for specific language governing permissions
and limitations under the License.

The following is a minified version of the unmodified Typescript 5.2
***************************************************************************** */
`));

        await stream.write(minified.code);
        console.log('Typescript bundling done!');
    }
    finally {
        // Clean up all stuff left from typescript bundling
        await fs.rm('tmp', { recursive: true, force: true });
        await fs.rm('package.json');
        await fs.rm('package-lock.json');
        await fs.rm('node_modules', { recursive: true });
    }
}
async function compileJava(conf) {
    try {
        await fs.writeFile('Metadata.java', (await fs.readFile('src/me/topchetoeu/jscript/common/Metadata.java')).toString()
            .replace('${VERSION}', conf.version)
            .replace('${NAME}', conf.name)
            .replace('${AUTHOR}', conf.author)
        );
        const args = ['--release', '11', ];
        if (argv[2] === 'debug') args.push('-g');
        args.push('-d', 'dst/classes', 'Metadata.java');
    
        console.log('Compiling java project...');
        for await (const path of find('src', undefined, v => v.endsWith('.java') && !v.endsWith('Metadata.java'))) args.push(path);
        await run(false, conf.javahome + 'javac', ...args);
        console.log('Compiled java project!');
    }
    finally {
        await fs.rm('Metadata.java');
    }
}
async function jar(conf, project, mainClass) {
    const args = [
        'jar', '-c',
        '-f', `dst/${project}-v${conf.version}.jar`,
    ];
    if (mainClass) args.push('-e', mainClass);
    args.push('-C', 'dst/classes', project.replaceAll('.', '/'));
    console.log(args.join(' '));

    await run(true, ...args);
}

(async () => {
    try {
        if (argv[2] === 'init-ts') {
            await downloadTypescript('src/me/topchetoeu/jscript/utils/assets/js/ts.js');
        }
        else {
            const conf = {
                name: "java-jscript",
                author: "TopchetoEU",
                javahome: "",
                version: argv[3]
            };

            if (conf.version.startsWith('refs/tags/')) conf.version = conf.version.substring(10);
            if (conf.version.startsWith('v')) conf.version = conf.version.substring(1);

            try { await fs.rm('dst', { recursive: true }); } catch {}

            await Promise.all([
                (async () => {
                    await copy('src', 'dst/classes', v => !v.endsWith('.java'));
                    // await downloadTypescript('dst/classes/me/topchetoeu/jscript/utils/assets/js/ts.js');
                })(),
                compileJava(conf),
            ]);

            await Promise.all([
                jar(conf, 'me.topchetoeu.jscript.common'),
                jar(conf, 'me.topchetoeu.jscript.core'),
                jar(conf, 'me.topchetoeu.jscript.lib'),
                jar(conf, 'me.topchetoeu.jscript.utils'),
                jar(conf, 'me.topchetoeu.jscript', 'me.topchetoeu.jscript.utils.JScriptRepl'),
            ]);

            console.log('Done!');
        }
    }
    catch (e) {
        if (argv[2] === 'debug') throw e;
        console.log(e.toString());
        exit(-1);
    }
})();
