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
            copy('src', 'dst/classes', v => !v.endsWith('.java')),
            compileJava(conf),
        ]);

        await Promise.all([
            jar(conf, 'me.topchetoeu.jscript.common'),
            jar(conf, 'me.topchetoeu.jscript.core'),
            jar(conf, 'me.topchetoeu.jscript.utils'),
            jar(conf, 'me.topchetoeu.jscript', 'me.topchetoeu.jscript.utils.JScriptRepl'),
        ]);

        console.log('Done!');
    }
    catch (e) {
        if (argv[2] === 'debug') throw e;
        console.log(e.toString());
        exit(-1);
    }
})();
